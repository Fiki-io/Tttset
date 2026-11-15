package com.example.domain.samp

import com.example.core.AppConstants
import com.example.domain.assets.FontsDatData
import com.example.domain.model.TdAlignment
import com.example.domain.model.TdFont
import com.example.domain.model.TextDrawElement
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

data class VirtualRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(1f)
    val height: Float get() = (bottom - top).coerceAtLeast(1f)
}

data class WrappedLine(
    val runs: List<ColoredTextRun>,
    val plainText: String,
    val widthVirtual: Float
)

data class ComputedTextDrawLayout(
    val selectionRect: VirtualRect,
    val boxRect: VirtualRect?,
    val wrappedLines: List<WrappedLine>,
    val lineHeightVirtual: Float
)

object SampLayoutMath {
    private const val BOX_MARGIN_HORIZONTAL = 4f
    private const val BOX_MARGIN_TOP = 1.5f
    private const val BOX_MARGIN_BOTTOM = 2.5f
    const val FONT_SCALE_X_FACTOR = 1.35f
    const val GLYPH_HEIGHT_LINE_RATIO = 0.95f
    private const val FONT_SCALE_Y_FACTOR = 11.2f
    private const val DEFAULT_CHAR_ADVANCE = 15f

    fun snapValue(value: Float, snapEnabled: Boolean): Float {
        if (!snapEnabled) return roundToTenth(value)
        return round(value / AppConstants.SNAP_STEP_VIRTUAL) * AppConstants.SNAP_STEP_VIRTUAL
    }

    fun roundToTenth(value: Float): Float = round(value * 10f) / 10f

    fun roundToThousandth(value: Float): Float = round(value * 1000f) / 1000f

    fun charAdvanceVirtual(
        ch: Char,
        element: TextDrawElement,
        fontsDat: FontsDatData?
    ): Float {
        val table = fontsDat?.tableForSampFont(element.font.value)
        val scaleX = element.letterSizeX.coerceAtLeast(0.05f) * FONT_SCALE_X_FACTOR
        val rawAdvance = table?.charAdvance(ch, element.font.value, element.proportional)?.toFloat()
            ?: estimateFallbackAdvance(ch, element.font, element.proportional)
        return rawAdvance * scaleX
    }

    fun measureTextWidthVirtual(
        plainText: String,
        element: TextDrawElement,
        fontsDat: FontsDatData?
    ): Float {
        if (plainText.isEmpty()) return 0f
        var total = 0f
        for (ch in plainText) {
            total += charAdvanceVirtual(ch, element, fontsDat)
        }
        return total
    }

    private fun estimateFallbackAdvance(ch: Char, font: TdFont, proportional: Boolean): Float {
        if (!proportional) return 20f
        val effective = if (font == TdFont.FONT_2 || font == TdFont.FONT_3) ch.uppercaseChar() else ch
        return when (effective) {
            ' ', 'i', 'l', '!', '|', '.', ',', ':', ';', '\'' -> 10f
            'I', 'j', 't', 'f', 'r' -> 12f
            'm', 'M', 'w', 'W' -> 26f
            else -> DEFAULT_CHAR_ADVANCE
        }
    }

    fun computeLayout(element: TextDrawElement, fontsDat: FontsDatData?): ComputedTextDrawLayout {
        if (element.font == TdFont.SPRITE_4 || element.font == TdFont.MODEL_PREVIEW_5) {
            val w = element.textSizeX.coerceAtLeast(2f)
            val h = element.textSizeY.coerceAtLeast(2f)
            val rect = VirtualRect(
                left = element.x,
                top = element.y,
                right = element.x + w,
                bottom = element.y + h
            )
            return ComputedTextDrawLayout(
                selectionRect = rect,
                boxRect = if (element.useBox && element.font == TdFont.MODEL_PREVIEW_5) rect else null,
                wrappedLines = emptyList(),
                lineHeightVirtual = h
            )
        }

        val lineHeight = (element.letterSizeY.coerceAtLeast(0.05f) * FONT_SCALE_Y_FACTOR)
        val wrapWidthLimit = when (element.alignment) {
            TdAlignment.LEFT -> if (element.textSizeX <= element.x && !element.useBox) {
                (AppConstants.CANVAS_VIRTUAL_WIDTH - element.x).coerceAtLeast(32f)
            } else {
                (element.textSizeX - element.x).coerceAtLeast(8f)
            }
            TdAlignment.CENTER -> if (element.textSizeY <= 0f) {
                AppConstants.CANVAS_VIRTUAL_WIDTH
            } else {
                element.textSizeY.coerceAtLeast(8f)
            }
            TdAlignment.RIGHT -> if ((element.textSizeX <= 0f || element.textSizeX >= element.x) && !element.useBox) {
                element.x.coerceAtLeast(32f)
            } else {
                (element.x - element.textSizeX).coerceAtLeast(8f)
            }
        }

        val rawLines = SampColorUtils.parseColorTagsToLines(element.text, element.colorRgba)
        val wrapped = wrapLines(rawLines, wrapWidthLimit, element, fontsDat)
        val maxLineWidth = wrapped.maxOfOrNull { it.widthVirtual } ?: 0f
        val contentHeight = (wrapped.size.coerceAtLeast(1) * lineHeight)

        val boxRect = if (element.useBox) {
            when (element.alignment) {
                TdAlignment.LEFT -> {
                    val left = element.x - BOX_MARGIN_HORIZONTAL
                    val right = max(element.textSizeX + BOX_MARGIN_HORIZONTAL, element.x + 8f)
                    val bottom = max(
                        element.y + contentHeight + BOX_MARGIN_BOTTOM,
                        if (element.textSizeY > 0f && element.textSizeY > contentHeight) element.y + element.textSizeY else element.y + contentHeight + BOX_MARGIN_BOTTOM
                    )
                    VirtualRect(left, element.y - BOX_MARGIN_TOP, right, bottom)
                }
                TdAlignment.CENTER -> {
                    val halfW = (element.textSizeY.coerceAtLeast(8f) / 2f) + BOX_MARGIN_HORIZONTAL
                    val minHeight = max(contentHeight + BOX_MARGIN_BOTTOM, element.textSizeX.coerceAtLeast(0f))
                    VirtualRect(
                        left = element.x - halfW,
                        top = element.y - BOX_MARGIN_TOP,
                        right = element.x + halfW,
                        bottom = element.y + minHeight
                    )
                }
                TdAlignment.RIGHT -> {
                    val right = element.x + BOX_MARGIN_HORIZONTAL
                    val left = min(element.textSizeX - BOX_MARGIN_HORIZONTAL, element.x - 8f)
                    val bottom = max(
                        element.y + contentHeight + BOX_MARGIN_BOTTOM,
                        if (element.textSizeY > 0f && element.textSizeY > contentHeight) element.y + element.textSizeY else element.y + contentHeight + BOX_MARGIN_BOTTOM
                    )
                    VirtualRect(left, element.y - BOX_MARGIN_TOP, right, bottom)
                }
            }
        } else {
            null
        }

        val selectionRect = boxRect ?: when (element.alignment) {
            TdAlignment.LEFT -> VirtualRect(
                left = element.x,
                top = element.y,
                right = element.x + max(maxLineWidth, 24f),
                bottom = element.y + max(contentHeight, 12f)
            )
            TdAlignment.CENTER -> {
                val half = max(maxLineWidth / 2f, 16f)
                VirtualRect(
                    left = element.x - half,
                    top = element.y,
                    right = element.x + half,
                    bottom = element.y + max(contentHeight, 12f)
                )
            }
            TdAlignment.RIGHT -> VirtualRect(
                left = element.x - max(maxLineWidth, 24f),
                top = element.y,
                right = element.x,
                bottom = element.y + max(contentHeight, 12f)
            )
        }

        return ComputedTextDrawLayout(
            selectionRect = selectionRect,
            boxRect = boxRect,
            wrappedLines = wrapped,
            lineHeightVirtual = lineHeight
        )
    }

    private fun wrapLines(
        rawLines: List<List<ColoredTextRun>>,
        wrapWidthLimit: Float,
        element: TextDrawElement,
        fontsDat: FontsDatData?
    ): List<WrappedLine> {
        val output = mutableListOf<WrappedLine>()
        for (lineRuns in rawLines) {
            if (lineRuns.isEmpty()) {
                output.add(WrappedLine(emptyList(), "", 0f))
                continue
            }
            var currentRuns = mutableListOf<ColoredTextRun>()
            var currentPlain = StringBuilder()
            var currentWidth = 0f

            for (run in lineRuns) {
                val words = splitPreservingSpaces(run.text)
                for (token in words) {
                    val tokenW = measureTextWidthVirtual(token, element, fontsDat)
                    if (currentPlain.isNotEmpty() && currentWidth + tokenW > wrapWidthLimit && token.isNotBlank()) {
                        output.add(WrappedLine(currentRuns, currentPlain.toString().trimEnd(), currentWidth))
                        currentRuns = mutableListOf()
                        currentPlain = StringBuilder()
                        currentWidth = 0f
                    }
                    val trimmedToken = if (currentPlain.isEmpty()) token.trimStart() else token
                    if (trimmedToken.isNotEmpty()) {
                        val trimmedW = measureTextWidthVirtual(trimmedToken, element, fontsDat)
                        val prevLast = currentRuns.lastOrNull()
                        if (prevLast != null && prevLast.color == run.color) {
                            currentRuns[currentRuns.lastIndex] = prevLast.copy(text = prevLast.text + trimmedToken)
                        } else {
                            currentRuns.add(ColoredTextRun(trimmedToken, run.color))
                        }
                        currentPlain.append(trimmedToken)
                        currentWidth += trimmedW
                    }
                }
            }
            output.add(WrappedLine(currentRuns, currentPlain.toString(), currentWidth))
        }
        return output
    }

    private fun splitPreservingSpaces(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var inSpace = text[0].isWhitespace()
        for (ch in text) {
            val isWs = ch.isWhitespace()
            if (isWs != inSpace && current.isNotEmpty()) {
                tokens.add(current.toString())
                current.clear()
                inSpace = isWs
            }
            current.append(ch)
        }
        if (current.isNotEmpty()) tokens.add(current.toString())
        return tokens
    }

    fun applyResizeDelta(
        element: TextDrawElement,
        dxVirtual: Float,
        dyVirtual: Float
    ): TextDrawElement {
        return when {
            element.font == TdFont.SPRITE_4 || element.font == TdFont.MODEL_PREVIEW_5 -> {
                element.copy(
                    textSizeX = roundToTenth((element.textSizeX + dxVirtual).coerceAtLeast(4f)),
                    textSizeY = roundToTenth((element.textSizeY + dyVirtual).coerceAtLeast(4f))
                )
            }
            element.alignment == TdAlignment.LEFT -> {
                element.copy(
                    textSizeX = roundToTenth((element.textSizeX + dxVirtual).coerceAtLeast(element.x + 8f)),
                    textSizeY = roundToTenth((element.textSizeY + dyVirtual).coerceAtLeast(0f))
                )
            }
            element.alignment == TdAlignment.CENTER -> {
                element.copy(
                    textSizeY = roundToTenth((element.textSizeY + dxVirtual * 2f).coerceAtLeast(10f)),
                    textSizeX = roundToTenth((element.textSizeX + dyVirtual).coerceAtLeast(0f))
                )
            }
            else -> {
                element.copy(
                    textSizeX = roundToTenth((element.textSizeX + dxVirtual).coerceAtMost(element.x - 8f)),
                    textSizeY = roundToTenth((element.textSizeY + dyVirtual).coerceAtLeast(0f))
                )
            }
        }
    }
}
