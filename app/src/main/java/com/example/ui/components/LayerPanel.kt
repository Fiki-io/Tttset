package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.TdFont
import com.example.domain.model.TextDrawElement
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppDimens
import com.example.ui.viewmodel.NewElementPreset

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LayerPanel(
    elements: List<TextDrawElement>,
    selectedElementId: Long?,
    onSelectElement: (Long) -> Unit,
    onAddPreset: (NewElementPreset) -> Unit,
    onToggleVisibility: (Long) -> Unit,
    onToggleLock: (Long) -> Unit,
    onMoveLayer: (Long, Int) -> Unit,
    onDuplicate: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(AppDimens.RadiusMedium)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .background(AppColors.SurfaceBase.copy(alpha = 0.94f))
            .border(AppDimens.BorderThin, AppColors.BorderInteractive, shape)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        event.changes.forEach { if (!it.isConsumed) it.consume() }
                    }
                }
            }
            .padding(AppDimens.Space8)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = AppDimens.Space4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.panel_layers_title),
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("close_layers_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cd_close_panel),
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(AppDimens.Space4))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Space4),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Space4),
            maxItemsInEachRow = 2
        ) {
            QuickAddButton(
                label = stringResource(R.string.add_text),
                icon = Icons.Outlined.TextFields,
                onClick = { onAddPreset(NewElementPreset.TEXT) },
                testTag = "add_text_button",
                modifier = Modifier.weight(1f)
            )
            QuickAddButton(
                label = stringResource(R.string.add_box),
                icon = Icons.Outlined.CropSquare,
                onClick = { onAddPreset(NewElementPreset.BOX) },
                testTag = "add_box_button",
                modifier = Modifier.weight(1f)
            )
            QuickAddButton(
                label = stringResource(R.string.add_sprite),
                icon = Icons.Outlined.Image,
                onClick = { onAddPreset(NewElementPreset.SPRITE) },
                testTag = "add_sprite_button",
                modifier = Modifier.weight(1f)
            )
            QuickAddButton(
                label = stringResource(R.string.add_model),
                icon = Icons.Outlined.ViewInAr,
                onClick = { onAddPreset(NewElementPreset.MODEL) },
                testTag = "add_model_button",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(AppDimens.Space8))
        HorizontalDivider(color = AppColors.BorderSubtle)
        Spacer(modifier = Modifier.height(AppDimens.Space6))

        if (elements.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.layers_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    modifier = Modifier.padding(AppDimens.Space8)
                )
            }
        } else {
            val reversedList = elements.asReversed()
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Space4)
            ) {
                itemsIndexed(reversedList, key = { _, item -> item.id }) { revIndex, item ->
                    val isSelected = item.id == selectedElementId
                    val actualIndex = elements.lastIndex - revIndex
                    LayerRowItem(
                        element = item,
                        isSelected = isSelected,
                        canMoveUp = actualIndex < elements.lastIndex,
                        canMoveDown = actualIndex > 0,
                        onSelect = { onSelectElement(item.id) },
                        onToggleVisibility = { onToggleVisibility(item.id) },
                        onToggleLock = { onToggleLock(item.id) },
                        onMoveUp = { onMoveLayer(item.id, 1) },
                        onMoveDown = { onMoveLayer(item.id, -1) },
                        onDuplicate = { onDuplicate(item.id) },
                        onDelete = { onDelete(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAddButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .heightIn(min = 38.dp)
            .clip(RoundedCornerShape(AppDimens.RadiusSmall))
            .background(AppColors.SurfaceInteractive)
            .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusSmall))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = AppDimens.Space6, vertical = AppDimens.Space4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = AppColors.AccentBlue,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.TextPrimary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(AppDimens.Space4))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LayerRowItem(
    element: TextDrawElement,
    isSelected: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onSelect: () -> Unit,
    onToggleVisibility: () -> Unit,
    onToggleLock: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val bgColor = if (isSelected) AppColors.SurfaceSelected else AppColors.SurfaceCard.copy(alpha = 0.65f)
    val borderColor = if (isSelected) AppColors.BorderActive else AppColors.BorderSubtle

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.RadiusSmall))
            .background(bgColor)
            .border(AppDimens.BorderThin, borderColor, RoundedCornerShape(AppDimens.RadiusSmall))
            .clickable(onClick = onSelect)
            .testTag("layer_item_${element.id}")
            .padding(horizontal = AppDimens.Space8, vertical = AppDimens.Space6)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (element.font) {
                    TdFont.SPRITE_4 -> Icons.Outlined.Image
                    TdFont.MODEL_PREVIEW_5 -> Icons.Outlined.ViewInAr
                    else -> if (element.useBox && element.text == "_") Icons.Outlined.CropSquare else Icons.Outlined.TextFields
                },
                contentDescription = null,
                tint = if (isSelected) AppColors.AccentBlue else AppColors.TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(AppDimens.Space6))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = element.varName,
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (element.isPlayerTextDraw) {
                        stringResource(R.string.layer_badge_player)
                    } else {
                        stringResource(R.string.layer_badge_global)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSecondary,
                    maxLines = 1
                )
            }
            IconButton(
                onClick = onToggleVisibility,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = if (element.isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = stringResource(R.string.cd_layer_visibility),
                    tint = if (element.isVisible) AppColors.TextSecondary else AppColors.TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = onToggleLock,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = if (element.isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = stringResource(R.string.cd_layer_lock),
                    tint = if (element.isLocked) AppColors.StatusWarning else AppColors.TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (isSelected) {
            Spacer(modifier = Modifier.height(AppDimens.Space4))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = stringResource(R.string.cd_layer_up),
                        tint = if (canMoveUp) AppColors.TextPrimary else AppColors.TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDownward,
                        contentDescription = stringResource(R.string.cd_layer_down),
                        tint = if (canMoveDown) AppColors.TextPrimary else AppColors.TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onDuplicate,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = stringResource(R.string.cd_layer_duplicate),
                        tint = AppColors.TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = stringResource(R.string.cd_layer_delete),
                        tint = AppColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
