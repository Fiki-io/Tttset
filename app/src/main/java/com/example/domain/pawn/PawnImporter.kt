package com.example.domain.pawn

import com.example.domain.model.TdAlignment
import com.example.domain.model.TdFont
import com.example.domain.model.TextDrawElement
import com.example.domain.samp.SampColorUtils

data class PawnImportResult(
    val elements: List<TextDrawElement>
)

object PawnImporter {
    private val createGlobalRegex = Regex(
        """^\s*([A-Za-z0-9_.\[\]]+)\s*=\s*TextDrawCreate\s*\((.+)\)\s*;?\s*$"""
    )
    private val createPlayerRegex = Regex(
        """^\s*([A-Za-z0-9_.\[\]]+)\s*=\s*CreatePlayerTextDraw\s*\((.+)\)\s*;?\s*$"""
    )
    private val callRegex = Regex(
        """^\s*(TextDraw\w+|PlayerTextDraw\w+)\s*\((.+)\)\s*;?\s*$"""
    )

    fun parse(rawCode: String, startId: Long = 1L, startZOrder: Int = 0): PawnImportResult {
        val cleaned = stripComments(rawCode)
        val elementsByVar = LinkedHashMap<String, TextDrawElement>()
        var nextId = startId
        var nextZ = startZOrder

        for (rawLine in cleaned.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            val globalMatch = createGlobalRegex.matchEntire(line)
            if (globalMatch != null) {
                val varName = globalMatch.groupValues[1].trim()
                val args = splitArgs(globalMatch.groupValues[2])
                if (args.size >= 3) {
                    val x = args[0].toFloatOrNull() ?: 320f
                    val y = args[1].toFloatOrNull() ?: 240f
                    val text = unquoteString(args[2])
                    elementsByVar[varName] = TextDrawElement(
                        id = nextId++,
                        varName = varName,
                        isPlayerTextDraw = false,
                        x = x,
                        y = y,
                        text = text,
                        zOrder = nextZ++
                    )
                }
                continue
            }

            val playerMatch = createPlayerRegex.matchEntire(line)
            if (playerMatch != null) {
                val varName = playerMatch.groupValues[1].trim()
                val args = splitArgs(playerMatch.groupValues[2])
                if (args.size >= 4) {
                    val x = args[1].toFloatOrNull() ?: 320f
                    val y = args[2].toFloatOrNull() ?: 240f
                    val text = unquoteString(args[3])
                    elementsByVar[varName] = TextDrawElement(
                        id = nextId++,
                        varName = varName,
                        isPlayerTextDraw = true,
                        x = x,
                        y = y,
                        text = text,
                        zOrder = nextZ++
                    )
                }
                continue
            }

            val callMatch = callRegex.matchEntire(line) ?: continue
            val fnName = callMatch.groupValues[1]
            val args = splitArgs(callMatch.groupValues[2])
            val isPlayerCall = fnName.startsWith("PlayerTextDraw")
            val varIndex = if (isPlayerCall) 1 else 0
            if (args.size <= varIndex) continue

            val targetVar = args[varIndex].trim()
            val current = elementsByVar[targetVar] ?: continue
            val params = args.drop(varIndex + 1)
            val normalizedFn = fnName.removePrefix("Player")

            val updated = applySetter(current, normalizedFn, params)
            elementsByVar[targetVar] = updated
        }

        return PawnImportResult(elements = elementsByVar.values.toList())
    }

    private fun applySetter(
        el: TextDrawElement,
        fn: String,
        params: List<String>
    ): TextDrawElement {
        return when (fn) {
            "TextDrawSetString" -> {
                val text = params.firstOrNull()?.let(::unquoteString) ?: return el
                el.copy(text = text)
            }
            "TextDrawFont" -> {
                val font = params.firstOrNull()?.let(::parseFont) ?: return el
                el.copy(font = font)
            }
            "TextDrawAlignment" -> {
                val align = params.firstOrNull()?.let(::parseAlignment) ?: return el
                el.copy(alignment = align)
            }
            "TextDrawLetterSize" -> {
                if (params.size < 2) return el
                val lx = params[0].toFloatOrNull() ?: el.letterSizeX
                val ly = params[1].toFloatOrNull() ?: el.letterSizeY
                el.copy(letterSizeX = lx, letterSizeY = ly)
            }
            "TextDrawTextSize" -> {
                if (params.size < 2) return el
                val tx = params[0].toFloatOrNull() ?: el.textSizeX
                val ty = params[1].toFloatOrNull() ?: el.textSizeY
                el.copy(textSizeX = tx, textSizeY = ty)
            }
            "TextDrawColor", "TextDrawColour" -> {
                val color = params.firstOrNull()?.let(SampColorUtils::parseColorToken) ?: return el
                el.copy(colorRgba = color)
            }
            "TextDrawBoxColor", "TextDrawBoxColour" -> {
                val color = params.firstOrNull()?.let(SampColorUtils::parseColorToken) ?: return el
                el.copy(boxColorRgba = color)
            }
            "TextDrawBackgroundColor", "TextDrawBackgroundColour" -> {
                val color = params.firstOrNull()?.let(SampColorUtils::parseColorToken) ?: return el
                el.copy(backgroundColorRgba = color)
            }
            "TextDrawUseBox" -> {
                val enabled = params.firstOrNull()?.let(::parseBool) ?: return el
                el.copy(useBox = enabled)
            }
            "TextDrawSetOutline" -> {
                val outline = params.firstOrNull()?.toIntOrNull() ?: return el
                el.copy(outline = outline.coerceAtLeast(0))
            }
            "TextDrawSetShadow" -> {
                val shadow = params.firstOrNull()?.toIntOrNull() ?: return el
                el.copy(shadow = shadow.coerceAtLeast(0))
            }
            "TextDrawSetProportional" -> {
                val prop = params.firstOrNull()?.let(::parseBool) ?: return el
                el.copy(proportional = prop)
            }
            "TextDrawSetSelectable" -> {
                val sel = params.firstOrNull()?.let(::parseBool) ?: return el
                el.copy(selectable = sel)
            }
            "TextDrawSetPreviewModel" -> {
                val model = params.firstOrNull()?.toIntOrNull() ?: return el
                el.copy(previewModel = model)
            }
            "TextDrawSetPreviewRot" -> {
                if (params.size < 4) return el
                el.copy(
                    previewRotX = params[0].toFloatOrNull() ?: el.previewRotX,
                    previewRotY = params[1].toFloatOrNull() ?: el.previewRotY,
                    previewRotZ = params[2].toFloatOrNull() ?: el.previewRotZ,
                    previewZoom = params[3].toFloatOrNull() ?: el.previewZoom
                )
            }
            "TextDrawSetPreviewVehCol", "TextDrawSetPreviewVehicleColours" -> {
                if (params.size < 2) return el
                el.copy(
                    previewVehCol1 = params[0].toIntOrNull() ?: el.previewVehCol1,
                    previewVehCol2 = params[1].toIntOrNull() ?: el.previewVehCol2
                )
            }
            else -> el
        }
    }

    private fun parseFont(raw: String): TdFont? {
        val trimmed = raw.trim()
        trimmed.toIntOrNull()?.let { return TdFont.fromInt(it) }
        return when (trimmed.uppercase()) {
            "TEXT_DRAW_FONT_0" -> TdFont.FONT_0
            "TEXT_DRAW_FONT_1" -> TdFont.FONT_1
            "TEXT_DRAW_FONT_2" -> TdFont.FONT_2
            "TEXT_DRAW_FONT_3" -> TdFont.FONT_3
            "TEXT_DRAW_FONT_SPRITE_DRAW", "4" -> TdFont.SPRITE_4
            "TEXT_DRAW_FONT_MODEL_PREVIEW", "5" -> TdFont.MODEL_PREVIEW_5
            else -> null
        }
    }

    private fun parseAlignment(raw: String): TdAlignment? {
        val trimmed = raw.trim()
        trimmed.toIntOrNull()?.let { return TdAlignment.fromInt(it) }
        return when (trimmed.uppercase()) {
            "TEXT_DRAW_ALIGN_LEFT" -> TdAlignment.LEFT
            "TEXT_DRAW_ALIGN_CENTER", "TEXT_DRAW_ALIGN_CENTRE" -> TdAlignment.CENTER
            "TEXT_DRAW_ALIGN_RIGHT" -> TdAlignment.RIGHT
            else -> null
        }
    }

    private fun parseBool(raw: String): Boolean {
        val t = raw.trim().lowercase()
        return t == "1" || t == "true"
    }

    private fun stripComments(code: String): String {
        val noBlock = code.replace(Regex("/\\*[\\s\\S]*?\\*/"), "")
        return noBlock.lineSequence().joinToString("\n") { line ->
            var inQuotes = false
            var escaped = false
            var cutIdx = line.length
            for (i in line.indices) {
                val c = line[i]
                if (escaped) {
                    escaped = false
                    continue
                }
                if (c == '\\' && inQuotes) {
                    escaped = true
                    continue
                }
                if (c == '"') {
                    inQuotes = !inQuotes
                    continue
                }
                if (!inQuotes && c == '/' && i + 1 < line.length && line[i + 1] == '/') {
                    cutIdx = i
                    break
                }
            }
            line.substring(0, cutIdx)
        }
    }

    private fun splitArgs(argStr: String): List<String> {
        val args = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var escaped = false
        var parenDepth = 0

        for (c in argStr) {
            if (escaped) {
                current.append(c)
                escaped = false
                continue
            }
            if (c == '\\' && inQuotes) {
                current.append(c)
                escaped = true
                continue
            }
            if (c == '"') {
                inQuotes = !inQuotes
                current.append(c)
                continue
            }
            if (!inQuotes) {
                when (c) {
                    '(' -> parenDepth++
                    ')' -> if (parenDepth > 0) parenDepth--
                    ',' -> if (parenDepth == 0) {
                        args.add(current.toString().trim())
                        current.clear()
                        continue
                    }
                }
            }
            current.append(c)
        }
        val tail = current.toString().trim()
        if (tail.isNotEmpty()) args.add(tail)
        return args
    }

    private fun unquoteString(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.length >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length - 1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
        }
        return trimmed
    }
}
