package com.example.domain.assets

import kotlin.math.roundToInt

data class GlyphSlice(
    val srcX: Int,
    val srcY: Int,
    val width: Int,
    val height: Int
)

object SampFontAtlasLayout {
    private const val COLUMNS = 16
    private const val REFERENCE_HEIGHT = 512f
    private const val REFERENCE_ROW_HEIGHT = 40f
    private const val SUBFONT_DIGIT_START = 144
    private const val SUBFONT_COLON = 154
    private const val SUBFONT_ALPHA_START = 155
    private const val MAX_PRIMARY_CODE = 223

    fun usesFont1Sheet(sampFontId: Int): Boolean = sampFontId == 1 || sampFontId == 3

    fun usesUppercaseSubFont(sampFontId: Int): Boolean = sampFontId == 2 || sampFontId == 3

    fun resolveAtlasIndex(ch: Char, sampFontId: Int): Int {
        if (ch == ' ') return 0
        if (usesUppercaseSubFont(sampFontId)) {
            return when (ch) {
                in '0'..'9' -> SUBFONT_DIGIT_START + (ch - '0')
                ':' -> SUBFONT_COLON
                in 'A'..'Z' -> SUBFONT_ALPHA_START + (ch - 'A')
                in 'a'..'z' -> SUBFONT_ALPHA_START + (ch - 'a')
                else -> {
                    val code = ch.code
                    if (code in 33..MAX_PRIMARY_CODE) code - 32 else -1
                }
            }
        }
        val code = ch.code
        return if (code in 33..MAX_PRIMARY_CODE) code - 32 else -1
    }

    fun computeGlyphSlice(atlasWidth: Int, atlasHeight: Int, cellIdx: Int): GlyphSlice? {
        if (atlasWidth <= 0 || atlasHeight <= 0 || cellIdx < 0) return null
        val cellW = atlasWidth / COLUMNS
        if (cellW <= 0) return null
        val rowStepPx = atlasHeight * (REFERENCE_ROW_HEIGHT / REFERENCE_HEIGHT)
        if (rowStepPx <= 0f) return null

        val col = cellIdx % COLUMNS
        val row = cellIdx / COLUMNS
        val srcX = col * cellW
        val srcY = (row * rowStepPx).roundToInt()
        if (srcX + cellW > atlasWidth || srcY >= atlasHeight) return null

        val sliceH = rowStepPx.roundToInt().coerceAtMost(atlasHeight - srcY)
        if (sliceH <= 0) return null
        return GlyphSlice(srcX = srcX, srcY = srcY, width = cellW, height = sliceH)
    }
}

data class FontMetricsTable(
    val fontIndex: Int,
    val propWidths: IntArray,
    val unpropWidth: Int,
    val replacementSpaceWidth: Int? = null
) {
    fun charAdvance(asciiCode: Int, proportional: Boolean): Int {
        if (!proportional) return unpropWidth
        val index = (asciiCode - 32).coerceAtLeast(0)
        return if (index < propWidths.size) propWidths[index] else unpropWidth
    }

    fun charAdvance(ch: Char, sampFontId: Int, proportional: Boolean): Int {
        if (!proportional) return unpropWidth
        if (ch == ' ') {
            return replacementSpaceWidth?.takeIf { it > 0 }
                ?: propWidths.firstOrNull()
                ?: unpropWidth
        }
        val atlasIdx = SampFontAtlasLayout.resolveAtlasIndex(ch, sampFontId)
        if (atlasIdx in propWidths.indices) {
            return propWidths[atlasIdx]
        }
        val fallbackIdx = (ch.code - 32).coerceAtLeast(0)
        return if (fallbackIdx < propWidths.size) propWidths[fallbackIdx] else unpropWidth
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FontMetricsTable) return false
        return fontIndex == other.fontIndex &&
            unpropWidth == other.unpropWidth &&
            replacementSpaceWidth == other.replacementSpaceWidth &&
            propWidths.contentEquals(other.propWidths)
    }

    override fun hashCode(): Int {
        var result = fontIndex
        result = 31 * result + propWidths.contentHashCode()
        result = 31 * result + unpropWidth
        result = 31 * result + (replacementSpaceWidth ?: 0)
        return result
    }
}

data class FontsDatData(
    val tablesByFontId: Map<Int, FontMetricsTable>
) {
    fun tableForSampFont(sampFontId: Int): FontMetricsTable? {
        // Sprite[0] = font2 (table 0: FONT_0 Diploma & FONT_2 Slim/Menu)
        // Sprite[1] = font1 (table 1: FONT_1 Subtitles & FONT_3 Pricedown)
        val textureSlot = if (SampFontAtlasLayout.usesFont1Sheet(sampFontId)) 1 else 0
        if (tablesByFontId.size >= 4) {
            tablesByFontId[sampFontId]?.let { return it }
        }
        return tablesByFontId[textureSlot] ?: tablesByFontId[sampFontId] ?: tablesByFontId.values.firstOrNull()
    }
}

object FontsDatParser {
    private enum class Section {
        NONE,
        FONT_ID,
        PROP,
        UNPROP,
        REPLACEMENT_SPACE_CHAR,
        OTHER
    }

    fun parse(rawText: String): FontsDatData {
        val tables = mutableMapOf<Int, FontMetricsTable>()
        var currentFontId = 0
        var currentSection = Section.NONE
        val currentProp = mutableListOf<Int>()
        var currentUnprop = 16
        var currentSpaceChar: Int? = null

        fun commitCurrent() {
            if (currentProp.isNotEmpty()) {
                tables[currentFontId] = FontMetricsTable(
                    fontIndex = currentFontId,
                    propWidths = currentProp.toIntArray(),
                    unpropWidth = currentUnprop,
                    replacementSpaceWidth = currentSpaceChar
                )
            }
        }

        val lines = rawText.lineSequence()
        for (rawLine in lines) {
            val cleaned = rawLine.substringBefore('#').substringBefore(';').trim()
            if (cleaned.isEmpty()) continue

            if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
                when (cleaned.uppercase()) {
                    "[FONT_ID]" -> {
                        commitCurrent()
                        currentProp.clear()
                        currentUnprop = 16
                        currentSpaceChar = null
                        currentSection = Section.FONT_ID
                    }
                    "[PROP]" -> currentSection = Section.PROP
                    "[UNPROP]" -> currentSection = Section.UNPROP
                    "[REPLACEMENT_SPACE_CHAR]" -> currentSection = Section.REPLACEMENT_SPACE_CHAR
                    else -> currentSection = Section.OTHER
                }
                continue
            }

            val tokens = cleaned.split(Regex("\\s+")).filter { it.isNotEmpty() }
            when (currentSection) {
                Section.FONT_ID -> {
                    tokens.firstOrNull()?.toIntOrNull()?.let { currentFontId = it }
                }
                Section.PROP -> {
                    for (token in tokens) {
                        token.toIntOrNull()?.let { currentProp.add(it) }
                    }
                }
                Section.UNPROP -> {
                    tokens.firstOrNull()?.toIntOrNull()?.let { currentUnprop = it }
                }
                Section.REPLACEMENT_SPACE_CHAR -> {
                    tokens.firstOrNull()?.toIntOrNull()?.let { currentSpaceChar = it }
                }
                else -> Unit
            }
        }
        commitCurrent()
        return FontsDatData(tablesByFontId = tables)
    }
}
