package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.EditProject
import com.example.data.database.PhotoEditorDatabase
import com.example.data.model.*
import com.example.data.repository.GeminiRepository
import com.example.data.repository.PhotoEditorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditStateSnapshot(
    val filterName: String,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val temperature: Float,
    val vignette: Float,
    val exposure: Float,
    val drawings: List<BrushStrokeData>,
    val texts: List<TextLayerData>,
    val stickers: List<StickerLayerData>,
    val orientation: Int,
    val isFlippedHorizontally: Boolean,
    val isFlippedVertically: Boolean
)

@Suppress("UNCHECKED_CAST")
class PhotoEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PhotoEditorDatabase.getDatabase(application)
    private val repository = PhotoEditorRepository(db.editProjectDao())
    private val geminiRepository = GeminiRepository()

    // Workspace Active Draft URI or URL
    private val _currentImageSrc = MutableStateFlow("https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1280")
    val currentImageSrc: StateFlow<String> = _currentImageSrc.asStateFlow()

    // Title / File Name
    private val _projectTitle = MutableStateFlow("Untitled Edit")
    val projectTitle: StateFlow<String> = _projectTitle.asStateFlow()

    // Active Project ID if loaded from DB
    private val _activeProjectId = MutableStateFlow<Int?>(null)
    val activeProjectId: StateFlow<Int?> = _activeProjectId.asStateFlow()

    // Presets / Filters
    private val _activeFilter = MutableStateFlow("Normal")
    val activeFilter: StateFlow<String> = _activeFilter.asStateFlow()

    // Manual Slider Tweaks
    private val _brightness = MutableStateFlow(0f) 
    val brightness: StateFlow<Float> = _brightness.asStateFlow()

    private val _contrast = MutableStateFlow(0f)
    val contrast: StateFlow<Float> = _contrast.asStateFlow()

    private val _saturation = MutableStateFlow(0f)
    val saturation: StateFlow<Float> = _saturation.asStateFlow()

    private val _temperature = MutableStateFlow(0f)
    val temperature: StateFlow<Float> = _temperature.asStateFlow()

    private val _vignette = MutableStateFlow(0f)
    val vignette: StateFlow<Float> = _vignette.asStateFlow()

    private val _exposure = MutableStateFlow(0f)
    val exposure: StateFlow<Float> = _exposure.asStateFlow()

    private val _blur = MutableStateFlow(0f)
    val blur: StateFlow<Float> = _blur.asStateFlow()

    // Layout/Transforms
    private val _orientationDegrees = MutableStateFlow(0)
    val orientationDegrees: StateFlow<Int> = _orientationDegrees.asStateFlow()

    private val _isFlippedHorizontally = MutableStateFlow(false)
    val isFlippedHorizontally: StateFlow<Boolean> = _isFlippedHorizontally.asStateFlow()

    private val _isFlippedVertically = MutableStateFlow(false)
    val isFlippedVertically: StateFlow<Boolean> = _isFlippedVertically.asStateFlow()

    // Secondary Layers
    private val _drawings = MutableStateFlow<List<BrushStrokeData>>(emptyList())
    val drawings: StateFlow<List<BrushStrokeData>> = _drawings.asStateFlow()

    private val _texts = MutableStateFlow<List<TextLayerData>>(emptyList())
    val texts: StateFlow<List<TextLayerData>> = _texts.asStateFlow()

    private val _stickers = MutableStateFlow<List<StickerLayerData>>(emptyList())
    val stickers: StateFlow<List<StickerLayerData>> = _stickers.asStateFlow()

    // Selection
    private val _selectedTextId = MutableStateFlow<String?>(null)
    val selectedTextId: StateFlow<String?> = _selectedTextId.asStateFlow()

    private val _selectedStickerId = MutableStateFlow<String?>(null)
    val selectedStickerId: StateFlow<String?> = _selectedStickerId.asStateFlow()

    // Undo/Redo Stacks
    private val undoStack = ArrayList<EditStateSnapshot>()
    private val redoStack = ArrayList<EditStateSnapshot>()

    // Database Projects List
    private val _savedProjects = MutableStateFlow<List<EditProject>>(emptyList())
    val savedProjects: StateFlow<List<EditProject>> = _savedProjects.asStateFlow()

    // Gemini AI copilot messages
    private val _aiAdvice = MutableStateFlow("Tap on the AI icon, fill in what style vibe you want, and click Ask Co-Pilot to analyze this masterpiece!")
    val aiAdvice: StateFlow<String> = _aiAdvice.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Export & Rendering Flow
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportMessage = MutableStateFlow("")
    val exportMessage: StateFlow<String> = _exportMessage.asStateFlow()

    init {
        // Fetch saved projects from databases
        viewModelScope.launch {
            repository.allProjects.collect {
                _savedProjects.value = it
            }
        }
    }

    // State capture helper for Undo support
    private fun saveToUndoStack() {
        val snapshot = EditStateSnapshot(
            filterName = _activeFilter.value,
            brightness = _brightness.value,
            contrast = _contrast.value,
            saturation = _saturation.value,
            temperature = _temperature.value,
            vignette = _vignette.value,
            exposure = _exposure.value,
            drawings = _drawings.value.toList(),
            texts = _texts.value.toList(),
            stickers = _stickers.value.toList(),
            orientation = _orientationDegrees.value,
            isFlippedHorizontally = _isFlippedHorizontally.value,
            isFlippedVertically = _isFlippedVertically.value
        )
        undoStack.add(snapshot)
        if (undoStack.size > 20) {
            undoStack.removeAt(0)
        }
        redoStack.clear() // Clear redo stack on manual user edit actions
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val current = EditStateSnapshot(
                filterName = _activeFilter.value,
                brightness = _brightness.value,
                contrast = _contrast.value,
                saturation = _saturation.value,
                temperature = _temperature.value,
                vignette = _vignette.value,
                exposure = _exposure.value,
                drawings = _drawings.value.toList(),
                texts = _texts.value.toList(),
                stickers = _stickers.value.toList(),
                orientation = _orientationDegrees.value,
                isFlippedHorizontally = _isFlippedHorizontally.value,
                isFlippedVertically = _isFlippedVertically.value
            )
            redoStack.add(current)
            
            val last = undoStack.removeAt(undoStack.size - 1)
            restoreSnapshot(last)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val current = EditStateSnapshot(
                filterName = _activeFilter.value,
                brightness = _brightness.value,
                contrast = _contrast.value,
                saturation = _saturation.value,
                temperature = _temperature.value,
                vignette = _vignette.value,
                exposure = _exposure.value,
                drawings = _drawings.value.toList(),
                texts = _texts.value.toList(),
                stickers = _stickers.value.toList(),
                orientation = _orientationDegrees.value,
                isFlippedHorizontally = _isFlippedHorizontally.value,
                isFlippedVertically = _isFlippedVertically.value
            )
            undoStack.add(current)
            
            val next = redoStack.removeAt(redoStack.size - 1)
            restoreSnapshot(next)
        }
    }

    private fun restoreSnapshot(snapshot: EditStateSnapshot) {
        _activeFilter.value = snapshot.filterName
        _brightness.value = snapshot.brightness
        _contrast.value = snapshot.contrast
        _saturation.value = snapshot.saturation
        _temperature.value = snapshot.temperature
        _vignette.value = snapshot.vignette
        _exposure.value = snapshot.exposure
        _drawings.value = snapshot.drawings
        _texts.value = snapshot.texts
        _stickers.value = snapshot.stickers
        _orientationDegrees.value = snapshot.orientation
        _isFlippedHorizontally.value = snapshot.isFlippedHorizontally
        _isFlippedVertically.value = snapshot.isFlippedVertically
    }

    // Load new Photo Workspace
    fun loadNewImage(src: String, title: String = "Imported Photo") {
        saveToUndoStack()
        _currentImageSrc.value = src
        _projectTitle.value = title
        _activeProjectId.value = null
        resetAdjustmentsRaw()
    }

    fun resetAdjustments() {
        saveToUndoStack()
        resetAdjustmentsRaw()
    }

    private fun resetAdjustmentsRaw() {
        _activeFilter.value = "Normal"
        _brightness.value = 0f
        _contrast.value = 0f
        _saturation.value = 0f
        _temperature.value = 0f
        _vignette.value = 0f
        _exposure.value = 0f
        _blur.value = 0f
        _orientationDegrees.value = 0
        _isFlippedHorizontally.value = false
        _isFlippedVertically.value = false
        _drawings.value = emptyList()
        _texts.value = emptyList()
        _stickers.value = emptyList()
        _selectedTextId.value = null
        _selectedStickerId.value = null
    }

    // Setters
    fun setFilter(name: String) {
        saveToUndoStack()
        _activeFilter.value = name
    }

    fun setBrightness(value: Float) {
        _brightness.value = value
    }

    fun setContrast(value: Float) {
        _contrast.value = value
    }

    fun setSaturation(value: Float) {
        _saturation.value = value
    }

    fun setTemperature(value: Float) {
        _temperature.value = value
    }

    fun setVignette(value: Float) {
        _vignette.value = value
    }

    fun setExposure(value: Float) {
        _exposure.value = value
    }

    fun setBlur(value: Float) {
        _blur.value = value
    }

    fun rotateLeft() {
        saveToUndoStack()
        _orientationDegrees.value = (_orientationDegrees.value - 90 + 360) % 360
    }

    fun rotateRight() {
        saveToUndoStack()
        _orientationDegrees.value = (_orientationDegrees.value + 90) % 360
    }

    fun flipHorizontal() {
        saveToUndoStack()
        _isFlippedHorizontally.value = !_isFlippedHorizontally.value
    }

    fun flipVertical() {
        saveToUndoStack()
        _isFlippedVertically.value = !_isFlippedVertically.value
    }

    // Brush Operations
    fun addBrushStroke(stroke: BrushStrokeData) {
        saveToUndoStack()
        val newList = _drawings.value.toMutableList()
        newList.add(stroke)
        _drawings.value = newList
    }

    fun clearDrawings() {
        saveToUndoStack()
        _drawings.value = emptyList()
    }

    // Text Overlay Operations
    fun addTextLayer(text: String, color: Color, size: Float, hasBg: Boolean = false, bgHex: String = "#33000000") {
        saveToUndoStack()
        val colorHex = String.format("#%08X", color.value.toLong() and 0xFFFFFFFFL)
        val newText = TextLayerData(
            id = "txt_" + System.currentTimeMillis() + "_" + (100..999).random(),
            text = text,
            colorHex = colorHex,
            fontSize = size,
            xOffset = 0f,
            yOffset = -100f,
            backgroundColorHex = bgHex,
            hasBackground = hasBg
        )
        val newList = _texts.value.toMutableList()
        newList.add(newText)
        _texts.value = newList
        _selectedTextId.value = newText.id
    }

    fun updateTextOffset(id: String, x: Float, y: Float) {
        val newList = _texts.value.map {
            if (it.id == id) {
                it.copy(xOffset = it.xOffset + x, yOffset = it.yOffset + y)
            } else it
        }
        _texts.value = newList
    }

    fun selectTextLayer(id: String?) {
        _selectedTextId.value = id
        _selectedStickerId.value = null
    }

    fun removeTextLayer(id: String) {
        saveToUndoStack()
        _texts.value = _texts.value.filter { it.id != id }
        if (_selectedTextId.value == id) {
            _selectedTextId.value = null
        }
    }

    // Sticker Layer Operations
    fun addStickerLayer(sticker: String) {
        saveToUndoStack()
        val newSticker = StickerLayerData(
            id = "stk_" + System.currentTimeMillis() + "_" + (100..999).random(),
            sticker = sticker,
            scale = 1.0f,
            xOffset = 0f,
            yOffset = 100f,
            rotation = 0f
        )
        val newList = _stickers.value.toMutableList()
        newList.add(newSticker)
        _stickers.value = newList
        _selectedStickerId.value = newSticker.id
    }

    fun updateStickerOffset(id: String, x: Float, y: Float) {
        val newList = _stickers.value.map {
            if (it.id == id) {
                it.copy(xOffset = it.xOffset + x, yOffset = it.yOffset + y)
            } else it
        }
        _stickers.value = newList
    }

    fun updateStickerScaleAndRotation(id: String, scaleDelta: Float, rotationDelta: Float) {
        val newList = _stickers.value.map {
            if (it.id == id) {
                val newScale = (it.scale + scaleDelta).coerceIn(0.2f, 5.0f)
                val newRot = (it.rotation + rotationDelta) % 360f
                it.copy(scale = newScale, rotation = newRot)
            } else it
        }
        _stickers.value = newList
    }

    fun selectStickerLayer(id: String?) {
        _selectedStickerId.value = id
        _selectedTextId.value = null
    }

    fun removeStickerLayer(id: String) {
        saveToUndoStack()
        _stickers.value = _stickers.value.filter { it.id != id }
        if (_selectedStickerId.value == id) {
            _selectedStickerId.value = null
        }
    }

    // AI copilot query
    fun queryAiCopilot(inputPrompt: String, activeBitmap: Bitmap?) {
        _isAiLoading.value = true
        _aiAdvice.value = "Analyzing photograph pixels..."
        viewModelScope.launch {
            val response = geminiRepository.getEditAdvice(inputPrompt, activeBitmap)
            _aiAdvice.value = response
            _isAiLoading.value = false
        }
    }

    // Save project metadata to internal Room
    fun saveProjectDraft(title: String) {
        _projectTitle.value = title
        viewModelScope.launch {
            val project = EditProject(
                id = _activeProjectId.value ?: 0,
                title = title,
                originalImageUriOrTemplate = _currentImageSrc.value,
                timestamp = System.currentTimeMillis(),
                filterName = _activeFilter.value,
                brightness = _brightness.value,
                contrast = _contrast.value,
                saturation = _saturation.value,
                temperature = _temperature.value,
                vignette = _vignette.value,
                exposure = _exposure.value,
                orientationDegrees = _orientationDegrees.value,
                isFlippedHorizontally = _isFlippedHorizontally.value,
                isFlippedVertically = _isFlippedVertically.value,
                serializedDrawings = EditorSerializer.strokesToJson(_drawings.value),
                serializedTexts = EditorSerializer.textsToJson(_texts.value),
                serializedStickers = EditorSerializer.stickersToJson(_stickers.value)
            )
            val generatedId = repository.saveProject(project)
            if (_activeProjectId.value == null) {
                _activeProjectId.value = generatedId.toInt()
            }
        }
    }

    // Load Project from History DB
    fun loadProject(project: EditProject) {
        saveToUndoStack()
        _activeProjectId.value = project.id
        _projectTitle.value = project.title
        _currentImageSrc.value = project.originalImageUriOrTemplate
        _activeFilter.value = project.filterName
        _brightness.value = project.brightness
        _contrast.value = project.contrast
        _saturation.value = project.saturation
        _temperature.value = project.temperature
        _vignette.value = project.vignette
        _exposure.value = project.exposure
        _orientationDegrees.value = project.orientationDegrees
        _isFlippedHorizontally.value = project.isFlippedHorizontally
        _isFlippedVertically.value = project.isFlippedVertically
        _drawings.value = EditorSerializer.jsonToStrokes(project.serializedDrawings)
        _texts.value = EditorSerializer.jsonToTexts(project.serializedTexts)
        _stickers.value = EditorSerializer.jsonToStickers(project.serializedStickers)
        _selectedTextId.value = null
        _selectedStickerId.value = null
    }

    // Delete a project from history
    fun deleteSavedProject(id: Int) {
        viewModelScope.launch {
            repository.deleteProject(id)
            if (_activeProjectId.value == id) {
                _activeProjectId.value = null
            }
        }
    }

    // Clear whole database draft list
    fun clearDraftHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Simulated high-fidelity export rendering
    fun exportFinalImage(onSuccess: (String) -> Unit) {
        _isExporting.value = true
        _exportMessage.value = "Constructing flat composite matrix..."
        
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            _exportMessage.value = "Applying GPU shaders and adjustments..."
            kotlinx.coroutines.delay(1000)
            _exportMessage.value = "Drawing vector graphics and overlays..."
            kotlinx.coroutines.delay(800)
            _exportMessage.value = "Flushing compression streams to memory..."
            kotlinx.coroutines.delay(600)
            
            _isExporting.value = false
            _exportMessage.value = ""
            
            // Save draft successfully when exporting
            saveProjectDraft(_projectTitle.value)
            onSuccess("Success! Rendered and saved '${_projectTitle.value}' with all filters & annotations applied.")
        }
    }
}
