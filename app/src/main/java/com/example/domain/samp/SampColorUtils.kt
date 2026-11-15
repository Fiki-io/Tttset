package com.example.domain.samp

import androidx.compose.ui.graphics.Color
import java.util.Locale

data class ColoredTextRun(
    val text: String,
    val color: Color
)

object SampColorUtils {
    private val baseRed = Color(0xFFB4191D)
    private val baseGreen = Color(0xFF36682C)
    private val baseBlue = Color(0xFF323C7F)
    private val baseYellow = Color(0xFFE2C063)
    private val basePurple = Color(0xFFA86BB2)
    private val baseBlack = Color(0xFF000000)
    private val baseWhite = Color(0xFFE1E1E1)

    val quickPresetRgbaList = listOf(
        0xFFFFFFFFL, 0x000000FFL, 0x00000088L, 0x0A78D2FFL,
        0xB4191DFFL, 0x36682CFFL, 0xE2C063FFL, 0xA86BB2FFL
    )

    fun rgbaLongToComposeColor(rgba: Long): Color {
        val r = ((rgba ushr 24) and 0xFF).toInt()
        val g = ((rgba ushr 16) and 0xFF).toInt()
        val b = ((rgba ushr 8) and 0xFF).toInt()
        val a = (rgba and 0xFF).toInt()
        return Color(red = r, green = g, blue = b, alpha = a)
    }

    fun rgbaLongToArgbInt(rgba: Long): Int {
        val rgb = ((rgba ushr 8) and 0x00FFFFFFL).toInt()
        val a = (rgba and 0xFFL).toInt()
        return (a shl 24) or rgb
    }

    fun argbIntToRgbaLong(argb: Int): Long {
        val a = (argb ushr 24) and 0xFF
        val r = (argb ushr 16) and 0xFF
        val g = (argb ushr 8) and 0xFF
        val b = argb and 0xFF
        return ((r.toLong() shl 24) or (g.toLong() shl 16) or (b.toLong() shl 8) or a.toLong()) and 0xFFFFFFFFL
    }

    fun formatRgbaHex(rgba: Long): String {
        return String.format(Locale.US, "0x%08X", rgba and 0xFFFFFFFFL)
    }

    fun parseColorToken(raw: String): Long? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("0x", ignoreCase = true)) {
            val hex = trimmed.substring(2)
            if (hex.isEmpty() || hex.length > 8) return null
            return hex.toLongOrNull(16)?.and(0xFFFFFFFFL)
        }
        val decimal = trimmed.toLongOrNull() ?: return null
        return decimal and 0xFFFFFFFFL
    }

    fun parseHex8Strict(raw: String): Long? {
        val cleaned = raw.trim().removePrefix("0x").removePrefix("0X").removePrefix("#")
        if (cleaned.length != 8) return null
        return cleaned.toLongOrNull(16)?.and(0xFFFFFFFFL)
    }

    fun parseColorTagsToLines(input: String, defaultColorRgba: Long): List<List<ColoredTextRun>> {
        val defaultColor = rgbaLongToComposeColor(defaultColorRgba)
        val defaultAlpha = ((defaultColorRgba and 0xFFL) / 255f).coerceIn(0f, 1f)
        val lines = mutableListOf<MutableList<ColoredTextRun>>(mutableListOf())
        var currentColor = defaultColor
        var lightenCount = 0
        val buffer = StringBuilder()

        fun flushBuffer() {
            if (buffer.isNotEmpty()) {
                val effective = applyLighten(currentColor, lightenCount)
                lines.last().add(ColoredTextRun(buffer.toString(), effective))
                buffer.clear()
            }
        }

        var i = 0
        while (i < input.length) {
            if (input[i] == '~') {
                val close = input.indexOf('~', i + 1)
                if (close > i + 1 && close - i <= 4) {
                    val tag = input.substring(i + 1, close).lowercase(Locale.US)
                    when (tag) {
                        "n" -> {
                            flushBuffer()
                            lines.add(mutableListOf())
                            i = close + 1
                            continue
                        }
                        "r" -> {
                            flushBuffer()
                            currentColor = baseRed.copy(alpha = defaultAlpha)
                            lightenCount = 0
                            i = close + 1
                            continue
                        }
                        "g" -> {
                            flushBuffer()
                            currentColor = baseGreen.copy(alpha = defaultAlpha)
                            lightenCount = 0
                            i = close + 1
                            continue
                        }
                        "b" -> {
                            flushBuffer()
                            currentColor = baseBlue.copy(alpha = defaultAlpha)
                            lightenCount = 0
                            i = close + 1
                            continue
                        }
                        "y" -> {
                            flushBuffer()
                            currentColor = baseYellow.copy(alpha = defaultAlpha)
                            lightenCount = 0
                            i = close + 1
                            continue
                        }
                        "p" -> {
                            flushBuffer()
                            currentColor = basePurple.copy(alpha = defaultAlpha)
                            lightenCount = 0
                            i = close + 1
                            continue
                        }
                        "l" -> {
                            flushBuffer()
                            currentColor = baseBlack.copy(alpha = defaultAlpha)
                            lightenCount = 0
                            i = close + 1
                            continue
                        }
                        "w" -> {
                            flushBuffer()
                            currentColor = baseWhite.copy(alpha = defaultAlpha)
                            lightenCount = 0
                            i = close + 1
                            continue
                        }
                        "h" -> {
                            flushBuffer()
                            lightenCount = (lightenCount + 1).coerceAtMost(3)
                            i = close + 1
                            continue
                        }
                    }
                }
            }
            if (input[i] == '\n') {
                flushBuffer()
                lines.add(mutableListOf())
            } else {
                buffer.append(input[i])
            }
            i++
        }
        flushBuffer()
        return lines
    }

    private fun applyLighten(color: Color, steps: Int): Color {
        if (steps <= 0) return color
        var r = color.red
        var g = color.green
        var b = color.blue
        repeat(steps) {
            r += (1f - r) * 0.4f
            g += (1f - g) * 0.4f
            b += (1f - b) * 0.4f
        }
        return Color(red = r, green = g, blue = b, alpha = color.alpha)
    }
}
