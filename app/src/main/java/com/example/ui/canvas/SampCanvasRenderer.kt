package com.example.ui.canvas

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.AppConstants
import com.example.domain.assets.FontsDatData
import com.example.domain.assets.SampFontAtlasLayout
import com.example.domain.model.CanvasAspectRatio
import com.example.domain.model.TdAlignment
import com.example.domain.model.TdFont
import com.example.domain.model.TextDrawElement
import com.example.domain.samp.ComputedTextDrawLayout
import com.example.domain.samp.SampColorUtils
import com.example.domain.samp.SampLayoutMath
import com.example.domain.samp.VirtualRect
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppDimens
import kotlin.math.roundToInt

private enum class DragMode {
    NONE,
    MOVE,
    RESIZE
}

@Composable
fun SampCanvasViewport(
    elements: List<TextDrawElement>,
    selectedElementId: Long?,
    aspectRatio: CanvasAspectRatio,
    showGrid: Boolean,
    loadedSprites: Map<String, ImageBitmap>,
    fontAtlas1: ImageBitmap?,
    fontAtlas2: ImageBitmap?,
    fontsDat: FontsDatData?,
    onSelectElement: (Long?) -> Unit,
    onBeginGesture: () -> Unit,
    onDragElement: (Long, Float, Float) -> Unit,
    onResizeElement: (Long, Float, Float) -> Unit,
    onEndGesture: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spriteLabels = elements
        .filter { it.font == TdFont.SPRITE_4 }
        .associate { it.id to stringResource(R.string.canvas_sprite_missing, it.text) }
    val modelLabels = elements
        .filter { it.font == TdFont.MODEL_PREVIEW_5 }
        .associate { it.id to stringResource(R.string.canvas_font5_label, it.previewModel) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.CanvasBackdrop),
        contentAlignment = Alignment.Center
    ) {
        val availRatio = if (maxHeight > 0.dp) maxWidth / maxHeight else aspectRatio.ratioFloat
        val targetRatio = aspectRatio.ratioFloat
        val canvasSizingModifier = if (availRatio > targetRatio) {
            Modifier.fillMaxHeight().aspectRatio(targetRatio)
        } else {
            Modifier.fillMaxWidth().aspectRatio(targetRatio)
        }

        val currentElements by rememberUpdatedState(elements)
        val currentSelectedId by rememberUpdatedState(selectedElementId)
        val currentFontsDat by rememberUpdatedState(fontsDat)

        val computedLayouts = remember(elements, fontsDat) {
            elements.associate { el ->
                el.id to SampLayoutMath.computeLayout(el, fontsDat)
            }
        }
        val currentLayouts by rememberUpdatedState(computedLayouts)

        val fallbackTextPaint = remember {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                isSubpixelText = true
            }
        }

        Box(
            modifier = canvasSizingModifier
                .background(AppColors.CanvasFrameBg)
                .border(AppDimens.BorderThin, AppColors.BorderInteractive)
        ) {
            var activeDragMode by remember { mutableStateOf(DragMode.NONE) }
            var activeTargetId by remember { mutableStateOf<Long?>(null) }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { tapOffset ->
                            val scaleX = size.width / AppConstants.CANVAS_VIRTUAL_WIDTH
                            val scaleY = size.height / AppConstants.CANVAS_VIRTUAL_HEIGHT
                            val vx = tapOffset.x / scaleX
                            val vy = tapOffset.y / scaleY
                            val hit = currentElements
                                .asReversed()
                                .firstOrNull { el ->
                                    if (!el.isVisible) return@firstOrNull false
                                    val rect = currentLayouts[el.id]?.selectionRect ?: return@firstOrNull false
                                    vx >= rect.left - 8f && vx <= rect.right + 8f &&
                                        vy >= rect.top - 8f && vy <= rect.bottom + 8f
                                }
                            onSelectElement(hit?.id)
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                val scaleX = size.width / AppConstants.CANVAS_VIRTUAL_WIDTH
                                val scaleY = size.height / AppConstants.CANVAS_VIRTUAL_HEIGHT
                                val vx = startOffset.x / scaleX
                                val vy = startOffset.y / scaleY

                                val selId = currentSelectedId
                                val selEl = currentElements.firstOrNull { it.id == selId && it.isVisible && !it.isLocked }
                                val selRect = selEl?.let { currentLayouts[it.id]?.selectionRect }

                                if (selEl != null && selRect != null) {
                                    val handleHitRadius = 18f
                                    if (kotlin.math.abs(vx - selRect.right) <= handleHitRadius &&
                                        kotlin.math.abs(vy - selRect.bottom) <= handleHitRadius
                                    ) {
                                        activeDragMode = DragMode.RESIZE
                                        activeTargetId = selEl.id
                                        onBeginGesture()
                                        return@detectDragGestures
                                    }
                                }

                                val hit = currentElements
                                    .asReversed()
                                    .firstOrNull { el ->
                                        if (!el.isVisible || el.isLocked) return@firstOrNull false
                                        val rect = currentLayouts[el.id]?.selectionRect ?: return@firstOrNull false
                                        vx >= rect.left - 6f && vx <= rect.right + 6f &&
                                            vy >= rect.top - 6f && vy <= rect.bottom + 6f
                                    }

                                if (hit != null) {
                                    onSelectElement(hit.id)
                                    activeDragMode = DragMode.MOVE
                                    activeTargetId = hit.id
                                    onBeginGesture()
                                } else {
                                    activeDragMode = DragMode.NONE
                                    activeTargetId = null
                                }
                            },
                            onDrag = { change, dragAmount ->
                                val targetId = activeTargetId ?: return@detectDragGestures
                                if (activeDragMode == DragMode.NONE) return@detectDragGestures
                                change.consume()
                                val scaleX = size.width / AppConstants.CANVAS_VIRTUAL_WIDTH
                                val scaleY = size.height / AppConstants.CANVAS_VIRTUAL_HEIGHT
                                val dxV = dragAmount.x / scaleX
                                val dyV = dragAmount.y / scaleY
                                when (activeDragMode) {
                                    DragMode.MOVE -> onDragElement(targetId, dxV, dyV)
                                    DragMode.RESIZE -> onResizeElement(targetId, dxV, dyV)
                                    DragMode.NONE -> Unit
                                }
                            },
                            onDragEnd = {
                                if (activeDragMode != DragMode.NONE) {
                                    onEndGesture()
                                }
                                activeDragMode = DragMode.NONE
                                activeTargetId = null
                            },
                            onDragCancel = {
                                if (activeDragMode != DragMode.NONE) {
                                    onEndGesture()
                                }
                                activeDragMode = DragMode.NONE
                                activeTargetId = null
                            }
                        )
                    }
            ) {
                val scaleX = size.width / AppConstants.CANVAS_VIRTUAL_WIDTH
                val scaleY = size.height / AppConstants.CANVAS_VIRTUAL_HEIGHT

                if (showGrid) {
                    drawVirtualGrid(scaleX, scaleY)
                }

                for (el in elements) {
                    if (!el.isVisible) continue
                    val layout = computedLayouts[el.id] ?: continue
                    drawTextDrawElement(
                        element = el,
                        layout = layout,
                        scaleX = scaleX,
                        scaleY = scaleY,
                        loadedSprites = loadedSprites,
                        fontAtlas1 = fontAtlas1,
                        fontAtlas2 = fontAtlas2,
                        fontsDat = currentFontsDat,
                        fallbackTextPaint = fallbackTextPaint,
                        spriteMissingLabel = spriteLabels[el.id] ?: el.text,
                        modelLabel = modelLabels[el.id] ?: "#${el.previewModel}"
                    )
                }

                val selected = elements.firstOrNull { it.id == selectedElementId }
                val selectedLayout = selected?.let { computedLayouts[it.id] }
                if (selected != null && selectedLayout != null && selected.isVisible) {
                    drawSelectionOverlay(
                        rect = selectedLayout.selectionRect,
                        anchorX = selected.x,
                        anchorY = selected.y,
                        scaleX = scaleX,
                        scaleY = scaleY,
                        isLocked = selected.isLocked
                    )
                }
            }

            if (elements.isEmpty()) {
                Text(
                    text = stringResource(R.string.canvas_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(AppDimens.Space24)
                        .background(AppColors.SurfaceModal.copy(alpha = 0.85f), RoundedCornerShape(AppDimens.RadiusMedium))
                        .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusMedium))
                        .padding(horizontal = AppDimens.Space16, vertical = AppDimens.Space12)
                )
            }
        }
    }
}

private fun DrawScope.drawVirtualGrid(scaleX: Float, scaleY: Float) {
    val step = AppConstants.GRID_STEP_VIRTUAL * 2f
    var x = step
    while (x < AppConstants.CANVAS_VIRTUAL_WIDTH) {
        val px = x * scaleX
        drawLine(
            color = AppColors.GridLine,
            start = Offset(px, 0f),
            end = Offset(px, size.height),
            strokeWidth = 1f
        )
        x += step
    }
    var y = step
    while (y < AppConstants.CANVAS_VIRTUAL_HEIGHT) {
        val py = y * scaleY
        drawLine(
            color = AppColors.GridLine,
            start = Offset(0f, py),
            end = Offset(size.width, py),
            strokeWidth = 1f
        )
        y += step
    }
    // Center crosshairs (320, 240)
    val cx = 320f * scaleX
    val cy = 240f * scaleY
    val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
    drawLine(AppColors.CenterGuide, Offset(cx, 0f), Offset(cx, size.height), strokeWidth = 1.2f, pathEffect = dash)
    drawLine(AppColors.CenterGuide, Offset(0f, cy), Offset(size.width, cy), strokeWidth = 1.2f, pathEffect = dash)
}

private fun DrawScope.drawTextDrawElement(
    element: TextDrawElement,
    layout: ComputedTextDrawLayout,
    scaleX: Float,
    scaleY: Float,
    loadedSprites: Map<String, ImageBitmap>,
    fontAtlas1: ImageBitmap?,
    fontAtlas2: ImageBitmap?,
    fontsDat: FontsDatData?,
    fallbackTextPaint: Paint,
    spriteMissingLabel: String,
    modelLabel: String
) {
    // 1. Box Background
    layout.boxRect?.let { box ->
        val boxColor = SampColorUtils.rgbaLongToComposeColor(element.boxColorRgba)
        drawRect(
            color = boxColor,
            topLeft = Offset(box.left * scaleX, box.top * scaleY),
            size = Size(box.width * scaleX, box.height * scaleY)
        )
    }

    // 2. Sprite (Font 4)
    if (element.font == TdFont.SPRITE_4) {
        val key = element.text.trim().lowercase()
        val spriteBmp = loadedSprites[key]
        val leftPx = element.x * scaleX
        val topPx = element.y * scaleY
        val widthPx = (element.textSizeX.coerceAtLeast(2f) * scaleX).roundToInt().coerceAtLeast(1)
        val heightPx = (element.textSizeY.coerceAtLeast(2f) * scaleY).roundToInt().coerceAtLeast(1)
        val tintColor = SampColorUtils.rgbaLongToComposeColor(element.colorRgba)

        if (spriteBmp != null) {
            drawImage(
                image = spriteBmp,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(spriteBmp.width, spriteBmp.height),
                dstOffset = IntOffset(leftPx.roundToInt(), topPx.roundToInt()),
                dstSize = IntSize(widthPx, heightPx),
                colorFilter = ColorFilter.tint(tintColor, BlendMode.Modulate)
            )
        } else {
            drawRect(
                color = tintColor.copy(alpha = 0.16f),
                topLeft = Offset(leftPx, topPx),
                size = Size(widthPx.toFloat(), heightPx.toFloat())
            )
            drawRect(
                color = AppColors.BorderInteractive,
                topLeft = Offset(leftPx, topPx),
                size = Size(widthPx.toFloat(), heightPx.toFloat()),
                style = Stroke(width = 1.5f)
            )
            drawIntoCanvas { canvas ->
                fallbackTextPaint.color = AppColors.TextSecondary.toArgb()
                fallbackTextPaint.textSize = (11f * scaleY).coerceIn(14f, 28f)
                fallbackTextPaint.textAlign = Paint.Align.LEFT
                fallbackTextPaint.style = Paint.Style.FILL
                fallbackTextPaint.typeface = Typeface.DEFAULT
                canvas.nativeCanvas.drawText(
                    element.text.ifBlank { spriteMissingLabel },
                    leftPx + 6f,
                    topPx + fallbackTextPaint.textSize + 4f,
                    fallbackTextPaint
                )
            }
        }
        return
    }

    // 3. Model Preview (Font 5)
    if (element.font == TdFont.MODEL_PREVIEW_5) {
        val leftPx = element.x * scaleX
        val topPx = element.y * scaleY
        val wPx = element.textSizeX.coerceAtLeast(2f) * scaleX
        val hPx = element.textSizeY.coerceAtLeast(2f) * scaleY
        val bgColor = SampColorUtils.rgbaLongToComposeColor(element.backgroundColorRgba)
        drawRect(color = bgColor, topLeft = Offset(leftPx, topPx), size = Size(wPx, hPx))
        drawRect(
            color = AppColors.BorderInteractive,
            topLeft = Offset(leftPx, topPx),
            size = Size(wPx, hPx),
            style = Stroke(width = 1.5f)
        )
        drawLine(AppColors.BorderSubtle, Offset(leftPx, topPx), Offset(leftPx + wPx, topPx + hPx), strokeWidth = 1f)
        drawLine(AppColors.BorderSubtle, Offset(leftPx + wPx, topPx), Offset(leftPx, topPx + hPx), strokeWidth = 1f)
        drawIntoCanvas { canvas ->
            fallbackTextPaint.color = AppColors.TextPrimary.toArgb()
            fallbackTextPaint.textSize = (12f * scaleY).coerceIn(14f, 30f)
            fallbackTextPaint.textAlign = Paint.Align.CENTER
            fallbackTextPaint.style = Paint.Style.FILL
            fallbackTextPaint.typeface = Typeface.MONOSPACE
            canvas.nativeCanvas.drawText(modelLabel, leftPx + wPx / 2f, topPx + hPx / 2f + fallbackTextPaint.textSize / 3f, fallbackTextPaint)
        }
        return
    }

    // 4. Text Fonts 0..3
    // Sprite[0] = font2 (FONT_0 Gothic/Diploma & FONT_2 Slim/Menu)
    // Sprite[1] = font1 (FONT_1 Normal/Subtitles & FONT_3 Pricedown)
    val selectedAtlas = if (SampFontAtlasLayout.usesFont1Sheet(element.font.value)) {
        fontAtlas1 ?: fontAtlas2
    } else {
        fontAtlas2 ?: fontAtlas1
    }

    if (selectedAtlas != null) {
        drawBitmapAtlasText(
            element = element,
            layout = layout,
            atlas = selectedAtlas,
            fontsDat = fontsDat,
            scaleX = scaleX,
            scaleY = scaleY
        )
    } else {
        drawSystemFallbackText(
            element = element,
            layout = layout,
            scaleX = scaleX,
            scaleY = scaleY,
            paint = fallbackTextPaint
        )
    }
}

private fun DrawScope.drawBitmapAtlasText(
    element: TextDrawElement,
    layout: ComputedTextDrawLayout,
    atlas: ImageBitmap,
    fontsDat: FontsDatData?,
    scaleX: Float,
    scaleY: Float
) {
    if (atlas.width < 16 || atlas.height < 16) return

    val fontTable = fontsDat?.tableForSampFont(element.font.value)
    val charHeightPx = layout.lineHeightVirtual * scaleY * 1.25f
    val glyphWidthScale = element.letterSizeX.coerceAtLeast(0.05f) * scaleX
    val bgColor = SampColorUtils.rgbaLongToComposeColor(element.backgroundColorRgba)
    val shadowShift = element.shadow.coerceAtLeast(0)
    val outlineWidth = element.outline.coerceAtLeast(0)

    layout.wrappedLines.forEachIndexed { lineIdx, line ->
        val lineTopPx = (element.y + lineIdx * layout.lineHeightVirtual) * scaleY
        var cursorX = when (element.alignment) {
            TdAlignment.LEFT -> element.x
            TdAlignment.CENTER -> element.x - (line.widthVirtual / 2f)
            TdAlignment.RIGHT -> element.x - line.widthVirtual
        }

        for (run in line.runs) {
            for (ch in run.text) {
                val advanceVirtual = (fontTable?.charAdvance(ch, element.font.value, element.proportional)?.toFloat()
                    ?: 15f) * element.letterSizeX.coerceAtLeast(0.05f)

                if (ch != ' ') {
                    val cellIdx = SampFontAtlasLayout.resolveAtlasIndex(ch, element.font.value)
                    val slice = SampFontAtlasLayout.computeGlyphSlice(atlas.width, atlas.height, cellIdx)
                    if (slice != null) {
                        val dstX = cursorX * scaleX
                        val glyphDrawW = (32.0f * glyphWidthScale).coerceAtLeast(2f)
                        val dstSize = IntSize(
                            glyphDrawW.roundToInt().coerceAtLeast(1),
                            charHeightPx.roundToInt().coerceAtLeast(1)
                        )
                        val srcOffset = IntOffset(slice.srcX, slice.srcY)
                        val srcSize = IntSize(slice.width, slice.height)

                        // Shadow
                        if (shadowShift > 0) {
                            val sx = dstX + shadowShift * scaleX
                            val sy = lineTopPx + shadowShift * scaleY
                            drawImage(
                                image = atlas,
                                srcOffset = srcOffset,
                                srcSize = srcSize,
                                dstOffset = IntOffset(sx.roundToInt(), sy.roundToInt()),
                                dstSize = dstSize,
                                colorFilter = ColorFilter.tint(bgColor, BlendMode.Modulate)
                            )
                        }

                        // Outline
                        if (outlineWidth > 0) {
                            val ox = outlineWidth * scaleX * 0.8f
                            val oy = outlineWidth * scaleY * 0.8f
                            val offsets = arrayOf(
                                Offset(-ox, 0f), Offset(ox, 0f),
                                Offset(0f, -oy), Offset(0f, oy),
                                Offset(-ox, -oy), Offset(ox, -oy),
                                Offset(-ox, oy), Offset(ox, oy)
                            )
                            for (off in offsets) {
                                drawImage(
                                    image = atlas,
                                    srcOffset = srcOffset,
                                    srcSize = srcSize,
                                    dstOffset = IntOffset((dstX + off.x).roundToInt(), (lineTopPx + off.y).roundToInt()),
                                    dstSize = dstSize,
                                    colorFilter = ColorFilter.tint(bgColor, BlendMode.Modulate)
                                )
                            }
                        }

                        // Foreground glyph
                        drawImage(
                            image = atlas,
                            srcOffset = srcOffset,
                            srcSize = srcSize,
                            dstOffset = IntOffset(dstX.roundToInt(), lineTopPx.roundToInt()),
                            dstSize = dstSize,
                            colorFilter = ColorFilter.tint(run.color, BlendMode.Modulate)
                        )
                    }
                }
                cursorX += advanceVirtual
            }
        }
    }
}

private fun DrawScope.drawSystemFallbackText(
    element: TextDrawElement,
    layout: ComputedTextDrawLayout,
    scaleX: Float,
    scaleY: Float,
    paint: Paint
) {
    val bgArgb = SampColorUtils.rgbaLongToArgbInt(element.backgroundColorRgba)
    val fontSizePx = (layout.lineHeightVirtual * scaleY * 0.88f).coerceIn(8f, 240f)
    paint.textSize = fontSizePx
    paint.typeface = when (element.font) {
        TdFont.FONT_0 -> Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        TdFont.FONT_2 -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        TdFont.FONT_3 -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        else -> Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    paint.textAlign = Paint.Align.LEFT

    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        layout.wrappedLines.forEachIndexed { idx, line ->
            val baselineY = ((element.y + (idx + 0.82f) * layout.lineHeightVirtual) * scaleY)
            var cursorXVirtual = when (element.alignment) {
                TdAlignment.LEFT -> element.x
                TdAlignment.CENTER -> element.x - (line.widthVirtual / 2f)
                TdAlignment.RIGHT -> element.x - line.widthVirtual
            }

            for (run in line.runs) {
                val drawX = cursorXVirtual * scaleX
                if (element.shadow > 0) {
                    paint.style = Paint.Style.FILL
                    paint.color = bgArgb
                    native.drawText(run.text, drawX + element.shadow * scaleX, baselineY + element.shadow * scaleY, paint)
                }
                if (element.outline > 0) {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = (element.outline * scaleX * 1.4f).coerceAtLeast(1f)
                    paint.color = bgArgb
                    native.drawText(run.text, drawX, baselineY, paint)
                }
                paint.style = Paint.Style.FILL
                paint.color = run.color.toArgb()
                native.drawText(run.text, drawX, baselineY, paint)

                cursorXVirtual += SampLayoutMath.measureTextWidthVirtual(run.text, element, null)
            }
        }
    }
}

private fun DrawScope.drawSelectionOverlay(
    rect: VirtualRect,
    anchorX: Float,
    anchorY: Float,
    scaleX: Float,
    scaleY: Float,
    isLocked: Boolean
) {
    val left = rect.left * scaleX
    val top = rect.top * scaleY
    val width = rect.width * scaleX
    val height = rect.height * scaleY
    val outlineColor = if (isLocked) AppColors.StatusWarning else AppColors.SelectionOutline

    drawRect(
        color = outlineColor,
        topLeft = Offset(left, top),
        size = Size(width, height),
        style = Stroke(width = 2.5f)
    )

    // Anchor marker
    drawCircle(
        color = outlineColor,
        radius = 5f,
        center = Offset(anchorX * scaleX, anchorY * scaleY)
    )

    // Bottom-right resize handle
    if (!isLocked) {
        val handleR = AppDimens.CanvasHandleRadius.toPx()
        val handleCenter = Offset(left + width, top + height)
        drawCircle(color = AppColors.SelectionHandleFill, radius = handleR, center = handleCenter)
        drawCircle(color = outlineColor, radius = handleR, center = handleCenter, style = Stroke(width = 2.5f))
    }
}
