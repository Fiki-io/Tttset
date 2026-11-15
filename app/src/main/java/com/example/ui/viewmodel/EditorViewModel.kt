package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.core.AppConstants
import com.example.data.assets.AssetPackRepository
import com.example.data.assets.LoadedFontAtlas
import com.example.data.local.AppDatabase
import com.example.data.local.ProjectRepository
import com.example.domain.assets.FontsDatData
import com.example.domain.model.AssetPackStatus
import com.example.domain.model.CanvasAspectRatio
import com.example.domain.model.PawnSyntaxMode
import com.example.domain.model.ProjectSummary
import com.example.domain.model.SpriteIndexEntry
import com.example.domain.model.TdAlignment
import com.example.domain.model.TdFont
import com.example.domain.model.TdProject
import com.example.domain.model.TextDrawElement
import com.example.domain.pawn.PawnExporter
import com.example.domain.pawn.PawnImporter
import com.example.domain.samp.SampLayoutMath
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NewElementPreset {
    TEXT,
    BOX,
    SPRITE,
    MODEL
}

data class EditorUiState(
    val currentProject: TdProject? = null,
    val selectedElementId: Long? = null,
    val showGrid: Boolean = true,
    val snapToGrid: Boolean = true,
    val nudgeStep: Float = 1f,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val assetStatus: AssetPackStatus = AssetPackStatus.NotLoaded,
    val spriteCatalog: List<SpriteIndexEntry> = emptyList(),
    val loadedSprites: Map<String, ImageBitmap> = emptyMap(),
    val fontAtlas1: ImageBitmap? = null,
    val fontAtlas2: ImageBitmap? = null,
    val fontsDat: FontsDatData? = null
) {
    val selectedElement: TextDrawElement?
        get() = currentProject?.elements?.firstOrNull { it.id == selectedElementId }
}

sealed interface UiNotice {
    data class Message(val text: String) : UiNotice
}

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val projectRepository = ProjectRepository(database.projectDao())
    private val assetRepository = AssetPackRepository(application)

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    val projectSummaries: StateFlow<List<ProjectSummary>> = projectRepository.projectSummaries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _notices = MutableSharedFlow<UiNotice>()
    val notices: SharedFlow<UiNotice> = _notices.asSharedFlow()

    private val undoStack = ArrayDeque<List<TextDrawElement>>()
    private val redoStack = ArrayDeque<List<TextDrawElement>>()
    private val maxHistorySize = 50
    private var dragSnapshotBeforeGesture: List<TextDrawElement>? = null

    init {
        initializeApp()
    }

    private fun initializeApp() {
        viewModelScope.launch {
            _uiState.update { it.copy(assetStatus = AssetPackStatus.Loading) }
            val status = assetRepository.initializeFromExistingOrBundled()
            applyLoadedAssetState(status)

            val existing = projectRepository.allProjects.first()
            val initialProject = if (existing.isNotEmpty()) {
                existing.first()
            } else {
                projectRepository.createProject(
                    name = getApplication<Application>().getString(R.string.project_default_name)
                )
            }
            setActiveProject(initialProject)
        }
    }

    private fun applyLoadedAssetState(status: AssetPackStatus) {
        val atlas: LoadedFontAtlas? = assetRepository.fontAtlas
        _uiState.update { state ->
            state.copy(
                assetStatus = status,
                spriteCatalog = assetRepository.spriteEntries,
                fontAtlas1 = atlas?.font1Bitmap?.asImageBitmap(),
                fontAtlas2 = atlas?.font2Bitmap?.asImageBitmap(),
                fontsDat = assetRepository.fontsDat
            )
        }
        preloadVisibleSprites()
    }

    fun importAssetsZip(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(assetStatus = AssetPackStatus.Loading) }
            val status = assetRepository.importZipFromUri(uri)
            applyLoadedAssetState(status)
            if (status is AssetPackStatus.Error) {
                _notices.emit(UiNotice.Message(getApplication<Application>().getString(R.string.assets_error_prefix, status.message)))
            }
        }
    }

    fun requestSpriteLoad(sampNotation: String) {
        val normalizedKey = sampNotation.trim().lowercase()
        if (normalizedKey.isEmpty() || !normalizedKey.contains(":")) return
        if (_uiState.value.loadedSprites.containsKey(normalizedKey)) return

        viewModelScope.launch {
            val bmp = assetRepository.loadSpriteBitmap(sampNotation) ?: return@launch
            _uiState.update { state ->
                val updatedMap = state.loadedSprites.toMutableMap()
                updatedMap[normalizedKey] = bmp.asImageBitmap()
                state.copy(loadedSprites = updatedMap)
            }
        }
    }

    private fun preloadVisibleSprites() {
        val elements = _uiState.value.currentProject?.elements ?: return
        elements.filter { it.font == TdFont.SPRITE_4 }.forEach { el ->
            requestSpriteLoad(el.text)
        }
    }

    private fun setActiveProject(project: TdProject) {
        undoStack.clear()
        redoStack.clear()
        _uiState.update {
            it.copy(
                currentProject = project,
                selectedElementId = project.elements.lastOrNull()?.id,
                canUndo = false,
                canRedo = false
            )
        }
        preloadVisibleSprites()
    }

    fun openProject(projectId: Long) {
        viewModelScope.launch {
            val loaded = projectRepository.loadProject(projectId) ?: return@launch
            setActiveProject(loaded)
        }
    }

    fun createNewProject(name: String) {
        val cleanName = name.trim().ifEmpty {
            getApplication<Application>().getString(R.string.project_default_name)
        }
        viewModelScope.launch {
            val created = projectRepository.createProject(cleanName)
            setActiveProject(created)
        }
    }

    fun renameProject(projectId: Long, newName: String) {
        val clean = newName.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            projectRepository.renameProject(projectId, clean)
            _uiState.update { state ->
                val proj = state.currentProject
                if (proj != null && proj.id == projectId) {
                    state.copy(currentProject = proj.copy(name = clean))
                } else {
                    state
                }
            }
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            val all = projectRepository.allProjects.first()
            if (all.size <= 1) {
                _notices.emit(UiNotice.Message(getApplication<Application>().getString(R.string.project_cannot_delete_last)))
                return@launch
            }
            projectRepository.deleteProject(projectId)
            if (_uiState.value.currentProject?.id == projectId) {
                val fallback = projectRepository.allProjects.first().firstOrNull()
                    ?: projectRepository.createProject(getApplication<Application>().getString(R.string.project_default_name))
                setActiveProject(fallback)
            }
        }
    }

    fun selectElement(elementId: Long?) {
        _uiState.update { it.copy(selectedElementId = elementId) }
    }

    fun toggleAspectRatio() {
        val project = _uiState.value.currentProject ?: return
        val next = if (project.aspectRatio == CanvasAspectRatio.RATIO_16_9) {
            CanvasAspectRatio.RATIO_4_3
        } else {
            CanvasAspectRatio.RATIO_16_9
        }
        val updated = project.copy(aspectRatio = next)
        _uiState.update { it.copy(currentProject = updated) }
        persistProject(updated)
    }

    fun setSyntaxMode(mode: PawnSyntaxMode) {
        val project = _uiState.value.currentProject ?: return
        val updated = project.copy(syntaxMode = mode)
        _uiState.update { it.copy(currentProject = updated) }
        persistProject(updated)
    }

    fun toggleGrid() {
        _uiState.update { it.copy(showGrid = !it.showGrid) }
    }

    fun toggleSnap() {
        _uiState.update { it.copy(snapToGrid = !it.snapToGrid) }
    }

    fun cycleNudgeStep() {
        _uiState.update { state ->
            val next = when (state.nudgeStep) {
                0.5f -> 1f
                1f -> 5f
                else -> 0.5f
            }
            state.copy(nudgeStep = next)
        }
    }

    fun addElement(preset: NewElementPreset) {
        val project = _uiState.value.currentProject ?: return
        val nextId = (project.elements.maxOfOrNull { it.id } ?: 0L) + 1L
        val count = project.elements.size
        val newElement = when (preset) {
            NewElementPreset.TEXT -> TextDrawElement(
                id = nextId,
                varName = "tdText$count",
                x = 240f,
                y = 200f,
                text = "TextDraw",
                font = TdFont.FONT_1,
                alignment = TdAlignment.LEFT,
                letterSizeX = 0.35f,
                letterSizeY = 1.4f,
                textSizeX = 480f,
                textSizeY = 20f,
                outline = 1,
                zOrder = count
            )
            NewElementPreset.BOX -> TextDrawElement(
                id = nextId,
                varName = "tdBox$count",
                x = 200f,
                y = 140f,
                text = "_",
                font = TdFont.FONT_1,
                alignment = TdAlignment.LEFT,
                letterSizeX = 0f,
                letterSizeY = 12f,
                textSizeX = 440f,
                textSizeY = 0f,
                colorRgba = 0xFFFFFFFFL,
                boxColorRgba = 0x00000099L,
                useBox = true,
                outline = 0,
                shadow = 0,
                zOrder = count
            )
            NewElementPreset.SPRITE -> {
                val firstSprite = _uiState.value.spriteCatalog.firstOrNull()?.sampNotation ?: "LD_SPAC:white"
                requestSpriteLoad(firstSprite)
                TextDrawElement(
                    id = nextId,
                    varName = "tdSprite$count",
                    x = 260f,
                    y = 180f,
                    text = firstSprite,
                    font = TdFont.SPRITE_4,
                    alignment = TdAlignment.LEFT,
                    letterSizeX = 0f,
                    letterSizeY = 0f,
                    textSizeX = 120f,
                    textSizeY = 120f,
                    colorRgba = AppConstants.DEFAULT_SPRITE_COLOR_RGBA,
                    outline = 0,
                    zOrder = count
                )
            }
            NewElementPreset.MODEL -> TextDrawElement(
                id = nextId,
                varName = "tdModel$count",
                x = 260f,
                y = 170f,
                text = "",
                font = TdFont.MODEL_PREVIEW_5,
                alignment = TdAlignment.LEFT,
                letterSizeX = 0f,
                letterSizeY = 0f,
                textSizeX = 120f,
                textSizeY = 120f,
                backgroundColorRgba = 0x00000055L,
                colorRgba = 0xFFFFFFFFL,
                outline = 0,
                previewModel = 411,
                zOrder = count
            )
        }
        commitElementListChange(project.elements + newElement, selectId = newElement.id)
    }

    fun duplicateElement(elementId: Long) {
        val project = _uiState.value.currentProject ?: return
        val source = project.elements.firstOrNull { it.id == elementId } ?: return
        val nextId = (project.elements.maxOfOrNull { it.id } ?: 0L) + 1L
        val copyName = "${source.varName}_copy"
        val offset = 12f
        val duplicated = source.copy(
            id = nextId,
            varName = copyName,
            x = (source.x + offset).coerceIn(0f, AppConstants.CANVAS_VIRTUAL_WIDTH),
            y = (source.y + offset).coerceIn(0f, AppConstants.CANVAS_VIRTUAL_HEIGHT),
            textSizeX = if (source.font != TdFont.SPRITE_4 && source.font != TdFont.MODEL_PREVIEW_5 && source.alignment != TdAlignment.CENTER) {
                source.textSizeX + offset
            } else {
                source.textSizeX
            },
            zOrder = project.elements.size
        )
        commitElementListChange(project.elements + duplicated, selectId = duplicated.id)
    }

    fun deleteElement(elementId: Long) {
        val project = _uiState.value.currentProject ?: return
        val remaining = project.elements.filterNot { it.id == elementId }
        val nextSelected = if (_uiState.value.selectedElementId == elementId) {
            remaining.lastOrNull()?.id
        } else {
            _uiState.value.selectedElementId
        }
        commitElementListChange(remaining, selectId = nextSelected)
    }

    fun moveLayer(elementId: Long, delta: Int) {
        val project = _uiState.value.currentProject ?: return
        val list = project.elements.toMutableList()
        val idx = list.indexOfFirst { it.id == elementId }
        if (idx < 0) return
        val target = (idx + delta).coerceIn(0, list.lastIndex)
        if (target == idx) return
        val item = list.removeAt(idx)
        list.add(target, item)
        commitElementListChange(list, selectId = elementId)
    }

    fun toggleVisibility(elementId: Long) {
        updateElement(elementId, recordHistory = false) { it.copy(isVisible = !it.isVisible) }
    }

    fun toggleLock(elementId: Long) {
        updateElement(elementId, recordHistory = false) { it.copy(isLocked = !it.isLocked) }
    }

    fun updateSelectedElement(recordHistory: Boolean = true, transform: (TextDrawElement) -> TextDrawElement) {
        val id = _uiState.value.selectedElementId ?: return
        updateElement(id, recordHistory, transform)
    }

    fun updateElement(
        elementId: Long,
        recordHistory: Boolean = true,
        transform: (TextDrawElement) -> TextDrawElement
    ) {
        val project = _uiState.value.currentProject ?: return
        var changed = false
        val updatedList = project.elements.map { el ->
            if (el.id == elementId) {
                val next = transform(el)
                if (next != el) changed = true
                if (next.font == TdFont.SPRITE_4 && next.text != el.text) {
                    requestSpriteLoad(next.text)
                }
                next
            } else {
                el
            }
        }
        if (!changed) return
        if (recordHistory) {
            commitElementListChange(updatedList, selectId = _uiState.value.selectedElementId)
        } else {
            val updatedProject = project.copy(elements = updatedList)
            _uiState.update { it.copy(currentProject = updatedProject) }
            persistProject(updatedProject)
        }
    }

    fun beginCanvasGesture() {
        val project = _uiState.value.currentProject ?: return
        if (dragSnapshotBeforeGesture == null) {
            dragSnapshotBeforeGesture = project.elements
        }
    }

    fun dragElementBy(elementId: Long, dxVirtual: Float, dyVirtual: Float) {
        val project = _uiState.value.currentProject ?: return
        val current = project.elements.firstOrNull { it.id == elementId } ?: return
        if (current.isLocked) return

        val rawX = (current.x + dxVirtual).coerceIn(-100f, AppConstants.CANVAS_VIRTUAL_WIDTH + 100f)
        val rawY = (current.y + dyVirtual).coerceIn(-100f, AppConstants.CANVAS_VIRTUAL_HEIGHT + 100f)
        val newX = SampLayoutMath.roundToTenth(rawX)
        val newY = SampLayoutMath.roundToTenth(rawY)
        val actualDx = newX - current.x

        val updatedElement = current.copy(
            x = newX,
            y = newY,
            textSizeX = if (current.font != TdFont.SPRITE_4 &&
                current.font != TdFont.MODEL_PREVIEW_5 &&
                current.alignment != TdAlignment.CENTER
            ) {
                SampLayoutMath.roundToTenth(current.textSizeX + actualDx)
            } else {
                current.textSizeX
            }
        )
        applyTransientElementUpdate(updatedElement)
    }

    fun resizeElementBy(elementId: Long, dxVirtual: Float, dyVirtual: Float) {
        val project = _uiState.value.currentProject ?: return
        val current = project.elements.firstOrNull { it.id == elementId } ?: return
        if (current.isLocked) return
        val resized = SampLayoutMath.applyResizeDelta(current, dxVirtual, dyVirtual)
        applyTransientElementUpdate(resized)
    }

    fun endCanvasGesture() {
        val snapshot = dragSnapshotBeforeGesture ?: return
        dragSnapshotBeforeGesture = null
        val project = _uiState.value.currentProject ?: return
        val snapEnabled = _uiState.value.snapToGrid
        val finalElements = if (snapEnabled) {
            project.elements.map { el ->
                if (el.id == _uiState.value.selectedElementId && !el.isLocked) {
                    val snappedX = SampLayoutMath.snapValue(el.x, true)
                    val snappedY = SampLayoutMath.snapValue(el.y, true)
                    val dx = snappedX - el.x
                    el.copy(
                        x = snappedX,
                        y = snappedY,
                        textSizeX = if (el.font != TdFont.SPRITE_4 &&
                            el.font != TdFont.MODEL_PREVIEW_5 &&
                            el.alignment != TdAlignment.CENTER
                        ) {
                            SampLayoutMath.roundToTenth(el.textSizeX + dx)
                        } else {
                            el.textSizeX
                        }
                    )
                } else {
                    el
                }
            }
        } else {
            project.elements
        }

        if (snapshot != finalElements) {
            pushUndoSnapshot(snapshot)
            val updatedProject = project.copy(elements = finalElements)
            _uiState.update {
                it.copy(
                    currentProject = updatedProject,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty()
                )
            }
            persistProject(updatedProject)
        }
    }

    fun nudgeSelected(dxDirection: Int, dyDirection: Int) {
        val id = _uiState.value.selectedElementId ?: return
        val step = _uiState.value.nudgeStep
        updateElement(id, recordHistory = true) { el ->
            if (el.isLocked) return@updateElement el
            val dx = dxDirection * step
            val dy = dyDirection * step
            el.copy(
                x = SampLayoutMath.roundToTenth(el.x + dx),
                y = SampLayoutMath.roundToTenth(el.y + dy),
                textSizeX = if (el.font != TdFont.SPRITE_4 &&
                    el.font != TdFont.MODEL_PREVIEW_5 &&
                    el.alignment != TdAlignment.CENTER
                ) {
                    SampLayoutMath.roundToTenth(el.textSizeX + dx)
                } else {
                    el.textSizeX
                }
            )
        }
    }

    fun undo() {
        val project = _uiState.value.currentProject ?: return
        if (undoStack.isEmpty()) return
        val previous = undoStack.removeLast()
        redoStack.addLast(project.elements)
        val restored = project.copy(elements = previous)
        val validSelection = previous.firstOrNull { it.id == _uiState.value.selectedElementId }?.id
            ?: previous.lastOrNull()?.id
        _uiState.update {
            it.copy(
                currentProject = restored,
                selectedElementId = validSelection,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        persistProject(restored)
        preloadVisibleSprites()
    }

    fun redo() {
        val project = _uiState.value.currentProject ?: return
        if (redoStack.isEmpty()) return
        val next = redoStack.removeLast()
        undoStack.addLast(project.elements)
        val restored = project.copy(elements = next)
        val validSelection = next.firstOrNull { it.id == _uiState.value.selectedElementId }?.id
            ?: next.lastOrNull()?.id
        _uiState.update {
            it.copy(
                currentProject = restored,
                selectedElementId = validSelection,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        persistProject(restored)
        preloadVisibleSprites()
    }

    fun importPawnCode(rawCode: String, replaceExisting: Boolean): String? {
        val app = getApplication<Application>()
        if (rawCode.isBlank()) {
            return app.getString(R.string.import_error_empty)
        }
        val project = _uiState.value.currentProject ?: return null
        val startId = if (replaceExisting) 1L else ((project.elements.maxOfOrNull { it.id } ?: 0L) + 1L)
        val startZ = if (replaceExisting) 0 else project.elements.size
        val parsed = PawnImporter.parse(rawCode, startId = startId, startZOrder = startZ)
        if (parsed.elements.isEmpty()) {
            return app.getString(R.string.import_error_none_found)
        }

        val combined = if (replaceExisting) parsed.elements else project.elements + parsed.elements
        commitElementListChange(combined, selectId = parsed.elements.last().id)
        preloadVisibleSprites()
        viewModelScope.launch {
            _notices.emit(UiNotice.Message(app.getString(R.string.import_success_summary, parsed.elements.size)))
        }
        return null
    }

    fun generateExportCode(includeShowCalls: Boolean): String {
        val project = _uiState.value.currentProject ?: return ""
        return PawnExporter.generate(
            elements = project.elements,
            syntaxMode = project.syntaxMode,
            includeShowCalls = includeShowCalls
        )
    }

    private fun applyTransientElementUpdate(updatedElement: TextDrawElement) {
        val project = _uiState.value.currentProject ?: return
        val nextElements = project.elements.map { if (it.id == updatedElement.id) updatedElement else it }
        _uiState.update { it.copy(currentProject = project.copy(elements = nextElements)) }
    }

    private fun commitElementListChange(newElements: List<TextDrawElement>, selectId: Long?) {
        val project = _uiState.value.currentProject ?: return
        pushUndoSnapshot(project.elements)
        val normalized = newElements.mapIndexed { idx, item -> item.copy(zOrder = idx) }
        val updatedProject = project.copy(elements = normalized)
        _uiState.update {
            it.copy(
                currentProject = updatedProject,
                selectedElementId = selectId,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
        persistProject(updatedProject)
    }

    private fun pushUndoSnapshot(snapshot: List<TextDrawElement>) {
        if (undoStack.size >= maxHistorySize) {
            undoStack.removeFirst()
        }
        undoStack.addLast(snapshot)
        redoStack.clear()
    }

    private fun persistProject(project: TdProject) {
        viewModelScope.launch {
            projectRepository.saveProject(project)
        }
    }
}
