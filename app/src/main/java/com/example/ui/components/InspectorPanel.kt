package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.TdAlignment
import com.example.domain.model.TdFont
import com.example.domain.model.TextDrawElement
import com.example.domain.samp.SampColorUtils
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppDimens

enum class ColorTargetField {
    TEXT_COLOR,
    BOX_COLOR,
    BACKGROUND_COLOR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectorPanel(
    element: TextDrawElement?,
    onUpdate: (recordHistory: Boolean, (TextDrawElement) -> TextDrawElement) -> Unit,
    onOpenColorPicker: (ColorTargetField) -> Unit,
    onOpenSpritePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(AppColors.SurfaceBase)
            .border(AppDimens.BorderThin, AppColors.BorderSubtle)
            .padding(AppDimens.Space12)
    ) {
        Text(
            text = stringResource(R.string.panel_inspector_title),
            style = MaterialTheme.typography.titleSmall,
            color = AppColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(AppDimens.Space8))

        if (element == null) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.inspector_no_selection),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    modifier = Modifier.padding(AppDimens.Space8)
                )
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Space12)
        ) {
            // Variable name & Global/Player toggle
            StudioCard {
                OutlinedTextField(
                    value = element.varName,
                    onValueChange = { next ->
                        onUpdate(true) { it.copy(varName = next) }
                    },
                    label = { Text(stringResource(R.string.field_var_name), style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = inspectorTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("inspector_var_name_input")
                )
                Spacer(modifier = Modifier.height(AppDimens.Space8))
                Text(
                    text = stringResource(R.string.field_type_scope),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(AppDimens.Space4))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.Space6)
                ) {
                    SegmentedChoiceChip(
                        label = stringResource(R.string.scope_global),
                        selected = !element.isPlayerTextDraw,
                        onClick = { onUpdate(true) { it.copy(isPlayerTextDraw = false) } },
                        modifier = Modifier.weight(1f)
                    )
                    SegmentedChoiceChip(
                        label = stringResource(R.string.scope_player),
                        selected = element.isPlayerTextDraw,
                        onClick = { onUpdate(true) { it.copy(isPlayerTextDraw = true) } },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Text / Sprite input + color tags
            if (element.font != TdFont.MODEL_PREVIEW_5) {
                StudioCard {
                    OutlinedTextField(
                        value = element.text,
                        onValueChange = { next ->
                            onUpdate(true) { it.copy(text = next) }
                        },
                        label = { Text(stringResource(R.string.field_text_content), style = MaterialTheme.typography.labelSmall) },
                        maxLines = 3,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = inspectorTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("inspector_text_input")
                    )

                    if (element.font == TdFont.SPRITE_4) {
                        Spacer(modifier = Modifier.height(AppDimens.Space8))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = AppDimens.MinTouchTarget)
                                .clip(RoundedCornerShape(AppDimens.RadiusSmall))
                                .background(AppColors.SurfaceInteractive)
                                .border(AppDimens.BorderThin, AppColors.BorderInteractive, RoundedCornerShape(AppDimens.RadiusSmall))
                                .clickable(onClick = onOpenSpritePicker)
                                .testTag("open_sprite_picker_button")
                                .padding(horizontal = AppDimens.Space12),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Image,
                                contentDescription = null,
                                tint = AppColors.AccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(AppDimens.Space8))
                            Text(
                                text = stringResource(R.string.action_pick_sprite),
                                style = MaterialTheme.typography.labelMedium,
                                color = AppColors.TextPrimary
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(AppDimens.Space8))
                        Text(
                            text = stringResource(R.string.section_color_tags),
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(AppDimens.Space4))
                        val tags = listOf("~r~", "~g~", "~b~", "~y~", "~p~", "~l~", "~w~", "~h~", "~n~")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(AppDimens.Space4)
                        ) {
                            tags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(AppDimens.RadiusSmall))
                                        .background(AppColors.SurfaceInteractive)
                                        .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusSmall))
                                        .clickable { onUpdate(true) { it.copy(text = it.text + tag) } }
                                        .padding(horizontal = AppDimens.Space8, vertical = AppDimens.Space6)
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppColors.TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Font & Alignment
            StudioCard {
                var fontMenuExpanded by remember { mutableStateOf(false) }
                val fontOptions = listOf(
                    TdFont.FONT_0 to stringResource(R.string.font_0_diploma),
                    TdFont.FONT_1 to stringResource(R.string.font_1_futura),
                    TdFont.FONT_2 to stringResource(R.string.font_2_pricedown),
                    TdFont.FONT_3 to stringResource(R.string.font_3_bankgothic),
                    TdFont.SPRITE_4 to stringResource(R.string.font_4_sprite),
                    TdFont.MODEL_PREVIEW_5 to stringResource(R.string.font_5_model)
                )
                val currentFontLabel = fontOptions.firstOrNull { it.first == element.font }?.second ?: ""

                ExposedDropdownMenuBox(
                    expanded = fontMenuExpanded,
                    onExpandedChange = { fontMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = currentFontLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.field_font), style = MaterialTheme.typography.labelSmall) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fontMenuExpanded) },
                        colors = inspectorTextFieldColors(),
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = fontMenuExpanded,
                        onDismissRequest = { fontMenuExpanded = false },
                        modifier = Modifier.background(AppColors.SurfaceModal)
                    ) {
                        fontOptions.forEach { (fontEnum, label) ->
                            DropdownMenuItem(
                                text = { Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.TextPrimary) },
                                onClick = {
                                    fontMenuExpanded = false
                                    onUpdate(true) { it.copy(font = fontEnum) }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimens.Space8))
                Text(
                    text = stringResource(R.string.field_alignment),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(AppDimens.Space4))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.Space4)
                ) {
                    SegmentedChoiceChip(
                        label = stringResource(R.string.align_left),
                        selected = element.alignment == TdAlignment.LEFT,
                        onClick = { onUpdate(true) { it.copy(alignment = TdAlignment.LEFT) } },
                        modifier = Modifier.weight(1f)
                    )
                    SegmentedChoiceChip(
                        label = stringResource(R.string.align_center),
                        selected = element.alignment == TdAlignment.CENTER,
                        onClick = { onUpdate(true) { it.copy(alignment = TdAlignment.CENTER) } },
                        modifier = Modifier.weight(1f)
                    )
                    SegmentedChoiceChip(
                        label = stringResource(R.string.align_right),
                        selected = element.alignment == TdAlignment.RIGHT,
                        onClick = { onUpdate(true) { it.copy(alignment = TdAlignment.RIGHT) } },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Position & Dimensions
            StudioCard {
                Text(
                    text = stringResource(R.string.section_position),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(AppDimens.Space6))
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Space8)) {
                    FloatStepperField(
                        label = stringResource(R.string.label_pos_x),
                        value = element.x,
                        step = 1f,
                        onValueChange = { nx -> onUpdate(true) { it.copy(x = nx) } },
                        modifier = Modifier.weight(1f)
                    )
                    FloatStepperField(
                        label = stringResource(R.string.label_pos_y),
                        value = element.y,
                        step = 1f,
                        onValueChange = { ny -> onUpdate(true) { it.copy(y = ny) } },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (element.font != TdFont.SPRITE_4 && element.font != TdFont.MODEL_PREVIEW_5) {
                    Spacer(modifier = Modifier.height(AppDimens.Space12))
                    Text(
                        text = stringResource(R.string.section_letter_size),
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(AppDimens.Space6))
                    Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Space8)) {
                        FloatStepperField(
                            label = stringResource(R.string.label_letter_x),
                            value = element.letterSizeX,
                            step = 0.02f,
                            decimals = 3,
                            onValueChange = { lx -> onUpdate(true) { it.copy(letterSizeX = lx.coerceAtLeast(0f)) } },
                            modifier = Modifier.weight(1f)
                        )
                        FloatStepperField(
                            label = stringResource(R.string.label_letter_y),
                            value = element.letterSizeY,
                            step = 0.1f,
                            decimals = 3,
                            onValueChange = { ly -> onUpdate(true) { it.copy(letterSizeY = ly.coerceAtLeast(0f)) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppDimens.Space12))
                Text(
                    text = stringResource(R.string.section_text_size),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(AppDimens.Space6))
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Space8)) {
                    FloatStepperField(
                        label = stringResource(R.string.label_text_x),
                        value = element.textSizeX,
                        step = 2f,
                        onValueChange = { tx -> onUpdate(true) { it.copy(textSizeX = tx) } },
                        modifier = Modifier.weight(1f)
                    )
                    FloatStepperField(
                        label = stringResource(R.string.label_text_y),
                        value = element.textSizeY,
                        step = 2f,
                        onValueChange = { ty -> onUpdate(true) { it.copy(textSizeY = ty) } },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (element.alignment == TdAlignment.CENTER && element.font != TdFont.SPRITE_4 && element.font != TdFont.MODEL_PREVIEW_5) {
                    Spacer(modifier = Modifier.height(AppDimens.Space6))
                    Text(
                        text = stringResource(R.string.hint_center_textsize),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextSecondary
                    )
                }
            }

            // Colors
            StudioCard {
                Text(
                    text = stringResource(R.string.section_colors),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(AppDimens.Space6))
                ColorSwatchRow(
                    label = stringResource(R.string.color_text),
                    rgba = element.colorRgba,
                    onClick = { onOpenColorPicker(ColorTargetField.TEXT_COLOR) },
                    testTag = "pick_text_color"
                )
                Spacer(modifier = Modifier.height(AppDimens.Space6))
                ColorSwatchRow(
                    label = stringResource(R.string.color_box),
                    rgba = element.boxColorRgba,
                    onClick = { onOpenColorPicker(ColorTargetField.BOX_COLOR) },
                    testTag = "pick_box_color"
                )
                Spacer(modifier = Modifier.height(AppDimens.Space6))
                ColorSwatchRow(
                    label = stringResource(R.string.color_background),
                    rgba = element.backgroundColorRgba,
                    onClick = { onOpenColorPicker(ColorTargetField.BACKGROUND_COLOR) },
                    testTag = "pick_bg_color"
                )
            }

            // Style & Interaction
            StudioCard {
                Text(
                    text = stringResource(R.string.section_style),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(AppDimens.Space4))
                StudioSwitchRow(
                    label = stringResource(R.string.toggle_use_box),
                    checked = element.useBox,
                    onCheckedChange = { v -> onUpdate(true) { it.copy(useBox = v) } },
                    testTag = "switch_use_box"
                )
                StudioSwitchRow(
                    label = stringResource(R.string.toggle_proportional),
                    checked = element.proportional,
                    onCheckedChange = { v -> onUpdate(true) { it.copy(proportional = v) } },
                    testTag = "switch_proportional"
                )
                StudioSwitchRow(
                    label = stringResource(R.string.toggle_selectable),
                    checked = element.selectable,
                    onCheckedChange = { v -> onUpdate(true) { it.copy(selectable = v) } },
                    testTag = "switch_selectable"
                )
                Spacer(modifier = Modifier.height(AppDimens.Space6))
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Space8)) {
                    IntStepperField(
                        label = stringResource(R.string.label_outline),
                        value = element.outline,
                        minValue = 0,
                        maxValue = 5,
                        onValueChange = { o -> onUpdate(true) { it.copy(outline = o) } },
                        modifier = Modifier.weight(1f)
                    )
                    IntStepperField(
                        label = stringResource(R.string.label_shadow),
                        value = element.shadow,
                        minValue = 0,
                        maxValue = 5,
                        onValueChange = { s -> onUpdate(true) { it.copy(shadow = s) } },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Font 5 Model Preview Parameters
            if (element.font == TdFont.MODEL_PREVIEW_5) {
                StudioCard {
                    Text(
                        text = stringResource(R.string.section_model_preview),
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(AppDimens.Space6))
                    Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Space8)) {
                        IntStepperField(
                            label = stringResource(R.string.label_model_id),
                            value = element.previewModel,
                            minValue = 0,
                            maxValue = 20000,
                            onValueChange = { m -> onUpdate(true) { it.copy(previewModel = m) } },
                            modifier = Modifier.weight(1f)
                        )
                        FloatStepperField(
                            label = stringResource(R.string.label_zoom),
                            value = element.previewZoom,
                            step = 0.1f,
                            decimals = 2,
                            onValueChange = { z -> onUpdate(true) { it.copy(previewZoom = z.coerceAtLeast(0.1f)) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(AppDimens.Space8))
                    Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Space6)) {
                        FloatStepperField(
                            label = stringResource(R.string.label_rot_x),
                            value = element.previewRotX,
                            step = 5f,
                            decimals = 1,
                            onValueChange = { rx -> onUpdate(true) { it.copy(previewRotX = rx) } },
                            modifier = Modifier.weight(1f)
                        )
                        FloatStepperField(
                            label = stringResource(R.string.label_rot_y),
                            value = element.previewRotY,
                            step = 5f,
                            decimals = 1,
                            onValueChange = { ry -> onUpdate(true) { it.copy(previewRotY = ry) } },
                            modifier = Modifier.weight(1f)
                        )
                        FloatStepperField(
                            label = stringResource(R.string.label_rot_z),
                            value = element.previewRotZ,
                            step = 5f,
                            decimals = 1,
                            onValueChange = { rz -> onUpdate(true) { it.copy(previewRotZ = rz) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(AppDimens.Space8))
                    Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Space8)) {
                        IntStepperField(
                            label = stringResource(R.string.label_veh_col1),
                            value = element.previewVehCol1,
                            minValue = -1,
                            maxValue = 255,
                            onValueChange = { c1 -> onUpdate(true) { it.copy(previewVehCol1 = c1) } },
                            modifier = Modifier.weight(1f)
                        )
                        IntStepperField(
                            label = stringResource(R.string.label_veh_col2),
                            value = element.previewVehCol2,
                            minValue = -1,
                            maxValue = 255,
                            onValueChange = { c2 -> onUpdate(true) { it.copy(previewVehCol2 = c2) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 38.dp)
            .clip(RoundedCornerShape(AppDimens.RadiusSmall))
            .background(if (selected) AppColors.AccentBlueSoft else AppColors.SurfaceInteractive)
            .border(
                AppDimens.BorderThin,
                if (selected) AppColors.AccentBlue else AppColors.BorderSubtle,
                RoundedCornerShape(AppDimens.RadiusSmall)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = AppDimens.Space6, vertical = AppDimens.Space4),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) AppColors.TextPrimary else AppColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ColorSwatchRow(
    label: String,
    rgba: Long,
    onClick: () -> Unit,
    testTag: String
) {
    val color = SampColorUtils.rgbaLongToComposeColor(rgba)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AppDimens.MinTouchTarget)
            .clip(RoundedCornerShape(AppDimens.RadiusSmall))
            .background(AppColors.SurfaceInteractive)
            .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusSmall))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = AppDimens.Space8, vertical = AppDimens.Space6),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = AppColors.TextPrimary)
            Text(
                text = SampColorUtils.formatRgbaHex(rgba),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextSecondary
            )
        }
        Box(
            modifier = Modifier
                .size(AppDimens.ColorSwatchSize)
                .clip(CircleShape)
                .background(color)
                .border(AppDimens.BorderThin, AppColors.BorderInteractive, CircleShape)
        )
    }
}

@Composable
fun inspectorTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AppColors.TextPrimary,
    unfocusedTextColor = AppColors.TextPrimary,
    focusedBorderColor = AppColors.AccentBlue,
    unfocusedBorderColor = AppColors.BorderSubtle,
    focusedLabelColor = AppColors.AccentBlue,
    unfocusedLabelColor = AppColors.TextSecondary,
    cursorColor = AppColors.AccentBlue
)
