package com.example.domain.assets

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class DecodedTxdTexture(
    val name: String,
    val maskName: String,
    val width: Int,
    val height: Int,
    val argbPixels: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DecodedTxdTexture) return false
        return name == other.name &&
            maskName == other.maskName &&
            width == other.width &&
            height == other.height &&
            argbPixels.contentEquals(other.argbPixels)
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + maskName.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + argbPixels.contentHashCode()
        return result
    }
}

data class TxdTextureHeader(
    val name: String,
    val maskName: String,
    val width: Int,
    val height: Int,
    val dataOffsetInFile: Int
)

object TxdParser {
    private const val RW_STRUCT = 0x0001
    private const val RW_TEXTURENATIVE = 0x0015
    private const val RW_TEXDICTIONARY = 0x0016

    private const val PLATFORM_D3D8 = 8
    private const val PLATFORM_D3D9 = 9

    private const val FORMAT_MASK_BASE = 0x0F00
    private const val FORMAT_1555 = 0x0100
    private const val FORMAT_565 = 0x0200
    private const val FORMAT_4444 = 0x0300
    private const val FORMAT_LUM8 = 0x0400
    private const val FORMAT_8888 = 0x0500
    private const val FORMAT_888 = 0x0600
    private const val FORMAT_555 = 0x0A00

    private const val FORMAT_EXT_PAL8 = 0x2000
    private const val FORMAT_EXT_PAL4 = 0x4000

    private const val FOURCC_DXT1 = 0x31545844
    private const val FOURCC_DXT3 = 0x33545844
    private const val FOURCC_DXT5 = 0x35545844

    fun listHeaders(bytes: ByteArray): List<TxdTextureHeader> {
        if (bytes.size < 12) return emptyList()
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val rootType = buf.int
        val rootSize = buf.int
        buf.int // libraryId
        if (rootType != RW_TEXDICTIONARY) return emptyList()

        val rootEnd = (12L + (rootSize.toLong() and 0xFFFFFFFFL)).coerceAtMost(bytes.size.toLong()).toInt()
        val headers = mutableListOf<TxdTextureHeader>()
        while (buf.position() + 12 <= rootEnd) {
            val chunkOffset = buf.position()
            val type = buf.int
            val size = buf.int
            buf.int // libraryId
            val chunkEnd = (buf.position().toLong() + (size.toLong() and 0xFFFFFFFFL))
                .coerceAtMost(rootEnd.toLong()).toInt()

            if (type == RW_TEXTURENATIVE && chunkEnd - buf.position() >= 12) {
                val childType = buf.int
                val childSize = buf.int
                buf.int // libraryId
                if (childType == RW_STRUCT && buf.position() + 88 <= chunkEnd) {
                    val structStart = buf.position()
                    buf.position(structStart + 8) // skip platformId + filterFlags
                    val name = readNullTerminatedAscii(buf, 32)
                    val maskName = readNullTerminatedAscii(buf, 32)
                    buf.position(structStart + 80) // after rasterFormat(4) + d3dFormat/hasAlpha(4)
                    val width = buf.short.toInt() and 0xFFFF
                    val height = buf.short.toInt() and 0xFFFF
                    if (name.isNotEmpty() && width > 0 && height > 0) {
                        headers.add(
                            TxdTextureHeader(
                                name = name,
                                maskName = maskName,
                                width = width,
                                height = height,
                                dataOffsetInFile = chunkOffset
                            )
                        )
                    }
                    buf.position((structStart.toLong() + (childSize.toLong() and 0xFFFFFFFFL)).coerceAtMost(chunkEnd.toLong()).toInt())
                }
            }
            buf.position(chunkEnd)
        }
        return headers
    }

    fun decodeAll(bytes: ByteArray): List<DecodedTxdTexture> {
        val headers = listHeaders(bytes)
        if (headers.isEmpty()) return emptyList()
        val result = ArrayList<DecodedTxdTexture>(headers.size)
        for (header in headers) {
            decodeAtOffset(bytes, header.dataOffsetInFile)?.let(result::add)
        }
        return result
    }

    fun decodeTextureByName(bytes: ByteArray, targetName: String): DecodedTxdTexture? {
        val header = listHeaders(bytes).firstOrNull {
            it.name.equals(targetName, ignoreCase = true)
        } ?: return null
        return decodeAtOffset(bytes, header.dataOffsetInFile)
    }

    fun decodeAtOffset(bytes: ByteArray, textureNativeOffset: Int): DecodedTxdTexture? {
        if (textureNativeOffset < 0 || textureNativeOffset + 12 > bytes.size) return null
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        buf.position(textureNativeOffset)
        val type = buf.int
        val size = buf.int
        buf.int
        if (type != RW_TEXTURENATIVE) return null

        val chunkEnd = (buf.position().toLong() + (size.toLong() and 0xFFFFFFFFL))
            .coerceAtMost(bytes.size.toLong()).toInt()
        if (buf.position() + 12 > chunkEnd) return null

        val structType = buf.int
        val structSize = buf.int
        buf.int
        if (structType != RW_STRUCT) return null

        val structEnd = (buf.position().toLong() + (structSize.toLong() and 0xFFFFFFFFL))
            .coerceAtMost(chunkEnd.toLong()).toInt()
        if (buf.position() + 88 > structEnd) return null

        val platformId = buf.int
        buf.int // filterFlags
        val name = readNullTerminatedAscii(buf, 32)
        val maskName = readNullTerminatedAscii(buf, 32)
        val rasterFormat = buf.int
        val d3dOrHasAlpha = buf.int
        val width = buf.short.toInt() and 0xFFFF
        val height = buf.short.toInt() and 0xFFFF
        val depth = buf.get().toInt() and 0xFF
        buf.get() // numLevels
        buf.get() // rasterType
        val flagsOrCompression = buf.get().toInt() and 0xFF

        if (width <= 0 || height <= 0 || width > 4096 || height > 4096) return null

        val isPal8 = (rasterFormat and FORMAT_EXT_PAL8) != 0
        val isPal4 = (rasterFormat and FORMAT_EXT_PAL4) != 0
        val baseFormat = rasterFormat and FORMAT_MASK_BASE

        val palette: IntArray? = when {
            isPal8 -> {
                if (buf.position() + 256 * 4 > structEnd) return null
                readPalette(buf, 256, baseFormat)
            }
            isPal4 -> {
                if (buf.position() + 16 * 4 > structEnd) return null
                readPalette(buf, 16, baseFormat)
            }
            else -> null
        }

        if (buf.position() + 4 > structEnd) return null
        val mip0Size = buf.int
        if (mip0Size <= 0 || buf.position() + mip0Size > structEnd) return null

        val pixelBytes = ByteArray(mip0Size)
        buf.get(pixelBytes)

        val dxtMode = detectDxtMode(platformId, d3dOrHasAlpha, flagsOrCompression)
        val pixels = when {
            dxtMode == 1 -> decodeDxt1(pixelBytes, width, height)
            dxtMode == 3 -> decodeDxt3(pixelBytes, width, height)
            dxtMode == 5 -> decodeDxt5(pixelBytes, width, height)
            isPal8 && palette != null -> decodePal8(pixelBytes, width, height, palette)
            isPal4 && palette != null -> decodePal4(pixelBytes, width, height, palette)
            baseFormat == FORMAT_8888 -> decodeBgra32(pixelBytes, width, height, forceOpaque = false)
            baseFormat == FORMAT_888 -> decodeBgra32(pixelBytes, width, height, forceOpaque = true)
            baseFormat == FORMAT_565 -> decodeRgb565(pixelBytes, width, height)
            baseFormat == FORMAT_1555 -> decodeArgb1555(pixelBytes, width, height)
            baseFormat == FORMAT_4444 -> decodeArgb4444(pixelBytes, width, height)
            baseFormat == FORMAT_555 -> decodeXrgb1555(pixelBytes, width, height)
            baseFormat == FORMAT_LUM8 -> decodeLum8(pixelBytes, width, height)
            depth == 32 -> decodeBgra32(pixelBytes, width, height, forceOpaque = false)
            depth == 16 -> decodeRgb565(pixelBytes, width, height)
            else -> return null
        }

        return DecodedTxdTexture(
            name = name,
            maskName = maskName,
            width = width,
            height = height,
            argbPixels = pixels
        )
    }

    private fun detectDxtMode(platformId: Int, d3dOrHasAlpha: Int, flagsOrCompression: Int): Int {
        if (platformId == PLATFORM_D3D9) {
            return when (d3dOrHasAlpha) {
                FOURCC_DXT1 -> 1
                FOURCC_DXT3 -> 3
                FOURCC_DXT5 -> 5
                else -> {
                    val isCompressed = (flagsOrCompression and 0x08) != 0
                    if (isCompressed && d3dOrHasAlpha == 0) 1 else 0
                }
            }
        }
        if (platformId == PLATFORM_D3D8) {
            return when (flagsOrCompression) {
                1 -> 1
                3 -> 3
                5 -> 5
                else -> 0
            }
        }
        return 0
    }

    private fun readNullTerminatedAscii(buf: ByteBuffer, maxLen: Int): String {
        val raw = ByteArray(maxLen)
        buf.get(raw)
        val zeroIndex = raw.indexOf(0)
        val end = if (zeroIndex >= 0) zeroIndex else maxLen
        return String(raw, 0, end, Charsets.ISO_8859_1).trim()
    }

    private fun readPalette(buf: ByteBuffer, count: Int, baseFormat: Int): IntArray {
        val forceOpaque = baseFormat == FORMAT_888 || baseFormat == FORMAT_555 || baseFormat == FORMAT_565
        val palette = IntArray(count)
        for (i in 0 until count) {
            val r = buf.get().toInt() and 0xFF
            val g = buf.get().toInt() and 0xFF
            val b = buf.get().toInt() and 0xFF
            val rawA = buf.get().toInt() and 0xFF
            val a = if (forceOpaque) 0xFF else rawA
            palette[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        return palette
    }

    private fun decodePal8(data: ByteArray, width: Int, height: Int, palette: IntArray): IntArray {
        val total = width * height
        val out = IntArray(total)
        val limit = minOf(total, data.size)
        for (i in 0 until limit) {
            val idx = data[i].toInt() and 0xFF
            out[i] = palette[idx]
        }
        return out
    }

    private fun decodePal4(data: ByteArray, width: Int, height: Int, palette: IntArray): IntArray {
        val total = width * height
        val out = IntArray(total)
        var px = 0
        for (byteVal in data) {
            if (px >= total) break
            val b = byteVal.toInt() and 0xFF
            out[px++] = palette[b and 0x0F]
            if (px < total) {
                out[px++] = palette[(b ushr 4) and 0x0F]
            }
        }
        return out
    }

    private fun decodeBgra32(data: ByteArray, width: Int, height: Int, forceOpaque: Boolean): IntArray {
        val total = width * height
        val out = IntArray(total)
        val limit = minOf(total, data.size / 4)
        for (i in 0 until limit) {
            val base = i * 4
            val b = data[base].toInt() and 0xFF
            val g = data[base + 1].toInt() and 0xFF
            val r = data[base + 2].toInt() and 0xFF
            val a = if (forceOpaque) 0xFF else (data[base + 3].toInt() and 0xFF)
            out[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        return out
    }

    private fun decodeRgb565(data: ByteArray, width: Int, height: Int): IntArray {
        val total = width * height
        val out = IntArray(total)
        val limit = minOf(total, data.size / 2)
        for (i in 0 until limit) {
            val v = (data[i * 2].toInt() and 0xFF) or ((data[i * 2 + 1].toInt() and 0xFF) shl 8)
            val r = ((v ushr 11) and 0x1F) * 255 / 31
            val g = ((v ushr 5) and 0x3F) * 255 / 63
            val b = (v and 0x1F) * 255 / 31
            out[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        return out
    }

    private fun decodeArgb1555(data: ByteArray, width: Int, height: Int): IntArray {
        val total = width * height
        val out = IntArray(total)
        val limit = minOf(total, data.size / 2)
        for (i in 0 until limit) {
            val v = (data[i * 2].toInt() and 0xFF) or ((data[i * 2 + 1].toInt() and 0xFF) shl 8)
            val a = if ((v and 0x8000) != 0) 0xFF else 0x00
            val r = ((v ushr 10) and 0x1F) * 255 / 31
            val g = ((v ushr 5) and 0x1F) * 255 / 31
            val b = (v and 0x1F) * 255 / 31
            out[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        return out
    }

    private fun decodeXrgb1555(data: ByteArray, width: Int, height: Int): IntArray {
        val total = width * height
        val out = IntArray(total)
        val limit = minOf(total, data.size / 2)
        for (i in 0 until limit) {
            val v = (data[i * 2].toInt() and 0xFF) or ((data[i * 2 + 1].toInt() and 0xFF) shl 8)
            val r = ((v ushr 10) and 0x1F) * 255 / 31
            val g = ((v ushr 5) and 0x1F) * 255 / 31
            val b = (v and 0x1F) * 255 / 31
            out[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        return out
    }

    private fun decodeArgb4444(data: ByteArray, width: Int, height: Int): IntArray {
        val total = width * height
        val out = IntArray(total)
        val limit = minOf(total, data.size / 2)
        for (i in 0 until limit) {
            val v = (data[i * 2].toInt() and 0xFF) or ((data[i * 2 + 1].toInt() and 0xFF) shl 8)
            val a = ((v ushr 12) and 0x0F) * 17
            val r = ((v ushr 8) and 0x0F) * 17
            val g = ((v ushr 4) and 0x0F) * 17
            val b = (v and 0x0F) * 17
            out[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        return out
    }

    private fun decodeLum8(data: ByteArray, width: Int, height: Int): IntArray {
        val total = width * height
        val out = IntArray(total)
        val limit = minOf(total, data.size)
        for (i in 0 until limit) {
            val lum = data[i].toInt() and 0xFF
            out[i] = (0xFF shl 24) or (lum shl 16) or (lum shl 8) or lum
        }
        return out
    }

    private fun decodeDxt1(data: ByteArray, width: Int, height: Int): IntArray {
        val out = IntArray(width * height)
        val blocksX = (width + 3) / 4
        val blocksY = (height + 3) / 4
        var offset = 0
        val colors = IntArray(4)

        for (by in 0 until blocksY) {
            for (bx in 0 until blocksX) {
                if (offset + 8 > data.size) return out
                val c0 = readU16(data, offset)
                val c1 = readU16(data, offset + 2)
                val indices = readU32(data, offset + 4)
                offset += 8

                val rgb0 = rgb565ToRgbTuple(c0)
                val rgb1 = rgb565ToRgbTuple(c1)
                colors[0] = packArgb(0xFF, rgb0[0], rgb0[1], rgb0[2])
                colors[1] = packArgb(0xFF, rgb1[0], rgb1[1], rgb1[2])
                if (c0 > c1) {
                    colors[2] = packArgb(0xFF, (2 * rgb0[0] + rgb1[0]) / 3, (2 * rgb0[1] + rgb1[1]) / 3, (2 * rgb0[2] + rgb1[2]) / 3)
                    colors[3] = packArgb(0xFF, (rgb0[0] + 2 * rgb1[0]) / 3, (rgb0[1] + 2 * rgb1[1]) / 3, (rgb0[2] + 2 * rgb1[2]) / 3)
                } else {
                    colors[2] = packArgb(0xFF, (rgb0[0] + rgb1[0]) / 2, (rgb0[1] + rgb1[1]) / 2, (rgb0[2] + rgb1[2]) / 2)
                    colors[3] = 0x00000000
                }

                for (py in 0 until 4) {
                    val y = by * 4 + py
                    if (y >= height) continue
                    for (px in 0 until 4) {
                        val x = bx * 4 + px
                        if (x >= width) continue
                        val bitIndex = (py * 4 + px) * 2
                        val code = ((indices ushr bitIndex) and 0x03L).toInt()
                        out[y * width + x] = colors[code]
                    }
                }
            }
        }
        return out
    }

    private fun decodeDxt3(data: ByteArray, width: Int, height: Int): IntArray {
        val out = IntArray(width * height)
        val blocksX = (width + 3) / 4
        val blocksY = (height + 3) / 4
        var offset = 0
        val colors = IntArray(4)

        for (by in 0 until blocksY) {
            for (bx in 0 until blocksX) {
                if (offset + 16 > data.size) return out
                val alphaOffset = offset
                val c0 = readU16(data, offset + 8)
                val c1 = readU16(data, offset + 10)
                val indices = readU32(data, offset + 12)
                offset += 16

                fillOpaqueDxtColors(c0, c1, colors)

                for (py in 0 until 4) {
                    val y = by * 4 + py
                    if (y >= height) continue
                    val alphaRow = readU16(data, alphaOffset + py * 2)
                    for (px in 0 until 4) {
                        val x = bx * 4 + px
                        if (x >= width) continue
                        val a4 = (alphaRow ushr (px * 4)) and 0x0F
                        val a = a4 * 17
                        val code = ((indices ushr ((py * 4 + px) * 2)) and 0x03L).toInt()
                        out[y * width + x] = (a shl 24) or (colors[code] and 0x00FFFFFF)
                    }
                }
            }
        }
        return out
    }

    private fun decodeDxt5(data: ByteArray, width: Int, height: Int): IntArray {
        val out = IntArray(width * height)
        val blocksX = (width + 3) / 4
        val blocksY = (height + 3) / 4
        var offset = 0
        val colors = IntArray(4)
        val alphas = IntArray(8)

        for (by in 0 until blocksY) {
            for (bx in 0 until blocksX) {
                if (offset + 16 > data.size) return out
                val a0 = data[offset].toInt() and 0xFF
                val a1 = data[offset + 1].toInt() and 0xFF
                var alphaBits = 0L
                for (i in 0 until 6) {
                    alphaBits = alphaBits or ((data[offset + 2 + i].toLong() and 0xFFL) shl (8 * i))
                }
                val c0 = readU16(data, offset + 8)
                val c1 = readU16(data, offset + 10)
                val indices = readU32(data, offset + 12)
                offset += 16

                alphas[0] = a0
                alphas[1] = a1
                if (a0 > a1) {
                    for (i in 1..6) {
                        alphas[i + 1] = ((7 - i) * a0 + i * a1) / 7
                    }
                } else {
                    for (i in 1..4) {
                        alphas[i + 1] = ((5 - i) * a0 + i * a1) / 5
                    }
                    alphas[6] = 0
                    alphas[7] = 255
                }

                fillOpaqueDxtColors(c0, c1, colors)

                for (py in 0 until 4) {
                    val y = by * 4 + py
                    if (y >= height) continue
                    for (px in 0 until 4) {
                        val x = bx * 4 + px
                        if (x >= width) continue
                        val idx = py * 4 + px
                        val aCode = ((alphaBits ushr (idx * 3)) and 0x07L).toInt()
                        val cCode = ((indices ushr (idx * 2)) and 0x03L).toInt()
                        out[y * width + x] = (alphas[aCode] shl 24) or (colors[cCode] and 0x00FFFFFF)
                    }
                }
            }
        }
        return out
    }

    private fun fillOpaqueDxtColors(c0: Int, c1: Int, outColors: IntArray) {
        val rgb0 = rgb565ToRgbTuple(c0)
        val rgb1 = rgb565ToRgbTuple(c1)
        outColors[0] = packArgb(0xFF, rgb0[0], rgb0[1], rgb0[2])
        outColors[1] = packArgb(0xFF, rgb1[0], rgb1[1], rgb1[2])
        outColors[2] = packArgb(0xFF, (2 * rgb0[0] + rgb1[0]) / 3, (2 * rgb0[1] + rgb1[1]) / 3, (2 * rgb0[2] + rgb1[2]) / 3)
        outColors[3] = packArgb(0xFF, (rgb0[0] + 2 * rgb1[0]) / 3, (rgb0[1] + 2 * rgb1[1]) / 3, (rgb0[2] + 2 * rgb1[2]) / 3)
    }

    private fun rgb565ToRgbTuple(c: Int): IntArray {
        val r = ((c ushr 11) and 0x1F) * 255 / 31
        val g = ((c ushr 5) and 0x3F) * 255 / 63
        val b = (c and 0x1F) * 255 / 31
        return intArrayOf(r, g, b)
    }

    private fun packArgb(a: Int, r: Int, g: Int, b: Int): Int =
        (a shl 24) or (r shl 16) or (g shl 8) or b

    private fun readU16(data: ByteArray, offset: Int): Int =
        (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)

    private fun readU32(data: ByteArray, offset: Int): Long =
        (data[offset].toLong() and 0xFFL) or
            ((data[offset + 1].toLong() and 0xFFL) shl 8) or
            ((data[offset + 2].toLong() and 0xFFL) shl 16) or
            ((data[offset + 3].toLong() and 0xFFL) shl 24)
}
