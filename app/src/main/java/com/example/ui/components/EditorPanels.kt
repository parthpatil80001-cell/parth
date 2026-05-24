package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.EditProject
import com.example.ui.viewmodel.PhotoEditorViewModel

// Preset/Filters Panel
@Composable
fun FiltersPanel(
    activeFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val filters = listOf(
        "Normal" to Color(0xFFE0E0E0),
        "Sepia" to Color(0xFFD2B48C),
        "Grayscale" to Color(0xFF9E9E9E),
        "Vintage" to Color(0xFFCFB53B),
        "Cinematic" to Color(0xFF008080),
        "Cyberpunk" to Color(0xFFFF007F),
        "Noir" to Color(0xFF303030),
        "Warm Sunset" to Color(0xFFFF4500),
        "Cold Frost" to Color(0xFF00BFFF)
    )

    Column(modifier = Modifier.padding(12.dp)) {
        Text(
            text = "PRESETS & FILTERS",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(filters) { (name, tintColor) ->
                val isSelected = activeFilter == name
                Card(
                    modifier = Modifier
                        .width(90.dp)
                        .height(85.dp)
                        .testTag("filter_card_$name")
                        .clickable { onFilterSelected(name) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                         else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = borderFromSelection(isSelected)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(6.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(tintColor)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = name,
                            fontSize = 11.sp,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// Manual Adjustments Sliders
@Composable
fun AdjustmentsPanel(
    brightness: Float,
    contrast: Float,
    saturation: Float,
    temperature: Float,
    vignette: Float,
    exposure: Float,
    blur: Float,
    onBrightnessChanged: (Float) -> Unit,
    onContrastChanged: (Float) -> Unit,
    onSaturationChanged: (Float) -> Unit,
    onTemperatureChanged: (Float) -> Unit,
    onVignetteChanged: (Float) -> Unit,
    onExposureChanged: (Float) -> Unit,
    onBlurChanged: (Float) -> Unit,
    onReset: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(scrollState)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MANUAL ADJUSTMENTS",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(
                onClick = onReset,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset sliders", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset All", fontSize = 12.sp)
            }
        }

        AdjustmentSliderItem(label = "Exposure", value = exposure, range = -1f..1f, valueFormatter = { "+${(it * 100).toInt()}%" }, onValueChange = onExposureChanged)
        AdjustmentSliderItem(label = "Brightness", value = brightness, range = -1f..1f, valueFormatter = { "+${(it * 100).toInt()}%" }, onValueChange = onBrightnessChanged)
        AdjustmentSliderItem(label = "Contrast", value = contrast, range = -1f..1f, valueFormatter = { "+${(it * 100).toInt()}%" }, onValueChange = onContrastChanged)
        AdjustmentSliderItem(label = "Saturation", value = saturation, range = -1f..1f, valueFormatter = { "+${(it * 100).toInt()}%" }, onValueChange = onSaturationChanged)
        AdjustmentSliderItem(label = "Warmth (Temp)", value = temperature, range = -1f..1f, valueFormatter = { if (it > 0) "Warm" else if (it < 0) "Cool" else "Neutral" }, onValueChange = onTemperatureChanged)
        AdjustmentSliderItem(label = "Vignette Dark", value = vignette, range = 0f..1f, valueFormatter = { "${(it * 100).toInt()}%" }, onValueChange = onVignetteChanged)
        AdjustmentSliderItem(label = "Simulated Blur", value = blur, range = 0f..1f, valueFormatter = { "${(it * 100).toInt()}%" }, onValueChange = onBlurChanged)
    }
}

@Composable
fun AdjustmentSliderItem(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueFormatter: (Float) -> String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = valueFormatter(value),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.height(30.dp).testTag("slider_${label.lowercase()}"),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            )
        )
    }
}

// Brush / Doodle Panel
@Composable
fun DrawingPanel(
    brushColor: Color,
    brushSize: Float,
    isEraser: Boolean,
    onColorChanged: (Color) -> Unit,
    onSizeChanged: (Float) -> Unit,
    onEraserToggled: (Boolean) -> Unit,
    onClearAll: () -> Unit
) {
    val brushColors = listOf(
        Color.White, Color.Red, Color(0xFFFF5722), Color.Yellow, 
        Color.Green, Color(0xFF00BCD4), Color.Blue, Color(0xFF9C27B0), Color(0xFFFF4081)
    )

    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BRUSH & PAINTING",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onClearAll, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Clear Canvas", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brush / Eraser Mode triggers
            FilterChip(
                selected = !isEraser,
                onClick = { onEraserToggled(false) },
                label = { Text("Brush Pen") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            FilterChip(
                selected = isEraser,
                onClick = { onEraserToggled(true) },
                label = { Text("Eraser Roll") },
                leadingIcon = { Icon(Icons.Default.FormatPaint, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        // Color Palette Selectors
        if (!isEraser) {
            Column {
                Text("Select Shader Color:", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 4.dp)
                ) {
                    items(brushColors) { color ->
                        val isSelected = brushColor.toArgb() == color.toArgb()
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.DarkGray,
                                    shape = CircleShape
                                )
                                .clickable { onColorChanged(color) }
                        )
                    }
                }
            }
        }

        // Brush Weight Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = (if (isEraser) "Eraser Width" else "Brush Size").uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${brushSize.toInt()} DP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = brushSize,
                onValueChange = onSizeChanged,
                valueRange = 4f..80f,
                modifier = Modifier.height(30.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                )
            )
        }
    }
}

// Text Panel
@Composable
fun TextOverlayPanel(
    onTextAdded: (String, Color, Float, Boolean, String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color.White) }
    var hasBackground by remember { mutableStateOf(false) }
    var fontSizeSlider by remember { mutableStateOf(24f) }

    val textColors = listOf(
        Color.White, Color.Black, Color.Yellow, Color.Red, 
        Color.Green, Color.Cyan, Color.Magenta, Color(0xFFE91E63)
    )

    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "ADD TEXT WATERMARK",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            placeholder = { Text("Type sticker text here...") },
            modifier = Modifier.fillMaxWidth().testTag("text_overlay_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(35, 35, 35),
                unfocusedContainerColor = Color(30, 30, 30)
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = hasBackground,
                    onCheckedChange = { hasBackground = it },
                    modifier = Modifier.testTag("text_bg_checkbox")
                )
                Text("Add translucent background box", fontSize = 12.sp, color = Color.LightGray)
            }
        }

        // Color selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Ink Color:", fontSize = 12.sp, color = Color.Gray)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(textColors) { color ->
                    val isSelected = selectedColor.toArgb() == color.toArgb()
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = color }
                    )
                }
            }
        }

        // Text Sizing Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FONT SIZE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${fontSizeSlider.toInt()} SP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = fontSizeSlider,
                onValueChange = { fontSizeSlider = it },
                valueRange = 12f..60f,
                modifier = Modifier.height(30.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                )
            )
        }

        Button(
            onClick = {
                if (textInput.isNotEmpty()) {
                    onTextAdded(
                        textInput,
                        selectedColor,
                        fontSizeSlider,
                        hasBackground,
                        if (selectedColor == Color.Black) "#80FFFFFF" else "#80000000"
                    )
                    textInput = ""
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("add_text_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Inject Text Layer")
        }
    }
}

// Sticker Select Panel
@Composable
fun StickersPanel(
    onStickerSelected: (String) -> Unit
) {
    val stickers = listOf(
        "🕶️", "👑", "💖", "⭐", "🔥", "🎉", "🚀", "💡", "🍀", "🐱", 
        "👻", "🎃", "🎨", "📷", "🍕", "🍦", "🌈", "🎈", "🎄", "👽"
    )

    Column(modifier = Modifier.padding(12.dp)) {
        Text(
            text = "STICKERS & OVERLAYS",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 48.dp),
            modifier = Modifier.height(130.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(stickers) { sticker ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(40, 40, 40))
                        .clickable { onStickerSelected(sticker) }
                        .testTag("sticker_btn_$sticker"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = sticker, fontSize = 26.sp)
                }
            }
        }
    }
}

// Rotate & Crop Panel
@Composable
fun CropRotatePanel(
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onFlipH: () -> Unit,
    onFlipV: () -> Unit
) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "CANVAS TRANSFORMATION SUITE",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onRotateLeft,
                modifier = Modifier.weight(1f).testTag("rotate_left_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(50, 50, 50))
            ) {
                Icon(Icons.Default.RotateLeft, contentDescription = "Rotate Left")
                Spacer(modifier = Modifier.width(6.dp))
                Text("90° L", fontSize = 12.sp)
            }
            Button(
                onClick = onRotateRight,
                modifier = Modifier.weight(1f).testTag("rotate_right_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(50, 50, 50))
            ) {
                Icon(Icons.Default.RotateRight, contentDescription = "Rotate Right")
                Spacer(modifier = Modifier.width(6.dp))
                Text("90° R", fontSize = 12.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onFlipH,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(40, 40, 40))
            ) {
                Icon(Icons.Default.Flip, contentDescription = "Flip Horizontally")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Flip Horiz", fontSize = 11.sp)
            }
            Button(
                onClick = onFlipV,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(40, 40, 40))
            ) {
                Icon(Icons.Default.FlipToBack, contentDescription = "Flip Vertically")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Flip Vert", fontSize = 11.sp)
            }
        }
    }
}

// AI Copilot Panel
@Composable
fun AiCopilotPanel(
    adviceText: String,
    isLoading: Boolean,
    onQuerySubmitted: (String) -> Unit
) {
    var userPrompt by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "AI CO-PILOT ADVISOR",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp, max = 110.dp),
            colors = CardDefaults.cardColors(containerColor = Color(32, 32, 32))
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(10.dp).verticalScroll(rememberScrollState()),
                contentAlignment = if (isLoading) Alignment.Center else Alignment.TopStart
            ) {
                if (isLoading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Gemini is analyzing...", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Text(
                        text = adviceText,
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = userPrompt,
                onValueChange = { userPrompt = it },
                placeholder = { Text("Ask advice (e.g., retro, cinematic)...", fontSize = 11.sp) },
                modifier = Modifier.weight(1f).testTag("ai_prompt_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(24, 24, 24),
                    unfocusedContainerColor = Color(24, 24, 24)
                )
            )

            Button(
                onClick = {
                    if (userPrompt.trim().isNotEmpty()) {
                        onQuerySubmitted(userPrompt)
                        userPrompt = ""
                    } else {
                        onQuerySubmitted("Analyze and suggest optimal style and adjustments for this composition.")
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.testTag("submit_ai_button"),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Ask", modifier = Modifier.size(16.dp))
            }
        }
    }
}

// Built-in Stock Templates Panel
@Composable
fun TemplatesPanel(
    onTemplateSelected: (String, String) -> Unit
) {
    val templates = listOf(
        Triple("Sunset Beach", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1280", Color(255, 127, 80)),
        Triple("Neon City", "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=1280", Color(138, 43, 226)),
        Triple("Cool Alpine", "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=1280", Color(70, 130, 180)),
        Triple("Retro Wheels", "https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?w=1280", Color(218, 165, 32)),
        Triple("Forest Path", "https://images.unsplash.com/photo-1448375240586-882707db888b?w=1280", Color(34, 139, 34))
    )

    Column(modifier = Modifier.padding(12.dp)) {
        Text(
            text = "LOAD HIGH-RES SOURCE TEMPLATE",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(templates) { (title, url, placeholderColor) ->
                Card(
                    modifier = Modifier
                        .width(115.dp)
                        .height(54.dp)
                        .testTag("template_card_$title")
                        .clickable { onTemplateSelected(url, title) },
                    colors = CardDefaults.cardColors(containerColor = Color(40, 40, 40))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.matchParentSize().background(placeholderColor.copy(alpha = 0.15f)).clip(RoundedCornerShape(4.dp)))
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// Historial Saved Projects Grid/List Layout
@Composable
fun HistoryProjectsList(
    projects: List<EditProject>,
    activeProjectId: Int?,
    onProjectSelected: (EditProject) -> Unit,
    onProjectDeleted: (Int) -> Unit,
    onClearAll: () -> Unit
) {
    Column(modifier = Modifier.padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SAVED SQLite HISTORY (${projects.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            if (projects.isNotEmpty()) {
                TextButton(
                    onClick = onClearAll,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Db", fontSize = 11.sp)
                }
            }
        }

        if (projects.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No saved edits yet. Apply transformations and tap the Save floppy above to store history!",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(projects) { project ->
                    val isOpened = activeProjectId == project.id
                    Card(
                        modifier = Modifier
                            .width(130.dp)
                            .height(60.dp)
                            .testTag("history_card_${project.id}")
                            .clickable { onProjectSelected(project) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isOpened) MaterialTheme.colorScheme.primaryContainer
                                             else Color(38, 38, 38)
                        ),
                        border = borderFromSelection(isOpened)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                                Text(
                                    text = project.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    color = if (isOpened) MaterialTheme.colorScheme.onPrimaryContainer else Color.White
                                )
                                Text(
                                    text = "Preset: ${project.filterName}",
                                    fontSize = 9.sp,
                                    color = Color.Gray
                                )
                            }

                            IconButton(
                                onClick = { onProjectDeleted(project.id) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun borderFromSelection(isSelected: Boolean): BorderStroke? =
    if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
