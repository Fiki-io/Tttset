package com.example.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.core.AppConstants
import com.example.domain.model.AssetPackStatus
import com.example.domain.model.CanvasAspectRatio
import com.example.domain.model.TextDrawElement
import com.example.ui.canvas.SampCanvasViewport
import com.example.ui.components.AssetStatusDialog
import com.example.ui.components.ColorTargetField
import com.example.ui.components.ExportPawnDialog
import com.example.ui.components.ImportPawnDialog
import com.example.ui.components.InspectorPanel
import com.example.ui.components.LayerPanel
import com.example.ui.components.ProjectManagerDialog
import com.example.ui.components.RgbaColorPickerDialog
import com.example.ui.components.SpritePickerDialog
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppDimens
import com.example.ui.viewmodel.EditorViewModel
import com.example.ui.viewmodel.UiNotice
import kotlinx.coroutines.launch

@Composable
fun StudioScreen(
    viewModel: EditorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val projects by viewModel.projectSummaries.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showLayersPanel by remember { mutableStateOf(true) }
    var showInspectorPanel by remember { mutableStateOf(true) }
    var showProjectsDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showSpritePickerDialog by remember { mutableStateOf(false) }
    var showAssetStatusDialog by remember { mutableStateOf(false) }
    var activeColorField by remember { mutableStateOf<ColorTargetField?>(null) }

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importAssetsZip(uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.notices.collect { notice ->
            when (notice) {
                is UiNotice.Message -> snackbarHostState.showSnackbar(notice.text)
            }
        }
    }

    val copiedSuccessText = stringResource(R.string.export_copied_message)
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    Scaffold(
        containerColor = AppColors.BackgroundBottom,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AppColors.BackgroundTop, AppColors.BackgroundBottom)
                    )
                )
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopStudioBar(
                    projectName = uiState.currentProject?.name ?: stringResource(R.string.project_default_name),
                    aspectRatio = uiState.currentProject?.aspectRatio ?: CanvasAspectRatio.RATIO_16_9,
                    showGrid = uiState.showGrid,
                    snapToGrid = uiState.snapToGrid,
                    canUndo = uiState.canUndo,
                    canRedo = uiState.canRedo,
                    showLayers = showLayersPanel,
                    showInspector = showInspectorPanel,
                    assetStatus = uiState.assetStatus,
                    onOpenProjects = { showProjectsDialog = true },
                    onOpenAssetDialog = { showAssetStatusDialog = true },
                    onToggleAspectRatio = viewModel::toggleAspectRatio,
                    onToggleGrid = viewModel::toggleGrid,
                    onToggleSnap = viewModel::toggleSnap,
                    onUndo = viewModel::undo,
                    onRedo = viewModel::redo,
                    onToggleLayers = { showLayersPanel = !showLayersPanel },
                    onToggleInspector = { showInspectorPanel = !showInspectorPanel },
                    onOpenImport = { showImportDialog = true },
                    onOpenExport = { showExportDialog = true }
                )

                BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    val isNarrow = maxWidth < 760.dp
                    val layerWidth = if (isNarrow) AppDimens.CompactRailWidth else AppDimens.LayerRailWidth
                    val inspectorWidth = if (isNarrow) AppDimens.CompactInspectorWidth else AppDimens.InspectorWidth

                    Row(modifier = Modifier.fillMaxSize()) {
                        AnimatedVisibility(visible = showLayersPanel) {
                            LayerPanel(
                                elements = uiState.currentProject?.elements.orEmpty(),
                                selectedElementId = uiState.selectedElementId,
                                onSelectElement = viewModel::selectElement,
                                onAddPreset = viewModel::addElement,
                                onToggleVisibility = viewModel::toggleVisibility,
                                onToggleLock = viewModel::toggleLock,
                                onMoveLayer = viewModel::moveLayer,
                                onDuplicate = viewModel::duplicateElement,
                                onDelete = viewModel::deleteElement,
                                modifier = Modifier.width(layerWidth)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            SampCanvasViewport(
                                elements = uiState.currentProject?.elements.orEmpty(),
                                selectedElementId = uiState.selectedElementId,
                                aspectRatio = uiState.currentProject?.aspectRatio ?: CanvasAspectRatio.RATIO_16_9,
                                showGrid = uiState.showGrid,
                                loadedSprites = uiState.loadedSprites,
                                fontAtlas1 = uiState.fontAtlas1,
                                fontAtlas2 = uiState.fontAtlas2,
                                fontsDat = uiState.fontsDat,
                                onSelectElement = viewModel::selectElement,
                                onBeginGesture = viewModel::beginCanvasGesture,
                                onDragElement = viewModel::dragElementBy,
                                onResizeElement = viewModel::resizeElementBy,
                                onEndGesture = viewModel::endCanvasGesture,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (uiState.assetStatus is AssetPackStatus.NotLoaded) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(AppDimens.Space8)
                                        .clip(RoundedCornerShape(AppDimens.RadiusMedium))
                                        .background(AppColors.SurfaceModal.copy(alpha = 0.92f))
                                        .border(AppDimens.BorderThin, AppColors.BorderInteractive, RoundedCornerShape(AppDimens.RadiusMedium))
                                        .clickable {
                                            zipPickerLauncher.launch(
                                                arrayOf(AppConstants.ZIP_MIME_TYPE, AppConstants.OCTET_STREAM_MIME_TYPE)
                                            )
                                        }
                                        .testTag("banner_import_assets")
                                        .padding(horizontal = AppDimens.Space12, vertical = AppDimens.Space6),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(AppDimens.Space8)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Archive,
                                        contentDescription = null,
                                        tint = AppColors.AccentBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.assets_banner_prompt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Text(
                                        text = stringResource(R.string.action_import_assets),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppColors.AccentBlue
                                    )
                                }
                            }

                            uiState.selectedElement?.let { selected ->
                                CanvasBottomNudgeOverlay(
                                    element = selected,
                                    nudgeStep = uiState.nudgeStep,
                                    onCycleStep = viewModel::cycleNudgeStep,
                                    onNudge = viewModel::nudgeSelected,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(AppDimens.Space8)
                                )
                            }
                        }

                        AnimatedVisibility(visible = showInspectorPanel) {
                            InspectorPanel(
                                element = uiState.selectedElement,
                                onUpdate = { recordHistory, transform ->
                                    viewModel.updateSelectedElement(recordHistory, transform)
                                },
                                onOpenColorPicker = { field -> activeColorField = field },
                                onOpenSpritePicker = { showSpritePickerDialog = true },
                                modifier = Modifier.width(inspectorWidth)
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showProjectsDialog) {
        ProjectManagerDialog(
            projects = projects,
            activeProjectId = uiState.currentProject?.id,
            onOpenProject = viewModel::openProject,
            onCreateProject = viewModel::createNewProject,
            onRenameProject = viewModel::renameProject,
            onDeleteProject = viewModel::deleteProject,
            onDismiss = { showProjectsDialog = false }
        )
    }

    if (showImportDialog) {
        ImportPawnDialog(
            onImport = { code, replace -> viewModel.importPawnCode(code, replace) },
            onDismiss = { showImportDialog = false }
        )
    }

    if (showExportDialog) {
        val currentProject = uiState.currentProject
        if (currentProject != null) {
            ExportPawnDialog(
                syntaxMode = currentProject.syntaxMode,
                onChangeSyntaxMode = viewModel::setSyntaxMode,
                generateCode = viewModel::generateExportCode,
                onCopyCode = { code ->
                    copyToClipboard(context, code)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(copiedSuccessText)
                    }
                },
                onDismiss = { showExportDialog = false }
            )
        }
    }

    if (showSpritePickerDialog) {
        SpritePickerDialog(
            spriteCatalog = uiState.spriteCatalog,
            loadedSprites = uiState.loadedSprites,
            onRequestSpriteLoad = viewModel::requestSpriteLoad,
            onSelectSprite = { notation ->
                viewModel.updateSelectedElement(true) { it.copy(text = notation) }
                showSpritePickerDialog = false
            },
            onDismiss = { showSpritePickerDialog = false }
        )
    }

    if (showAssetStatusDialog) {
        AssetStatusDialog(
            status = uiState.assetStatus,
            onPickAssetsZip = {
                zipPickerLauncher.launch(
                    arrayOf(AppConstants.ZIP_MIME_TYPE, AppConstants.OCTET_STREAM_MIME_TYPE)
                )
            },
            onDismiss = { showAssetStatusDialog = false }
        )
    }

    val colorField = activeColorField
    val selectedForColor = uiState.selectedElement
    if (colorField != null && selectedForColor != null) {
        val (targetLabel, initialRgba) = when (colorField) {
            ColorTargetField.TEXT_COLOR -> stringResource(R.string.color_text) to selectedForColor.colorRgba
            ColorTargetField.BOX_COLOR -> stringResource(R.string.color_box) to selectedForColor.boxColorRgba
            ColorTargetField.BACKGROUND_COLOR -> stringResource(R.string.color_background) to selectedForColor.backgroundColorRgba
        }
        RgbaColorPickerDialog(
            targetLabel = targetLabel,
            initialRgba = initialRgba,
            onDismiss = { activeColorField = null },
            onConfirm = { newRgba ->
                viewModel.updateSelectedElement(true) { el ->
                    when (colorField) {
                        ColorTargetField.TEXT_COLOR -> el.copy(colorRgba = newRgba)
                        ColorTargetField.BOX_COLOR -> el.copy(boxColorRgba = newRgba)
                        ColorTargetField.BACKGROUND_COLOR -> el.copy(backgroundColorRgba = newRgba)
                    }
                }
                activeColorField = null
            }
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("PawnTextDraw", text))
}

@Composable
private fun TopStudioBar(
    projectName: String,
    aspectRatio: CanvasAspectRatio,
    showGrid: Boolean,
    snapToGrid: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    showLayers: Boolean,
    showInspector: Boolean,
    assetStatus: AssetPackStatus,
    onOpenProjects: () -> Unit,
    onOpenAssetDialog: () -> Unit,
    onToggleAspectRatio: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleSnap: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleLayers: () -> Unit,
    onToggleInspector: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenExport: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AppDimens.TopBarHeight)
            .background(AppColors.SurfaceBase)
            .border(AppDimens.BorderThin, AppColors.BorderSubtle)
            .padding(horizontal = AppDimens.Space8, vertical = AppDimens.Space4)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Space6)
    ) {
        // Project Pill
        Row(
            modifier = Modifier
                .heightIn(min = 40.dp)
                .clip(RoundedCornerShape(AppDimens.RadiusSmall))
                .background(AppColors.SurfaceInteractive)
                .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusSmall))
                .clickable(onClick = onOpenProjects)
                .testTag("top_projects_button")
                .padding(horizontal = AppDimens.Space12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Space6)
        ) {
            Icon(
                imageVector = Icons.Filled.FolderOpen,
                contentDescription = stringResource(R.string.cd_project_menu),
                tint = AppColors.AccentBlue,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = projectName,
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Asset Status Pill
        Row(
            modifier = Modifier
                .heightIn(min = 40.dp)
                .clip(RoundedCornerShape(AppDimens.RadiusSmall))
                .background(
                    if (assetStatus is AssetPackStatus.Loaded) AppColors.SurfaceCard else AppColors.AccentBlueSoft
                )
                .border(
                    AppDimens.BorderThin,
                    if (assetStatus is AssetPackStatus.Loaded) AppColors.BorderSubtle else AppColors.AccentBlue,
                    RoundedCornerShape(AppDimens.RadiusSmall)
                )
                .clickable(onClick = onOpenAssetDialog)
                .testTag("top_assets_button")
                .padding(horizontal = AppDimens.Space12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Space6)
        ) {
            if (assetStatus is AssetPackStatus.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = AppColors.AccentBlue
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Archive,
                    contentDescription = null,
                    tint = if (assetStatus is AssetPackStatus.Loaded) AppColors.StatusSuccess else AppColors.AccentBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
            val statusLabel = when (assetStatus) {
                is AssetPackStatus.Loaded -> if (assetStatus.hasFontsDat && assetStatus.hasFontsTxd) {
                    stringResource(R.string.assets_loaded_with_fonts, assetStatus.txdCount, assetStatus.spriteCount)
                } else {
                    stringResource(R.string.assets_loaded_summary, assetStatus.txdCount, assetStatus.spriteCount)
                }
                is AssetPackStatus.Loading -> stringResource(R.string.assets_loading)
                is AssetPackStatus.Error -> stringResource(R.string.assets_not_loaded)
                AssetPackStatus.NotLoaded -> stringResource(R.string.action_import_assets)
            }
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextPrimary,
                maxLines = 1
            )
        }

        // Aspect Ratio Toggle
        TopChipButton(
            label = if (aspectRatio == CanvasAspectRatio.RATIO_16_9) {
                stringResource(R.string.ratio_16_9)
            } else {
                stringResource(R.string.ratio_4_3)
            },
            selected = true,
            onClick = onToggleAspectRatio,
            testTag = "top_ratio_button"
        )

        TopIconChip(
            icon = Icons.Filled.GridOn,
            contentDescription = stringResource(R.string.cd_toggle_grid),
            selected = showGrid,
            onClick = onToggleGrid,
            testTag = "top_grid_button"
        )

        TopIconChip(
            icon = Icons.Filled.Straighten,
            contentDescription = stringResource(R.string.cd_toggle_snap),
            selected = snapToGrid,
            onClick = onToggleSnap,
            testTag = "top_snap_button"
        )

        TopIconChip(
            icon = Icons.AutoMirrored.Filled.Undo,
            contentDescription = stringResource(R.string.cd_undo),
            selected = false,
            enabled = canUndo,
            onClick = onUndo,
            testTag = "top_undo_button"
        )

        TopIconChip(
            icon = Icons.AutoMirrored.Filled.Redo,
            contentDescription = stringResource(R.string.cd_redo),
            selected = false,
            enabled = canRedo,
            onClick = onRedo,
            testTag = "top_redo_button"
        )

        TopIconChip(
            icon = Icons.Filled.Layers,
            contentDescription = stringResource(R.string.cd_toggle_layers),
            selected = showLayers,
            onClick = onToggleLayers,
            testTag = "top_layers_toggle"
        )

        TopIconChip(
            icon = Icons.Filled.Tune,
            contentDescription = stringResource(R.string.cd_toggle_inspector),
            selected = showInspector,
            onClick = onToggleInspector,
            testTag = "top_inspector_toggle"
        )

        // Import Pawn
        Row(
            modifier = Modifier
                .heightIn(min = 40.dp)
                .clip(RoundedCornerShape(AppDimens.RadiusSmall))
                .background(AppColors.SurfaceInteractive)
                .border(AppDimens.BorderThin, AppColors.BorderInteractive, RoundedCornerShape(AppDimens.RadiusSmall))
                .clickable(onClick = onOpenImport)
                .testTag("top_import_pawn_button")
                .padding(horizontal = AppDimens.Space12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Space4)
        ) {
            Icon(Icons.Filled.FileDownload, contentDescription = null, tint = AppColors.TextPrimary, modifier = Modifier.size(16.dp))
            Text(stringResource(R.string.action_import_pawn), style = MaterialTheme.typography.labelMedium, color = AppColors.TextPrimary)
        }

        // Export Pawn
        Row(
            modifier = Modifier
                .heightIn(min = 40.dp)
                .clip(RoundedCornerShape(AppDimens.RadiusSmall))
                .background(AppColors.AccentBlue)
                .clickable(onClick = onOpenExport)
                .testTag("top_export_pawn_button")
                .padding(horizontal = AppDimens.Space12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Space4)
        ) {
            Icon(Icons.Filled.Code, contentDescription = null, tint = AppColors.OnAccent, modifier = Modifier.size(16.dp))
            Text(stringResource(R.string.action_export_pawn), style = MaterialTheme.typography.labelMedium, color = AppColors.OnAccent)
        }
    }
}

@Composable
private fun TopChipButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(AppDimens.RadiusSmall))
            .background(if (selected) AppColors.SurfaceInteractive else AppColors.SurfaceCard)
            .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusSmall))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = AppDimens.Space12),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = AppColors.TextPrimary)
    }
}

@Composable
private fun TopIconChip(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    testTag: String
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(AppDimens.RadiusSmall))
            .background(if (selected) AppColors.AccentBlueSoft else AppColors.SurfaceInteractive)
            .border(
                AppDimens.BorderThin,
                if (selected) AppColors.AccentBlue else AppColors.BorderSubtle,
                RoundedCornerShape(AppDimens.RadiusSmall)
            )
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = when {
                !enabled -> AppColors.TextTertiary
                selected -> AppColors.TextPrimary
                else -> AppColors.TextSecondary
            },
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun CanvasBottomNudgeOverlay(
    element: TextDrawElement,
    nudgeStep: Float,
    onCycleStep: () -> Unit,
    onNudge: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(AppDimens.RadiusMedium))
            .background(AppColors.SurfaceModal.copy(alpha = 0.90f))
            .border(AppDimens.BorderThin, AppColors.BorderInteractive, RoundedCornerShape(AppDimens.RadiusMedium))
            .padding(horizontal = AppDimens.Space12, vertical = AppDimens.Space4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Space6)
    ) {
        Text(
            text = stringResource(
                R.string.canvas_coord_readout,
                element.x,
                element.y,
                element.textSizeX,
                element.textSizeY
            ),
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextSecondary,
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(AppDimens.RadiusSmall))
                .background(AppColors.SurfaceInteractive)
                .border(AppDimens.BorderThin, AppColors.BorderSubtle, RoundedCornerShape(AppDimens.RadiusSmall))
                .clickable(onClick = onCycleStep)
                .padding(horizontal = AppDimens.Space8, vertical = AppDimens.Space4)
        ) {
            Text(
                text = stringResource(R.string.nudge_step_label, nudgeStep),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.AccentBlue
            )
        }
        NudgeIconButton(Icons.Filled.KeyboardArrowLeft, stringResource(R.string.cd_nudge_left)) { onNudge(-1, 0) }
        NudgeIconButton(Icons.Filled.KeyboardArrowUp, stringResource(R.string.cd_nudge_up)) { onNudge(0, -1) }
        NudgeIconButton(Icons.Filled.KeyboardArrowDown, stringResource(R.string.cd_nudge_down)) { onNudge(0, 1) }
        NudgeIconButton(Icons.Filled.KeyboardArrowRight, stringResource(R.string.cd_nudge_right)) { onNudge(1, 0) }
    }
}

@Composable
private fun NudgeIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(AppDimens.RadiusSmall))
            .background(AppColors.SurfaceInteractive)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = AppColors.TextPrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}
