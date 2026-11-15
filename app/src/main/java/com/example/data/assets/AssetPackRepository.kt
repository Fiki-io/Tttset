package com.example.data.assets

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import com.example.R
import com.example.core.AppConstants
import com.example.domain.assets.DecodedTxdTexture
import com.example.domain.assets.FontsDatData
import com.example.domain.assets.FontsDatParser
import com.example.domain.assets.TxdParser
import com.example.domain.model.AssetPackStatus
import com.example.domain.model.SpriteIndexEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.zip.ZipInputStream

data class LoadedFontAtlas(
    val font1Bitmap: Bitmap?,
    val font2Bitmap: Bitmap?
)

class AssetPackRepository(private val context: Context) {

    private val internalAssetDir: File
        get() = File(context.filesDir, AppConstants.ASSETS_INTERNAL_DIR)

    private val spriteBitmapCache = LruCache<String, Bitmap>(AppConstants.BITMAP_CACHE_MAX_ENTRIES)

    @Volatile
    var fontsDat: FontsDatData? = null
        private set

    @Volatile
    var fontAtlas: LoadedFontAtlas? = null
        private set

    @Volatile
    var spriteEntries: List<SpriteIndexEntry> = emptyList()
        private set

    @Volatile
    private var txdFileByLowerName: Map<String, File> = emptyMap()

    suspend fun initializeFromExistingOrBundled(): AssetPackStatus = withContext(Dispatchers.IO) {
        val dir = internalAssetDir
        val hasBundledZip = runCatching {
            context.assets.list("")?.contains(AppConstants.BUNDLED_ASSETS_ZIP_NAME) == true
        }.getOrDefault(false)

        if (hasBundledZip) {
            val bundledBytesCount = runCatching {
                context.assets.open(AppConstants.BUNDLED_ASSETS_ZIP_NAME).use { stream ->
                    var total = 0L
                    val buf = ByteArray(AppConstants.ZIP_BUFFER_SIZE)
                    var r = stream.read(buf)
                    while (r > 0) {
                        total += r
                        r = stream.read(buf)
                    }
                    total
                }
            }.getOrDefault(0L)

            if (bundledBytesCount == 0L) {
                if (hasExtractedAssets(dir)) {
                    val loaded = indexExtractedDirectory(dir, sourceLabel = dir.name)
                    if (loaded is AssetPackStatus.Loaded) return@withContext loaded
                }
                return@withContext AssetPackStatus.Error(context.getString(R.string.assets_error_bundled_empty))
            }

            val markerFile = File(dir, ".bundled_zip_size")
            val cachedBundledSize = if (markerFile.exists()) markerFile.readText().trim().toLongOrNull() else null
            if (cachedBundledSize == bundledBytesCount && hasExtractedAssets(dir)) {
                val loaded = indexExtractedDirectory(dir, sourceLabel = AppConstants.BUNDLED_ASSETS_ZIP_NAME)
                if (loaded is AssetPackStatus.Loaded) return@withContext loaded
            }

            val extractedStatus = context.assets.open(AppConstants.BUNDLED_ASSETS_ZIP_NAME).use { stream ->
                extractAndIndexZipStream(stream, sourceLabel = AppConstants.BUNDLED_ASSETS_ZIP_NAME)
            }
            if (extractedStatus is AssetPackStatus.Loaded) {
                dir.mkdirs()
                markerFile.writeText(bundledBytesCount.toString())
            }
            return@withContext extractedStatus
        }

        if (hasExtractedAssets(dir)) {
            val loaded = indexExtractedDirectory(dir, sourceLabel = dir.name)
            if (loaded is AssetPackStatus.Loaded) return@withContext loaded
        }

        AssetPackStatus.NotLoaded
    }

    suspend fun importZipFromUri(uri: Uri): AssetPackStatus = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return@withContext AssetPackStatus.Error(context.getString(R.string.assets_error_read_stream))
        val fileName = uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { AppConstants.BUNDLED_ASSETS_ZIP_NAME }
            ?: AppConstants.BUNDLED_ASSETS_ZIP_NAME
        inputStream.use { stream ->
            extractAndIndexZipStream(stream, sourceLabel = fileName)
        }
    }

    private fun extractAndIndexZipStream(inputStream: InputStream, sourceLabel: String): AssetPackStatus {
        val targetDir = internalAssetDir
        val stagingDir = File(context.cacheDir, "${AppConstants.ASSETS_INTERNAL_DIR}_staging")
        if (stagingDir.exists()) stagingDir.deleteRecursively()
        stagingDir.mkdirs()

        val stagingCanonical = stagingDir.canonicalPath
        var extractedRelevantFiles = 0

        try {
            ZipInputStream(inputStream).use { zis ->
                val buffer = ByteArray(AppConstants.ZIP_BUFFER_SIZE)
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val normalizedName = entry.name.replace('\\', '/').trimStart('/')
                        val lower = normalizedName.lowercase(Locale.US)
                        val isRelevant = lower.endsWith(AppConstants.TXD_EXTENSION) ||
                            lower.endsWith("/${AppConstants.FONTS_DAT_FILENAME}") ||
                            lower == AppConstants.FONTS_DAT_FILENAME

                        if (isRelevant) {
                            val outFile = File(stagingDir, normalizedName)
                            val outCanonical = outFile.canonicalPath
                            if (!outCanonical.startsWith(stagingCanonical + File.separator)) {
                                throw SecurityException("Unsafe zip path: $normalizedName")
                            }
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { fos ->
                                var read = zis.read(buffer)
                                while (read > 0) {
                                    fos.write(buffer, 0, read)
                                    read = zis.read(buffer)
                                }
                            }
                            extractedRelevantFiles++
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            stagingDir.deleteRecursively()
            return AssetPackStatus.Error(e.message ?: context.getString(R.string.assets_error_read_stream))
        }

        if (extractedRelevantFiles == 0) {
            stagingDir.deleteRecursively()
            return AssetPackStatus.Error(context.getString(R.string.assets_error_empty_zip))
        }

        if (targetDir.exists()) targetDir.deleteRecursively()
        if (!stagingDir.renameTo(targetDir)) {
            stagingDir.copyRecursively(targetDir, overwrite = true)
            stagingDir.deleteRecursively()
        }

        spriteBitmapCache.evictAll()
        return indexExtractedDirectory(targetDir, sourceLabel)
    }

    private fun hasExtractedAssets(dir: File): Boolean {
        if (!dir.exists() || !dir.isDirectory) return false
        return dir.walkTopDown().any {
            it.isFile && (it.name.equals(AppConstants.FONTS_DAT_FILENAME, ignoreCase = true) ||
                it.name.lowercase(Locale.US).endsWith(AppConstants.TXD_EXTENSION))
        }
    }

    private fun indexExtractedDirectory(dir: File, sourceLabel: String): AssetPackStatus {
        var fontsDatFile: File? = null
        var fontsTxdFile: File? = null
        val txdFiles = mutableListOf<File>()

        dir.walkTopDown().filter { it.isFile }.forEach { file ->
            val lower = file.name.lowercase(Locale.US)
            when {
                lower == AppConstants.FONTS_DAT_FILENAME -> fontsDatFile = file
                lower == AppConstants.FONTS_TXD_FILENAME -> fontsTxdFile = file
                lower.endsWith(AppConstants.TXD_EXTENSION) -> txdFiles.add(file)
            }
        }

        val parsedFontsDat = fontsDatFile?.let { file ->
            runCatching { FontsDatParser.parse(file.readText(Charsets.ISO_8859_1)) }.getOrNull()
        }
        fontsDat = parsedFontsDat

        val loadedAtlas = fontsTxdFile?.let { file ->
            runCatching {
                val textures = TxdParser.decodeAll(file.readBytes())
                val f1 = textures.firstOrNull { it.name.equals("font1", ignoreCase = true) } ?: textures.getOrNull(0)
                val f2 = textures.firstOrNull { it.name.equals("font2", ignoreCase = true) } ?: textures.getOrNull(1)
                LoadedFontAtlas(
                    font1Bitmap = f1?.toAndroidBitmap(),
                    font2Bitmap = f2?.toAndroidBitmap()
                )
            }.getOrNull()
        }
        fontAtlas = loadedAtlas

        val txdMap = LinkedHashMap<String, File>()
        val entries = mutableListOf<SpriteIndexEntry>()

        for (file in txdFiles.sortedBy { it.name.lowercase(Locale.US) }) {
            val txdBaseName = file.nameWithoutExtension
            val txdLower = txdBaseName.lowercase(Locale.US)
            txdMap[txdLower] = file
            val headers = runCatching { TxdParser.listHeaders(file.readBytes()) }.getOrDefault(emptyList())
            val relativePath = file.relativeTo(dir).path
            for (header in headers) {
                val entry = SpriteIndexEntry(
                    txdName = txdBaseName,
                    spriteName = header.name,
                    txdRelativePath = relativePath,
                    width = header.width,
                    height = header.height
                )
                entries.add(entry)
            }
        }

        txdFileByLowerName = txdMap
        spriteEntries = entries

        if (parsedFontsDat == null && loadedAtlas == null && txdMap.isEmpty()) {
            return AssetPackStatus.Error(context.getString(R.string.assets_error_empty_zip))
        }

        return AssetPackStatus.Loaded(
            sourceLabel = sourceLabel,
            hasFontsDat = parsedFontsDat != null && parsedFontsDat.tablesByFontId.isNotEmpty(),
            hasFontsTxd = loadedAtlas?.font1Bitmap != null || loadedAtlas?.font2Bitmap != null,
            txdCount = txdMap.size,
            spriteCount = entries.size
        )
    }

    suspend fun loadSpriteBitmap(sampNotation: String): Bitmap? = withContext(Dispatchers.IO) {
        val parts = sampNotation.trim().split(':', limit = 2)
        if (parts.size != 2) return@withContext null
        val txdLower = parts[0].trim().lowercase(Locale.US)
        val spriteLower = parts[1].trim().lowercase(Locale.US)
        if (txdLower.isEmpty() || spriteLower.isEmpty()) return@withContext null

        val key = "$txdLower:$spriteLower"
        spriteBitmapCache.get(key)?.let { return@withContext it }

        val txdFile = txdFileByLowerName[txdLower] ?: return@withContext null
        val decoded = runCatching {
            TxdParser.decodeTextureByName(txdFile.readBytes(), spriteLower)
        }.getOrNull() ?: return@withContext null

        val bmp = decoded.toAndroidBitmap()
        spriteBitmapCache.put(key, bmp)
        bmp
    }

    private fun DecodedTxdTexture.toAndroidBitmap(): Bitmap {
        return Bitmap.createBitmap(argbPixels, width, height, Bitmap.Config.ARGB_8888)
    }
}
