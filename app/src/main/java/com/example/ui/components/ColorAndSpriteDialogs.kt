package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AssetPackStatus
import com.example.domain.model.SpriteIndexEntry
import com.example.domain.samp.SampColorUtils
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppDimens

@Composable
fun RgbaColorPickerDialog(
    targetLabel: String,
    initialRgba: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var r by remember(initialRgba) { mutableIntStateOf(((initialRgba ushr 24) and 0xFF).toInt()) }
    var g by remember(initialRgba) { mutableIntStateOf(((initialRgba ushr 16) and 0xFF).toInt()) }
    var b by remember(initialRgba) { mutableIntStateOf(((initialRgba ushr 8) and 0xFF).toInt()) }
    var a by remember(initialRgba) { mutableIntStateOf((initialRgba and 0xFF).toInt()) }

    val currentRgba = ((r.toLong() shl 24) or (g.toLong() shl 16) or (b.toLong() shl 8) or a.toLong()) and 0xFFFFFFFFL
    var hexInput by remember(initialRgba) { mutableStateOf(SampColorUtils.formatRgbaHex(initialRgba)) }
    var hexError by remember { mutableStateOf(false) }

    val quickPalette = SampColorUtils.quickPresetRgbaList

    StudioDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.color_picker_title, targetLabel),
        maxWidth = 480.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Space12)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(AppDimens.RadiusSmall))
                    .background(SampColorUtils.rgbaLongToComposeColor(currentRgba))
                    .border(AppDimens.BorderThin, AppColors.BorderInteractive, RoundedCornerShape(AppDimens.RadiusSmall))
            )
            OutlinedTextField(
                value = hexInput,
                onValueChange = { raw ->
                    hexInput = raw
                    val parsed = SampColorUtils.parseHex8Strict(raw)
                    if (parsed != null) {
                        hexError = false
                        r = ((parsed ushr 24) and 0xFF).toInt()
                        g = ((parsed ushr 16) and 0xFF).toInt()
                        b = ((parsed ushr 8) and 0xFF).toInt()
                        a = (parsed and 0xFF).toInt()
                    } else {
                        hexError = true
                    }
                },
                label = { Text(stringResource(R.string.label_hex_rgba), style = MaterialTheme.typography.labelSmall) },
                isError = hexError,
                supportingText = if (hexError) {
                    { Text(stringResource(R.string.error_invalid_hex), style = MaterialTheme.typography.labelSmall) }
                } else null,
                singleLine = true,
                colors = inspectorTextFieldColors(),
                modifier = Modifier.weight(1f).testTag("color_hex_input")
            )
        }

        Spacer(modifier = Modifier.height(AppDimens.Space8))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Space8)
        ) {
            quickPalette.forEach { swatchRgba ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(SampColorUtils.rgbaLongToComposeColor(swatchRgba))
                        .border(AppDimens.BorderThin, AppColors.BorderInteractive, CircleShape)
                        .clickable {
                            r = ((swatchRgba ushr 24) and 0xFF).toInt()
                            g = ((swatchRgba ushr 16) and 0xFF).toInt()
                            b = ((swatchRgba ushr 8) and 0xFF).toInt()
                            a = (swatchRgba and 0xFF).toInt()
                            hexInput = SampColorUtils.formatRgbaHex(swatchRgba)
                            hexError = false
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.Space8))
        ChannelSliderRow(stringResource(R.string.label_channel_r), r, AppColors.StatusError) { v ->
            r = v
            hexInput = SampColorUtils.formatRgbaHex(((r.toLong() shl 24) or (g.toLong() shl 16) or (b.toLong() shl 8) or a.toLong()) and 0xFFFFFFFFL)
            hexError = false
        }
        ChannelSliderRow(stringResource(R.string.label_channel_g), g, AppColors.StatusSuccess) { v ->
            g = v
            hexInput = SampColorUtils.formatRgbaHex(((r.toLong() shl 24) or (g.toLong() shl 16) or (b.toLong() shl 8) or a.toLong()) and 0xFFFFFFFFL)
            hexError = false
        }
        ChannelSliderRow(stringResource(R.string.label_channel_b), b, AppColors.AccentBlue) { v ->
            b = v
            hexInput = SampColorUtils.formatRgbaHex(((r.toLong() shl 24) or (g.toLong() shl 16) or (b.toLong() shl 8) or a.toLong()) and 0xFFFFFFFFL)
            hexError = false
        }
        ChannelSliderRow(stringResource(R.string.label_channel_a), a, AppColors.TextSecondary) { v ->
            a = v
            hexInput = SampColorUtils.formatRgbaHex(((r.toLong() shl 24) or (g.toLong() shl 16) or (b.toLong() shl 8) or a.toLong()) and 0xFFFFFFFFL)
            hexError = false
        }

        Spacer(modifier = Modifier.height(AppDimens.Space12))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Space12)
        ) {
            StudioSecondaryButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            StudioPrimaryButton(
                text = stringResource(R.string.action_apply),
                onClick = { onConfirm(currentRgba) },
                modifier = Modifier.weight(1f).testTag("color_apply_button")
            )
        }
    }
}

@Composable
private fun ChannelSliderRow(
    label: String,
    value: Int,
    accentColor: Color,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextSecondary,
            modifier = Modifier.width(88.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt().coerceIn(0, 255)) },
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(
                thumbColor = AppColors.TextPrimary,
                activeTrackColor = accentColor,
                inactiveTrackColor = AppColors.SurfaceInteractive
            ),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextPrimary,
            modifier = Modifier.width(32.dp)
        )
    }
}

@Composable
fun SpritePickerDialog(
    spriteCatalog: List<SpriteIndexEntry>,
    loadedSprites: Map<String, ImageBitmap>,
    onRequestSpriteLoad: (String) -> Unit,
    onSelectSprite: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTxdFilter by remember { mutableStateOf<String?>(null) }

    val txdGroups = remember(spriteCatalog) {
        spriteCatalog.map { it.txdName }.distinct().sortedBy { it.lowercase() }
    }

    val filteredSprites = remember(spriteCatalog, searchQuery, selectedTxdFilter) {
        val q = searchQuery.trim().lowercase()
        spriteCatalog.filter { entry ->
            val matchesTxd = selectedTxdFilter == null || entry.txdName.equals(selectedTxdFilter, ignoreCase = true)
            val matchesQuery = q.isEmpty() ||
                entry.spriteName.lowercase().contains(q) ||
                entry.txdName.lowercase().contains(q)
            matchesTxd && matchesQuery
        }
    }

    StudioDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.sprite_picker_title),
        maxWidth = 640.dp
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(R.string.sprite_search_hint), style = MaterialTheme.typography.bodySmall) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = AppColors.TextSecondary) },
            singleLine = true,
            colors = inspectorTextFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag("sprite_search_input")
        )

        Spacer(modifier = Modifier.height(AppDimens.Space8))

        if (txdGroups.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Space6)
            ) {
                FilterPill(
                    label = stringResource(R.string.sprite_filter_all),
                    selected = selectedTxdFilter == null,
                    onClick = { selectedTxdFilter = null }
                )
                txdGroups.forEach { txd ->
                    FilterPill(
                        label = txd,
                        selected = selectedTxdFilter == txd,
                        onClick = { selectedTxdFilter = txd }
                    )
                }
            }
            Spacer(modifier = Modifier.height(AppDimens.Space8))
        }

        if (spriteCatalog.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.sprite_picker_empty_assets),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
        } else if (filteredSprites.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.sprite_picker_no_match),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 112.dp),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Space8),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Space8),
                modifier = Modifier.fillMaxWidth().height(165.dp)
            ) {
                items(filteredSprites, key = { it.cacheKey }) { entry ->
                    LaunchedEffect(entry.cacheKey) {
                        onRequestSpriteLoad(entry.sampNotation)
                    }
                    val bmp = loadedSprites[entry.cacheKey]
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(AppDimens.RadiusSmall))
                            .background(AppColors.SurfaceCard)
                            .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusSmall))
                            .clickable { onSelectSprite(entry.sampNotation) }
                            .padding(AppDimens.Space6),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(AppDimens.SpriteGridCellSize)
                                .clip(RoundedCornerShape(AppDimens.RadiusSmall))
                                .background(AppColors.CanvasBackdrop),
                            contentAlignment = Alignment.Center
                        ) {
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp,
                                    contentDescription = entry.sampNotation,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(64.dp)
                                )
                            } else {
                                Text(
                                    text = "${entry.width}x${entry.height}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppColors.TextTertiary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(AppDimens.Space4))
                        Text(
                            text = entry.spriteName,
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = entry.txdName,
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(AppDimens.RadiusSmall))
            .background(if (selected) AppColors.AccentBlueSoft else AppColors.SurfaceInteractive)
            .border(
                AppDimens.BorderThin,
                if (selected) AppColors.AccentBlue else AppColors.BorderSubtle,
                RoundedCornerShape(AppDimens.RadiusSmall)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = AppDimens.Space12, vertical = AppDimens.Space6)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) AppColors.TextPrimary else AppColors.TextSecondary,
            maxLines = 1
        )
    }
}

@Composable
fun AssetStatusDialog(
    status: AssetPackStatus,
    onPickAssetsZip: () -> Unit,
    onDismiss: () -> Unit
) {
    val presentLabel = stringResource(R.string.status_present)
    val missingLabel = stringResource(R.string.status_missing)

    StudioDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.assets_info_title),
        maxWidth = 460.dp
    ) {
        when (status) {
            is AssetPackStatus.Loaded -> {
                StudioCard {
                    Text(
                        text = stringResource(R.string.assets_info_source, status.sourceLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(AppDimens.Space6))
                    Text(
                        text = stringResource(R.string.assets_info_fonts_dat, if (status.hasFontsDat) presentLabel else missingLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (status.hasFontsDat) AppColors.StatusSuccess else AppColors.StatusWarning
                    )
                    Text(
                        text = stringResource(R.string.assets_info_fonts_txd, if (status.hasFontsTxd) presentLabel else missingLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (status.hasFontsTxd) AppColors.StatusSuccess else AppColors.StatusWarning
                    )
                    Text(
                        text = stringResource(R.string.assets_info_txd_count, status.txdCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                    Text(
                        text = stringResource(R.string.assets_info_sprite_count, status.spriteCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
            }
            is AssetPackStatus.Error -> {
                Text(
                    text = stringResource(R.string.assets_error_prefix, status.message),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.StatusError
                )
            }
            is AssetPackStatus.Loading -> {
                Text(
                    text = stringResource(R.string.assets_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
            AssetPackStatus.NotLoaded -> {
                Text(
                    text = stringResource(R.string.assets_banner_prompt),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.Space16))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Space12)
        ) {
            StudioSecondaryButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            StudioPrimaryButton(
                text = if (status is AssetPackStatus.Loaded) {
                    stringResource(R.string.action_reload_assets)
                } else {
                    stringResource(R.string.action_import_assets)
                },
                onClick = {
                    onDismiss()
                    onPickAssetsZip()
                },
                modifier = Modifier.weight(1f).testTag("dialog_import_assets_button")
            )
        }
    }
}
