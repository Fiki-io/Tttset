package com.example.domain.assets

data class FontMetricsTable(
    val fontIndex: Int,
    val propWidths: IntArray,
    val unpropWidth: Int
) {
    fun charAdvance(asciiCode: Int, proportional: Boolean): Int {
        if (!proportional) return unpropWidth
        val index = (asciiCode - 32).coerceAtLeast(0)
        return if (index < propWidths.size) propWidths[index] else unpropWidth
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FontMetricsTable) return false
        return fontIndex == other.fontIndex &&
            unpropWidth == other.unpropWidth &&
            propWidths.contentEquals(other.propWidths)
    }

    override fun hashCode(): Int {
        var result = fontIndex
        result = 31 * result + propWidths.contentHashCode()
        result = 31 * result + unpropWidth
        return result
    }
}

data class FontsDatData(
    val tablesByFontId: Map<Int, FontMetricsTable>
) {
    fun tableForSampFont(sampFontId: Int): FontMetricsTable? {
        // In GTA SA fonts.dat, Font ID 0 (font1) & 1 (font2) hold character widths;
        // SA-MP fonts 0 (Diploma) & 2 (Pricedown) use texture font1 (table 0),
        // fonts 1 (Futura) & 3 (Bank Gothic) use texture font2 (table 1), or direct index if present.
        val textureSlot = when (sampFontId) {
            0, 2 -> 0
            else -> 1
        }
        return tablesByFontId[sampFontId] ?: tablesByFontId[textureSlot] ?: tablesByFontId.values.firstOrNull()
    }
}

object FontsDatParser {
    private enum class Section {
        NONE,
        FONT_ID,
        PROP,
        UNPROP,
        OTHER
    }

    fun parse(rawText: String): FontsDatData {
        val tables = mutableMapOf<Int, FontMetricsTable>()
        var currentFontId = 0
        var currentSection = Section.NONE
        val currentProp = mutableListOf<Int>()
        var currentUnprop = 16

        fun commitCurrent() {
            if (currentProp.isNotEmpty()) {
                tables[currentFontId] = FontMetricsTable(
                    fontIndex = currentFontId,
                    propWidths = currentProp.toIntArray(),
                    unpropWidth = currentUnprop
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
                        currentSection = Section.FONT_ID
                    }
                    "[PROP]" -> currentSection = Section.PROP
                    "[UNPROP]" -> currentSection = Section.UNPROP
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
                else -> Unit
            }
        }
        commitCurrent()
        return FontsDatData(tablesByFontId = tables)
    }
}
