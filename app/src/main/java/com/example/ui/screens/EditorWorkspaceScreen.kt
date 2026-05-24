package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.ui.components.*
import com.example.ui.viewmodel.PhotoEditorViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorWorkspaceScreen(
    viewModel: PhotoEditorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ViewModel Flows
    val imageSrc by viewModel.currentImageSrc.collectAsStateWithLifecycle()
    val projectTitle by viewModel.projectTitle.collectAsStateWithLifecycle()
    val activeProjectId by viewModel.activeProjectId.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()
    
    // Manual Sliders
    val brightness by viewModel.brightness.collectAsStateWithLifecycle()
    val contrast by viewModel.contrast.collectAsStateWithLifecycle()
    val saturation by viewModel.saturation.collectAsStateWithLifecycle()
    val temperature by viewModel.temperature.collectAsStateWithLifecycle()
    val vignette by viewModel.vignette.collectAsStateWithLifecycle()
    val exposure by viewModel.exposure.collectAsStateWithLifecycle()
    val blur by viewModel.blur.collectAsStateWithLifecycle()

    // Transformations
    val orientationDegrees by viewModel.orientationDegrees.collectAsStateWithLifecycle()
    val isFlippedH by viewModel.isFlippedHorizontally.collectAsStateWithLifecycle()
    val isFlippedV by viewModel.isFlippedVertically.collectAsStateWithLifecycle()

    // Overlays
    val drawings by viewModel.drawings.collectAsStateWithLifecycle()
    val texts by viewModel.texts.collectAsStateWithLifecycle()
    val stickers by viewModel.stickers.collectAsStateWithLifecycle()
    val selectedTextId by viewModel.selectedTextId.collectAsStateWithLifecycle()
    val selectedStickerId by viewModel.selectedStickerId.collectAsStateWithLifecycle()

    // Saved database projects & AI
    val savedProjects by viewModel.savedProjects.collectAsStateWithLifecycle()
    val aiAdvice by viewModel.aiAdvice.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
    val exportMessage by viewModel.exportMessage.collectAsStateWithLifecycle()

    // Active Tool Category state
    var selectedToolCategory by remember { mutableStateOf("Filters") } // "Filters", "Adjust", "Transforms", "Draw", "Text", "Stickers", "AI", "History"

    // Custom Brush settings
    var brushColor by remember { mutableStateOf(Color.White) }
    var brushSize by remember { mutableStateOf(16f) }
    var isEraser by remember { mutableStateOf(false) }

    // Save prompt dialog state
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveTitleInput by remember { mutableStateOf(projectTitle) }

    // Instant comparative overlay state
    var showBeforeOnly by remember { mutableStateOf(false) }

    // Media picking SAF integration
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.loadNewImage(it.toString(), "Custom imported photo")
            Toast.makeText(context, "Photo loaded to canvas successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ProEdit v2.4",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = if (activeProjectId != null) "Editing: $projectTitle (#$activeProjectId)" else "Editing: $projectTitle (Unsaved)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    // Undo Action
                    IconButton(
                        onClick = { viewModel.undo() },
                        modifier = Modifier.testTag("undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Redo Action
                    IconButton(
                        onClick = { viewModel.redo() },
                        modifier = Modifier.testTag("redo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Save Floppy Action
                    IconButton(
                        onClick = {
                            saveTitleInput = projectTitle
                            showSaveDialog = true
                        },
                        modifier = Modifier.testTag("save_draft_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save draft", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Export / Render Action - Solid Pill design matching the High Density spec
                    Button(
                        onClick = {
                            viewModel.exportFinalImage { resultMessage ->
                                Toast.makeText(context, resultMessage, Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("export_composite_button")
                            .height(34.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(17.dp)
                    ) {
                        Text("Export", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Unified Interactive Live Render Canvas Viewport (HighDensityCanvasBg: #141218)
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxWidth()
                        .background(Color(0xFF141218))
                ) {
                    LiveCanvas(
                        imageSrc = imageSrc,
                        activeMode = selectedToolCategory,
                        activeFilter = activeFilter,
                        brightness = brightness,
                        contrast = contrast,
                        saturation = saturation,
                        temperature = temperature,
                        vignette = vignette,
                        exposure = exposure,
                        orientationDegrees = orientationDegrees,
                        isFlippedH = isFlippedH,
                        isFlippedV = isFlippedV,
                        drawings = drawings,
                        texts = texts,
                        stickers = stickers,
                        brushColor = brushColor,
                        brushSize = brushSize,
                        isEraser = isEraser,
                        selectedTextId = selectedTextId,
                        selectedStickerId = selectedStickerId,
                        onDrawStrokeAdded = { viewModel.addBrushStroke(it) },
                        onSelectText = { viewModel.selectTextLayer(it) },
                        onDragText = { id, dx, dy -> viewModel.updateTextOffset(id, dx, dy) },
                        onRemoveText = { viewModel.removeTextLayer(it) },
                        onSelectSticker = { viewModel.selectStickerLayer(it) },
                        onDragSticker = { id, dx, dy -> viewModel.updateStickerOffset(id, dx, dy) },
                        onRemoveSticker = { viewModel.removeStickerLayer(it) },
                        onScaleSticker = { id, s, r -> viewModel.updateStickerScaleAndRotation(id, s, r) },
                        showBeforeOnly = showBeforeOnly
                    )

                    // Floating Comparison trigger
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(14.dp)
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.7f))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        showBeforeOnly = true
                                        tryAwaitRelease()
                                        showBeforeOnly = false
                                    }
                                )
                            }
                            .testTag("compare_eyeball_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (showBeforeOnly) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Compare original held",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text("HOLD", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                        }
                    }

                    // Bottom Floating load image row
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .clickable { imagePickerLauncher.launch("image/*") }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Import source", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import Gallery Image", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                // Integrated High Density Controls Panel card (bg-[#25232A] / surface, top rounded corner radius: 28dp)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.95f),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // High Density TAB Navigation at the top of the Controls Sheet
                        ScrollableTabRow(
                            selectedTabIndex = getToolCategoryIndex(selectedToolCategory),
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = Color.Transparent, // Transparent so that Card surface color is preserved
                            contentColor = MaterialTheme.colorScheme.primary,
                            edgePadding = 12.dp,
                            divider = {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            }
                        ) {
                            val categories = listOf(
                                "Filters" to Icons.Default.Palette,
                                "Adjust" to Icons.Default.Tune,
                                "Transforms" to Icons.Default.Crop,
                                "Draw" to Icons.Default.Brush,
                                "Text" to Icons.Default.TextFormat,
                                "Stickers" to Icons.Default.SentimentSatisfiedAlt,
                                "AI" to Icons.Default.AutoAwesome,
                                "History" to Icons.Default.History
                            )

                            categories.forEach { (cat, icon) ->
                                val isSelected = selectedToolCategory == cat
                                Tab(
                                    selected = isSelected,
                                    onClick = { selectedToolCategory = cat },
                                    modifier = Modifier.testTag("tab_$cat"),
                                    text = {
                                        Text(
                                            text = cat.uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            letterSpacing = 1.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = cat,
                                            modifier = Modifier.size(18.dp),
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    },
                                    selectedContentColor = MaterialTheme.colorScheme.primary,
                                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Rich collapsible Tool Panels
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            AnimatedContent(
                                targetState = selectedToolCategory,
                                transitionSpec = {
                                    fadeIn() togetherWith fadeOut()
                                },
                                label = "active_panel_animation"
                            ) { targetCategory ->
                                when (targetCategory) {
                                    "Filters" -> FiltersPanel(
                                        activeFilter = activeFilter,
                                        onFilterSelected = { viewModel.setFilter(it) }
                                    )
                                    "Adjust" -> AdjustmentsPanel(
                                        brightness = brightness,
                                        contrast = contrast,
                                        saturation = saturation,
                                        temperature = temperature,
                                        vignette = vignette,
                                        exposure = exposure,
                                        blur = blur,
                                        onBrightnessChanged = { viewModel.setBrightness(it) },
                                        onContrastChanged = { viewModel.setContrast(it) },
                                        onSaturationChanged = { viewModel.setSaturation(it) },
                                        onTemperatureChanged = { viewModel.setTemperature(it) },
                                        onVignetteChanged = { viewModel.setVignette(it) },
                                        onExposureChanged = { viewModel.setExposure(it) },
                                        onBlurChanged = { viewModel.setBlur(it) },
                                        onReset = { viewModel.resetAdjustments() }
                                    )
                                    "Draw" -> DrawingPanel(
                                        brushColor = brushColor,
                                        brushSize = brushSize,
                                        isEraser = isEraser,
                                        onColorChanged = { brushColor = it },
                                        onSizeChanged = { brushSize = it },
                                        onEraserToggled = { isEraser = it },
                                        onClearAll = { viewModel.clearDrawings() }
                                    )
                                    "Text" -> TextOverlayPanel(
                                        onTextAdded = { text, color, size, bgCheck, bgHex ->
                                            viewModel.addTextLayer(text, color, size, bgCheck, bgHex)
                                        }
                                    )
                                    "Stickers" -> StickersPanel(
                                        onStickerSelected = { viewModel.addStickerLayer(it) }
                                    )
                                    "Transforms" -> CropRotatePanel(
                                        onRotateLeft = { viewModel.rotateLeft() },
                                        onRotateRight = { viewModel.rotateRight() },
                                        onFlipH = { viewModel.flipHorizontal() },
                                        onFlipV = { viewModel.flipVertical() }
                                    )
                                    "AI" -> AiCopilotPanel(
                                        adviceText = aiAdvice,
                                        isLoading = isAiLoading,
                                        onQuerySubmitted = { prompt ->
                                            // Extract bitmap securely using Coil resolver for AI context checks
                                            scope.launch {
                                                try {
                                                    val loader = ImageLoader(context)
                                                    val request = ImageRequest.Builder(context)
                                                        .data(imageSrc)
                                                        .allowHardware(false)
                                                        .build()
                                                    val result = loader.execute(request)
                                                    val loadedBitmap = (result as? SuccessResult)?.drawable?.let {
                                                        (it as? BitmapDrawable)?.bitmap
                                                    }
                                                    viewModel.queryAiCopilot(prompt, loadedBitmap)
                                                } catch (e: Exception) {
                                                    android.util.Log.e("EditorWorkspace", "Coil load exception: ${e.message}", e)
                                                    viewModel.queryAiCopilot(prompt, null)
                                                }
                                            }
                                        }
                                    )
                                    "History" -> Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                        TemplatesPanel(
                                            onTemplateSelected = { url, title ->
                                                viewModel.loadNewImage(url, title)
                                            }
                                        )
                                        HistoryProjectsList(
                                            projects = savedProjects,
                                            activeProjectId = activeProjectId,
                                            onProjectSelected = { viewModel.loadProject(it) },
                                            onProjectDeleted = { viewModel.deleteSavedProject(it) },
                                            onClearAll = { viewModel.clearDraftHistory() }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // High-Fidelity Rendering Modal Overlay Loader
            AnimatedVisibility(
                visible = isExporting,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(28, 28, 28)),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(
                                "BAKING PHOTO LAYERS...",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color.White,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = exportMessage,
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Local State Save Title prompts
            if (showSaveDialog) {
                AlertDialog(
                    onDismissRequest = { showSaveDialog = false },
                    title = { Text("Save Photo Draft") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Name this edit draft to store in SQLite history:", fontSize = 12.sp, color = Color.Gray)
                            OutlinedTextField(
                                value = saveTitleInput,
                                onValueChange = { saveTitleInput = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("save_draft_title_input"),
                                placeholder = { Text("Edit Title") }
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (saveTitleInput.trim().isNotEmpty()) {
                                    viewModel.saveProjectDraft(saveTitleInput.trim())
                                    showSaveDialog = false
                                    Toast.makeText(context, "State successfully written to local Room!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("save_draft_confirm")
                        ) {
                            Text("Save Draft")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSaveDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

private fun getToolCategoryIndex(cat: String): Int {
    return when (cat) {
        "Filters" -> 0
        "Adjust" -> 1
        "Transforms" -> 2
        "Draw" -> 3
        "Text" -> 4
        "Stickers" -> 5
        "AI" -> 6
        "History" -> 7
        else -> 0
    }
}
