package com.example.domain.pawn

import com.example.domain.model.PawnSyntaxMode
import com.example.domain.model.TdAlignment
import com.example.domain.model.TdFont
import com.example.domain.model.TextDrawElement
import com.example.domain.samp.SampColorUtils
import java.util.Locale

object PawnExporter {

    fun generate(
        elements: List<TextDrawElement>,
        syntaxMode: PawnSyntaxMode,
        includeShowCalls: Boolean
    ): String {
        if (elements.isEmpty()) return ""
        val sb = StringBuilder()

        val globals = elements.filter { !it.isPlayerTextDraw }
        val players = elements.filter { it.isPlayerTextDraw }

        if (globals.isNotEmpty()) {
            val invalidConst = if (syntaxMode == PawnSyntaxMode.OPEN_MP) "INVALID_TEXT_DRAW" else "Text:INVALID_TEXT_DRAW"
            for (el in globals) {
                val safeVar = sanitizeVarName(el.varName)
                sb.append("new Text:").append(safeVar).append(" = ").append(invalidConst).append(";\n")
            }
            sb.append("\n")
        }

        if (players.isNotEmpty()) {
            val invalidConst = if (syntaxMode == PawnSyntaxMode.OPEN_MP) "INVALID_PLAYER_TEXT_DRAW" else "PlayerText:INVALID_TEXT_DRAW"
            for (el in players) {
                val safeVar = sanitizeVarName(el.varName)
                sb.append("new PlayerText:").append(safeVar).append(" = ").append(invalidConst).append(";\n")
            }
            sb.append("\n")
        }

        for ((index, el) in elements.withIndex()) {
            appendElementCreation(sb, el, syntaxMode, includeShowCalls)
            if (index < elements.lastIndex) {
                sb.append("\n")
            }
        }
        return sb.toString()
    }

    private fun appendElementCreation(
        sb: StringBuilder,
        el: TextDrawElement,
        syntaxMode: PawnSyntaxMode,
        includeShowCalls: Boolean
    ) {
        val v = sanitizeVarName(el.varName)
        val escapedText = escapePawnString(el.text)
        val xStr = fmt(el.x)
        val yStr = fmt(el.y)

        if (!el.isPlayerTextDraw) {
            sb.append(v).append(" = TextDrawCreate(").append(xStr).append(", ").append(yStr).append(", \"").append(escapedText).append("\");\n")
            sb.append("TextDrawFont(").append(v).append(", ").append(formatFontArg(el.font, syntaxMode)).append(");\n")
            sb.append("TextDrawLetterSize(").append(v).append(", ").append(fmt(el.letterSizeX)).append(", ").append(fmt(el.letterSizeY)).append(");\n")
            sb.append("TextDrawTextSize(").append(v).append(", ").append(fmt(el.textSizeX)).append(", ").append(fmt(el.textSizeY)).append(");\n")
            sb.append("TextDrawSetOutline(").append(v).append(", ").append(el.outline).append(");\n")
            sb.append("TextDrawSetShadow(").append(v).append(", ").append(el.shadow).append(");\n")
            sb.append("TextDrawAlignment(").append(v).append(", ").append(formatAlignArg(el.alignment, syntaxMode)).append(");\n")
            val colorFn = if (syntaxMode == PawnSyntaxMode.OPEN_MP) "TextDrawColour" else "TextDrawColor"
            val bgFn = if (syntaxMode == PawnSyntaxMode.OPEN_MP) "TextDrawBackgroundColour" else "TextDrawBackgroundColor"
            val boxFn = if (syntaxMode == PawnSyntaxMode.OPEN_MP) "TextDrawBoxColour" else "TextDrawBoxColor"
            sb.append(colorFn).append("(").append(v).append(", ").append(SampColorUtils.formatRgbaHex(el.colorRgba)).append(");\n")
            sb.append(bgFn).append("(").append(v).append(", ").append(SampColorUtils.formatRgbaHex(el.backgroundColorRgba)).append(");\n")
            sb.append(boxFn).append("(").append(v).append(", ").append(SampColorUtils.formatRgbaHex(el.boxColorRgba)).append(");\n")
            sb.append("TextDrawUseBox(").append(v).append(", ").append(formatBoolArg(el.useBox, syntaxMode)).append(");\n")
            sb.append("TextDrawSetProportional(").append(v).append(", ").append(formatBoolArg(el.proportional, syntaxMode)).append(");\n")
            sb.append("TextDrawSetSelectable(").append(v).append(", ").append(formatBoolArg(el.selectable, syntaxMode)).append(");\n")
            if (el.font == TdFont.MODEL_PREVIEW_5) {
                sb.append("TextDrawSetPreviewModel(").append(v).append(", ").append(el.previewModel).append(");\n")
                sb.append("TextDrawSetPreviewRot(").append(v).append(", ")
                    .append(fmt(el.previewRotX)).append(", ")
                    .append(fmt(el.previewRotY)).append(", ")
                    .append(fmt(el.previewRotZ)).append(", ")
                    .append(fmt(el.previewZoom)).append(");\n")
                if (el.previewVehCol1 >= 0 || el.previewVehCol2 >= 0) {
                    val vehColFn = if (syntaxMode == PawnSyntaxMode.OPEN_MP) "TextDrawSetPreviewVehicleColours" else "TextDrawSetPreviewVehCol"
                    sb.append(vehColFn).append("(").append(v).append(", ").append(el.previewVehCol1).append(", ").append(el.previewVehCol2).append(");\n")
                }
            }
            if (includeShowCalls) {
                sb.append("TextDrawShowForPlayer(playerid, ").append(v).append(");\n")
            }
        } else {
            sb.append(v).append(" = CreatePlayerTextDraw(playerid, ").append(xStr).append(", ").append(yStr).append(", \"").append(escapedText).append("\");\n")
            sb.append("PlayerTextDrawFont(playerid, ").append(v).append(", ").append(formatFontArg(el.font, syntaxMode)).append(");\n")
            sb.append("PlayerTextDrawLetterSize(playerid, ").append(v).append(", ").append(fmt(el.letterSizeX)).append(", ").append(fmt(el.letterSizeY)).append(");\n")
            sb.append("PlayerTextDrawTextSize(playerid, ").append(v).append(", ").append(fmt(el.textSizeX)).append(", ").append(fmt(el.textSizeY)).append(");\n")
            sb.append("PlayerTextDrawSetOutline(playerid, ").append(v).append(", ").append(el.outline).append(");\n")
            sb.append("PlayerTextDrawSetShadow(playerid, ").append(v).append(", ").append(el.shadow).append(");\n")
            sb.append("PlayerTextDrawAlignment(playerid, ").append(v).append(", ").append(formatAlignArg(el.alignment, syntaxMode)).append(");\n")
            val colorFn = if (syntaxMode == PawnSyntaxMode.OPEN_MP) "PlayerTextDrawColour" else "PlayerTextDrawColor"
            val bgFn = if (syntaxMode == PawnSyntaxMode.OPEN_MP) "PlayerTextDrawBackgroundColour" else "PlayerTextDrawBackgroundColor"
            val boxFn = if (syntaxMode == PawnSyntaxMode.OPEN_MP) "PlayerTextDrawBoxColour" else "PlayerTextDrawBoxColor"
            sb.append(colorFn).append("(playerid, ").append(v).append(", ").append(SampColorUtils.formatRgbaHex(el.colorRgba)).append(");\n")
            sb.append(bgFn).append("(playerid, ").append(v).append(", ").append(SampColorUtils.formatRgbaHex(el.backgroundColorRgba)).append(");\n")
            sb.append(boxFn).append("(playerid, ").append(v).append(", ").append(SampColorUtils.formatRgbaHex(el.boxColorRgba)).append(");\n")
            sb.append("PlayerTextDrawUseBox(playerid, ").append(v).append(", ").append(formatBoolArg(el.useBox, syntaxMode)).append(");\n")
            sb.append("PlayerTextDrawSetProportional(playerid, ").append(v).append(", ").append(formatBoolArg(el.proportional, syntaxMode)).append(");\n")
            sb.append("PlayerTextDrawSetSelectable(playerid, ").append(v).append(", ").append(formatBoolArg(el.selectable, syntaxMode)).append(");\n")
            if (el.font == TdFont.MODEL_PREVIEW_5) {
                sb.append("PlayerTextDrawSetPreviewModel(playerid, ").append(v).append(", ").append(el.previewModel).append(");\n")
                sb.append("PlayerTextDrawSetPreviewRot(playerid, ").append(v).append(", ")
                    .append(fmt(el.previewRotX)).append(", ")
                    .append(fmt(el.previewRotY)).append(", ")
                    .append(fmt(el.previewRotZ)).append(", ")
                    .append(fmt(el.previewZoom)).append(");\n")
                if (el.previewVehCol1 >= 0 || el.previewVehCol2 >= 0) {
                    val vehColFn = if (syntaxMode == PawnSyntaxMode.OPEN_MP) "PlayerTextDrawSetPreviewVehicleColours" else "PlayerTextDrawSetPreviewVehCol"
                    sb.append(vehColFn).append("(playerid, ").append(v).append(", ").append(el.previewVehCol1).append(", ").append(el.previewVehCol2).append(");\n")
                }
            }
            if (includeShowCalls) {
                sb.append("PlayerTextDrawShow(playerid, ").append(v).append(");\n")
            }
        }
    }

    private fun formatFontArg(font: TdFont, mode: PawnSyntaxMode): String {
        if (mode == PawnSyntaxMode.SAMP_CLASSIC) return font.value.toString()
        return when (font) {
            TdFont.FONT_0 -> "TEXT_DRAW_FONT_0"
            TdFont.FONT_1 -> "TEXT_DRAW_FONT_1"
            TdFont.FONT_2 -> "TEXT_DRAW_FONT_2"
            TdFont.FONT_3 -> "TEXT_DRAW_FONT_3"
            TdFont.SPRITE_4 -> "TEXT_DRAW_FONT_SPRITE_DRAW"
            TdFont.MODEL_PREVIEW_5 -> "TEXT_DRAW_FONT_MODEL_PREVIEW"
        }
    }

    private fun formatAlignArg(align: TdAlignment, mode: PawnSyntaxMode): String {
        if (mode == PawnSyntaxMode.SAMP_CLASSIC) return align.value.toString()
        return when (align) {
            TdAlignment.LEFT -> "TEXT_DRAW_ALIGN_LEFT"
            TdAlignment.CENTER -> "TEXT_DRAW_ALIGN_CENTER"
            TdAlignment.RIGHT -> "TEXT_DRAW_ALIGN_RIGHT"
        }
    }

    private fun formatBoolArg(value: Boolean, mode: PawnSyntaxMode): String {
        return if (mode == PawnSyntaxMode.OPEN_MP) {
            if (value) "true" else "false"
        } else {
            if (value) "1" else "0"
        }
    }

    private fun fmt(value: Float): String {
        return String.format(Locale.US, "%.3f", value)
            .trimEnd('0')
            .let { if (it.endsWith(".")) "${it}0" else it }
    }

    private fun escapePawnString(raw: String): String {
        return raw
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "~n~")
            .replace("\r", "")
    }

    private fun sanitizeVarName(raw: String): String {
        val cleaned = raw.trim().replace(Regex("\\s+"), "_")
        return cleaned.ifEmpty { "tdElement" }
    }
}
