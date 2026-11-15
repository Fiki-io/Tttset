package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppDimens
import com.example.ui.theme.AppMotion
import com.example.ui.theme.rememberReducedMotionEnabled
import java.util.Locale

@Composable
fun StudioCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(AppDimens.RadiusMedium))
            .background(AppColors.SurfaceCard.copy(alpha = 0.72f))
            .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusMedium))
            .padding(AppDimens.Space12),
        content = content
    )
}

@Composable
fun StudioDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    maxWidth: androidx.compose.ui.unit.Dp = AppDimens.MaxDialogWidth,
    content: @Composable ColumnScope.() -> Unit
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val visibleState = remember {
        MutableTransitionState(reducedMotion).apply { targetState = true }
    }
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.ScrimDark)
                .padding(AppDimens.Space12),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visibleState = visibleState,
                enter = fadeIn(AppMotion.standardTween(reducedMotion)) + scaleIn(
                    initialScale = if (reducedMotion) 1f else 0.94f,
                    animationSpec = AppMotion.standardTween(reducedMotion)
                ),
                exit = fadeOut(AppMotion.fastTween(reducedMotion)) + scaleOut(
                    targetScale = if (reducedMotion) 1f else 0.94f,
                    animationSpec = AppMotion.fastTween(reducedMotion)
                )
            ) {
                Surface(
                    shape = RoundedCornerShape(AppDimens.RadiusDialog),
                    color = AppColors.SurfaceModal,
                    border = BorderStroke(AppDimens.BorderThin, AppColors.BorderInteractive),
                    modifier = modifier
                        .widthIn(max = maxWidth)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.background(
                            Brush.radialGradient(
                                colors = listOf(AppColors.GlowNeutral, Color.Transparent),
                                radius = 480f
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppDimens.Space16)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AppColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(horizontal = AppDimens.MinTouchTarget)
                                )
                                IconButton(
                                    onClick = onDismissRequest,
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .minimumInteractiveComponentSize()
                                        .testTag("dialog_close_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.cd_close_dialog),
                                        tint = AppColors.TextSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(AppDimens.Space8))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                content()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudioPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = rememberReducedMotionEnabled()
    val scale = if (pressed && !reducedMotion) 0.97f else 1f
    val haptic = LocalHapticFeedback.current

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(AppDimens.RadiusMedium),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.AccentBlue,
            contentColor = AppColors.OnAccent,
            disabledContainerColor = AppColors.SurfaceInteractive,
            disabledContentColor = AppColors.TextTertiary
        ),
        contentPadding = PaddingValues(horizontal = AppDimens.Space16, vertical = AppDimens.Space12),
        modifier = modifier
            .heightIn(min = AppDimens.MinTouchTarget)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun StudioSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(AppDimens.RadiusMedium),
        border = BorderStroke(AppDimens.BorderThin, AppColors.BorderInteractive),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AppColors.SurfaceInteractive,
            contentColor = AppColors.TextPrimary
        ),
        contentPadding = PaddingValues(horizontal = AppDimens.Space16, vertical = AppDimens.Space12),
        modifier = modifier.heightIn(min = AppDimens.MinTouchTarget)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun StudioSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AppDimens.MinTouchTarget)
            .clip(RoundedCornerShape(AppDimens.RadiusSmall))
            .clickable(role = Role.Switch) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!checked)
            }
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = AppDimens.Space8, vertical = AppDimens.Space4)
            .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = AppDimens.Space8)
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AppColors.OnAccent,
                checkedTrackColor = AppColors.AccentBlue,
                uncheckedThumbColor = AppColors.TextPrimary,
                uncheckedTrackColor = AppColors.SurfaceInteractive,
                uncheckedBorderColor = AppColors.BorderInteractive
            )
        )
    }
}

@Composable
fun FloatStepperField(
    label: String,
    value: Float,
    step: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    decimals: Int = 2
) {
    var textValue by remember(value) {
        mutableStateOf(String.format(Locale.US, "%.${decimals}f", value))
    }
    LaunchedEffect(value) {
        val parsed = textValue.toFloatOrNull()
        if (parsed == null || kotlin.math.abs(parsed - value) > 0.0005f) {
            textValue = String.format(Locale.US, "%.${decimals}f", value)
        }
    }
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(AppDimens.Space4))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .clip(RoundedCornerShape(AppDimens.RadiusSmall))
                .background(AppColors.SurfaceInteractive)
                .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusSmall)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onValueChange(value - step) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = stringResource(R.string.cd_stepper_decrease),
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
            BasicTextField(
                value = textValue,
                onValueChange = { raw ->
                    textValue = raw
                    raw.toFloatOrNull()?.let(onValueChange)
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextPrimary),
                cursorBrush = SolidColor(AppColors.AccentBlue),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.weight(1f).padding(horizontal = AppDimens.Space4)
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onValueChange(value + step) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.cd_stepper_increase),
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun IntStepperField(
    label: String,
    value: Int,
    minValue: Int,
    maxValue: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }
    LaunchedEffect(value) {
        if (textValue.toIntOrNull() != value) {
            textValue = value.toString()
        }
    }
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(AppDimens.Space4))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .clip(RoundedCornerShape(AppDimens.RadiusSmall))
                .background(AppColors.SurfaceInteractive)
                .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusSmall)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onValueChange((value - 1).coerceIn(minValue, maxValue)) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = stringResource(R.string.cd_stepper_decrease),
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
            BasicTextField(
                value = textValue,
                onValueChange = { raw ->
                    textValue = raw
                    raw.toIntOrNull()?.let { parsed ->
                        onValueChange(parsed.coerceIn(minValue, maxValue))
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(color = AppColors.TextPrimary),
                cursorBrush = SolidColor(AppColors.AccentBlue),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.weight(1f).padding(horizontal = AppDimens.Space4)
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onValueChange((value + 1).coerceIn(minValue, maxValue)) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.cd_stepper_increase),
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
