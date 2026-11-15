package com.example

import com.example.domain.assets.FontsDatParser
import com.example.domain.assets.SampFontAtlasLayout
import com.example.domain.assets.TxdParser
import com.example.domain.model.PawnSyntaxMode
import com.example.domain.model.TdAlignment
import com.example.domain.model.TdFont
import com.example.domain.model.TextDrawElement
import com.example.domain.pawn.PawnExporter
import com.example.domain.pawn.PawnImporter
import com.example.domain.samp.SampColorUtils
import com.example.domain.samp.SampLayoutMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TdEngineUnitTest {

    @Test
    fun sampColorUtils_hexAndTags_workAccurately() {
        assertEquals(0xFF0000FFL, SampColorUtils.parseColorToken("0xFF0000FF"))
        assertEquals("0x0A78D2FF", SampColorUtils.formatRgbaHex(0x0A78D2FFL))

        val lines = SampColorUtils.parseColorTagsToLines("~r~Warn~n~~w~OK", 0xFFFFFFFFL)
        assertEquals(2, lines.size)
        assertEquals("Warn", lines[0].first().text)
        assertEquals("OK", lines[1].first().text)
    }

    @Test
    fun pawnExporterAndImporter_roundTripClassicAndOpenMp() {
        val original = listOf(
            TextDrawElement(
                id = 1L,
                varName = "gHudBox",
                isPlayerTextDraw = false,
                x = 120f,
                y = 200f,
                text = "HUD ~g~Ready",
                font = TdFont.FONT_2,
                alignment = TdAlignment.CENTER,
                letterSizeX = 0.4f,
                letterSizeY = 1.6f,
                textSizeX = 24f,
                textSizeY = 180f,
                colorRgba = 0xFFFFFFFFL,
                boxColorRgba = 0x000000AAL,
                backgroundColorRgba = 0x000000FFL,
                useBox = true,
                outline = 1,
                shadow = 0,
                proportional = true,
                selectable = true
            ),
            TextDrawElement(
                id = 2L,
                varName = "pCarPreview[0]",
                isPlayerTextDraw = true,
                x = 300f,
                y = 100f,
                text = "",
                font = TdFont.MODEL_PREVIEW_5,
                alignment = TdAlignment.LEFT,
                textSizeX = 96f,
                textSizeY = 96f,
                previewModel = 411,
                previewRotX = -16f,
                previewRotY = 0f,
                previewRotZ = -55f,
                previewZoom = 1.1f,
                previewVehCol1 = 3,
                previewVehCol2 = 1
            )
        )

        val classicCode = PawnExporter.generate(original, PawnSyntaxMode.SAMP_CLASSIC, includeShowCalls = false)
        assertTrue(classicCode.contains("TextDrawColor(gHudBox, 0xFFFFFFFF);"))
        assertTrue(classicCode.contains("PlayerTextDrawSetPreviewVehCol(playerid, pCarPreview[0], 3, 1);"))

        val ompCode = PawnExporter.generate(original, PawnSyntaxMode.OPEN_MP, includeShowCalls = false)
        assertTrue(ompCode.contains("TextDrawColour(gHudBox, 0xFFFFFFFF);"))
        assertTrue(ompCode.contains("TextDrawFont(gHudBox, TEXT_DRAW_FONT_2);"))
        assertTrue(ompCode.contains("PlayerTextDrawSetPreviewVehicleColours(playerid, pCarPreview[0], 3, 1);"))

        val reimported = PawnImporter.parse(ompCode).elements
        assertEquals(2, reimported.size)
        assertEquals("gHudBox", reimported[0].varName)
        assertEquals(TdAlignment.CENTER, reimported[0].alignment)
        assertEquals(0x000000AAL, reimported[0].boxColorRgba)
        assertTrue(reimported[1].isPlayerTextDraw)
        assertEquals(TdFont.MODEL_PREVIEW_5, reimported[1].font)
        assertEquals(411, reimported[1].previewModel)
        assertEquals(3, reimported[1].previewVehCol1)
    }

    @Test
    fun fontsDatParser_readsPropUnpropAndSubFontTables() {
        val propValues = IntArray(181) { 20 }
        propValues[0] = 12 // space
        propValues[3] = 18
        propValues[144] = 27 // subfont '0'
        propValues[154] = 18 // subfont ':'
        propValues[155] = 29 // subfont 'A'
        val table0PropLine = propValues.joinToString(" ")
        val sample = """
            # GTA SA fonts.dat sample
            [FONT_ID]
            0
            [PROP]
            $table0PropLine
            [UNPROP]
            19
            [REPLACEMENT_SPACE_CHAR]
            10
            [FONT_ID]
            1
            [PROP]
            10 11 12 13
            [UNPROP]
            15
        """.trimIndent()

        val parsed = FontsDatParser.parse(sample)
        val font0 = parsed.tableForSampFont(0)
        val font1 = parsed.tableForSampFont(1)
        val font2 = parsed.tableForSampFont(2)
        val font3 = parsed.tableForSampFont(3)
        assertNotNull(font0)
        assertNotNull(font1)
        assertEquals(0, font0!!.fontIndex)
        assertEquals(1, font1!!.fontIndex)
        assertEquals(0, font2!!.fontIndex)
        assertEquals(1, font3!!.fontIndex)
        assertEquals(10, font0.charAdvance(' ', 0, proportional = true))
        assertEquals(19, font0.charAdvance(32, proportional = false))
        assertEquals(27, font2.charAdvance('0', 2, proportional = true))
        assertEquals(18, font2.charAdvance(':', 2, proportional = true))
        assertEquals(29, font2.charAdvance('a', 2, proportional = true))
        assertEquals(13, font1.charAdvance(35, proportional = true))
    }

    @Test
    fun sampFontAtlasLayout_uses40pxRowPitchAndCorrectSheetAndSubfontIndices() {
        assertEquals(false, SampFontAtlasLayout.usesFont1Sheet(0))
        assertEquals(true, SampFontAtlasLayout.usesFont1Sheet(1))
        assertEquals(false, SampFontAtlasLayout.usesFont1Sheet(2))
        assertEquals(true, SampFontAtlasLayout.usesFont1Sheet(3))

        // Primary font 'r' -> cellIdx 82 (row 5, col 2) -> srcX = 64, srcY = 5 * 40 = 200 on 512x512
        val rIdx = SampFontAtlasLayout.resolveAtlasIndex('r', sampFontId = 1)
        assertEquals(82, rIdx)
        val rSlice = SampFontAtlasLayout.computeGlyphSlice(512, 512, rIdx)
        assertNotNull(rSlice)
        assertEquals(64, rSlice!!.srcX)
        assertEquals(200, rSlice.srcY)
        assertEquals(32, rSlice.width)
        assertEquals(40, rSlice.height)

        // Subfont FONT_3 '0' -> cellIdx 144 (row 9, col 0) -> srcY = 360 on 512x512
        val zeroIdx = SampFontAtlasLayout.resolveAtlasIndex('0', sampFontId = 3)
        assertEquals(144, zeroIdx)
        val zeroSlice = SampFontAtlasLayout.computeGlyphSlice(512, 512, zeroIdx)!!
        assertEquals(0, zeroSlice.srcX)
        assertEquals(360, zeroSlice.srcY)
        assertEquals(32, zeroSlice.width)
        assertEquals(40, zeroSlice.height)

        // Subfont lowercase 'a' folds to 'A' (155) -> row 9, col 11 -> srcX = 352, srcY = 360
        assertEquals(155, SampFontAtlasLayout.resolveAtlasIndex('a', sampFontId = 3))
        assertEquals(155, SampFontAtlasLayout.resolveAtlasIndex('A', sampFontId = 2))
        assertEquals(180, SampFontAtlasLayout.resolveAtlasIndex('z', sampFontId = 2))
    }

    @Test
    fun sampLayoutMath_centerAlignmentSwapsTextSizeRoles() {
        val el = TextDrawElement(
            id = 1L,
            varName = "tdCenter",
            x = 320f,
            y = 100f,
            text = "Centered",
            alignment = TdAlignment.CENTER,
            textSizeX = 30f,
            textSizeY = 160f,
            useBox = true
        )
        val layout = SampLayoutMath.computeLayout(el, fontsDat = null)
        val box = layout.boxRect!!
        // Center alignment uses textSizeY (160) as box width (+8 margin total)
        assertEquals(320f - 84f, box.left, 0.5f)
        assertEquals(320f + 84f, box.right, 0.5f)
    }

    @Test
    fun pawnImporter_withoutTextSize_doesNotWrapRightEdgeTextVertically() {
        val code = """
            new PlayerText:gSpeedText = INVALID_PLAYER_TEXT_DRAW;
            gSpeedText = CreatePlayerTextDraw(playerid, 580.0, 420.0, "100km");
            PlayerTextDrawFont(playerid, gSpeedText, 2);
            PlayerTextDrawLetterSize(playerid, gSpeedText, 0.35, 1.8);
        """.trimIndent()
        val parsed = PawnImporter.parse(code).elements
        assertEquals(1, parsed.size)
        val el = parsed.first()
        val layout = SampLayoutMath.computeLayout(el, fontsDat = null)
        assertEquals(1, layout.wrappedLines.size)
        assertEquals("100km", layout.wrappedLines.first().plainText)

        val expectedWidth = "100km".sumOf { ch ->
            SampLayoutMath.charAdvanceVirtual(ch, el, fontsDat = null).toDouble()
        }.toFloat()
        assertEquals(expectedWidth, SampLayoutMath.measureTextWidthVirtual("100km", el, fontsDat = null), 0.01f)
    }

    @Test
    fun txdParser_decodesSyntheticBgra32AndPal8Chunks() {
        val bgraTxd = buildSyntheticTxd(
            name = "test_bgra",
            width = 2,
            height = 2,
            rasterFormat = 0x0500, // FORMAT_8888
            d3dFormat = 0,
            palette = null,
            pixelData = byteArrayOf(
                0x11, 0x22, 0x33, 0xFF.toByte(), // BGRA -> ARGB 0xFF332211
                0x00, 0xFF.toByte(), 0x00, 0x80.toByte(), // BGRA -> ARGB 0x8000FF00
                0xFF.toByte(), 0x00, 0x00, 0xFF.toByte(),
                0x00, 0x00, 0xFF.toByte(), 0xFF.toByte()
            )
        )
        val decodedBgra = TxdParser.decodeTextureByName(bgraTxd, "TEST_BGRA")
        assertNotNull(decodedBgra)
        assertEquals(2, decodedBgra!!.width)
        assertEquals(0xFF332211.toInt(), decodedBgra.argbPixels[0])
        assertEquals(0x8000FF00.toInt(), decodedBgra.argbPixels[1])

        val pal = ByteArray(256 * 4)
        pal[0] = 0xAA.toByte() // R
        pal[1] = 0xBB.toByte() // G
        pal[2] = 0xCC.toByte() // B
        pal[3] = 0xFF.toByte() // A
        val palTxd = buildSyntheticTxd(
            name = "pal_tex",
            width = 2,
            height = 1,
            rasterFormat = 0x2500, // PAL8 | 8888
            d3dFormat = 0,
            palette = pal,
            pixelData = byteArrayOf(0, 0)
        )
        val decodedPal = TxdParser.decodeTextureByName(palTxd, "pal_tex")
        assertNotNull(decodedPal)
        assertEquals(0xFFAABBCC.toInt(), decodedPal!!.argbPixels[0])
    }

    private fun buildSyntheticTxd(
        name: String,
        width: Int,
        height: Int,
        rasterFormat: Int,
        d3dFormat: Int,
        palette: ByteArray?,
        pixelData: ByteArray
    ): ByteArray {
        val structPayloadSize = 88 + (palette?.size ?: 0) + 4 + pixelData.size
        val texNativePayloadSize = 12 + structPayloadSize
        val rootPayloadSize = 12 + texNativePayloadSize

        val buf = ByteBuffer.allocate(12 + rootPayloadSize).order(ByteOrder.LITTLE_ENDIAN)
        // TEXDICTIONARY
        buf.putInt(0x0016)
        buf.putInt(rootPayloadSize)
        buf.putInt(0x1803FFFF)
        // TEXTURENATIVE
        buf.putInt(0x0015)
        buf.putInt(texNativePayloadSize)
        buf.putInt(0x1803FFFF)
        // STRUCT
        buf.putInt(0x0001)
        buf.putInt(structPayloadSize)
        buf.putInt(0x1803FFFF)

        buf.putInt(9) // D3D9
        buf.putInt(0) // filterFlags
        val nameBytes = ByteArray(32)
        name.toByteArray(Charsets.ISO_8859_1).copyInto(nameBytes)
        buf.put(nameBytes)
        buf.put(ByteArray(32)) // maskName
        buf.putInt(rasterFormat)
        buf.putInt(d3dFormat)
        buf.putShort(width.toShort())
        buf.putShort(height.toShort())
        buf.put(32.toByte())
        buf.put(1.toByte())
        buf.put(4.toByte())
        buf.put(1.toByte())
        if (palette != null) buf.put(palette)
        buf.putInt(pixelData.size)
        buf.put(pixelData)
        return buf.array()
    }
}
