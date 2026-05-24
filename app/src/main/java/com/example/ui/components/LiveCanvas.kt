package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.BrushStrokeData
import com.example.data.model.PointData
import com.example.data.model.StickerLayerData
import com.example.data.model.TextLayerData
import kotlin.math.roundToInt

@Composable
fun LiveCanvas(
    modifier: Modifier = Modifier,
    imageSrc: String,
    activeMode: String, // "Draw", "Text", "Stickers", "Adjust", "Crop", "Filters", "AI"
    activeFilter: String,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    temperature: Float,
    vignette: Float,
    exposure: Float,
    orientationDegrees: Int,
    isFlippedH: Boolean,
    isFlippedV: Boolean,
    
    // Core Layers
    drawings: List<BrushStrokeData>,
    texts: List<TextLayerData>,
    stickers: List<StickerLayerData>,
    
    // Active Brush Settings
    brushColor: Color,
    brushSize: Float,
    isEraser: Boolean,

    // Selection indicators
    selectedTextId: String?,
    selectedStickerId: String?,
    
    // Change Listeners
    onDrawStrokeAdded: (BrushStrokeData) -> Unit,
    onSelectText: (String?) -> Unit,
    onDragText: (String, Float, Float) -> Unit,
    onRemoveText: (String) -> Unit,
    onSelectSticker: (String?) -> Unit,
    onDragSticker: (String, Float, Float) -> Unit,
    onRemoveSticker: (String) -> Unit,
    onScaleSticker: (String, Float, Float) -> Unit,
    
    // Comparative overlay
    showBeforeOnly: Boolean = false
) {
    // Collect active drawing path points temporarily
    var activePathPoints = remember { mutableStateListOf<Offset>() }
    
    // Calculate final compiled ColorMatrix unless "Before mode" is held down
    val finalColorMatrix = remember(showBeforeOnly, activeFilter, brightness, contrast, saturation, temperature, exposure) {
        if (showBeforeOnly) {
            ColorMatrix() // Clean identity
        } else {
            getAdjustedColorMatrix(activeFilter, brightness, contrast, saturation, temperature, exposure)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(25, 25, 25))
            .pointerInput(activeMode, brushColor, brushSize, isEraser) {
                if (activeMode == "Draw") {
                    detectDragGestures(
                        onDragStart = { offset ->
                            activePathPoints.clear()
                            activePathPoints.add(offset)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            activePathPoints.add(change.position)
                        },
                        onDragEnd = {
                            if (activePathPoints.isNotEmpty()) {
                                val brushColorHex = String.format("#%08X", brushColor.toArgb())
                                val stroke = BrushStrokeData(
                                    points = activePathPoints.map { PointData(it.x, it.y) },
                                    colorHex = brushColorHex,
                                    strokeWidth = brushSize,
                                    isEraser = isEraser
                                )
                                onDrawStrokeAdded(stroke)
                            }
                            activePathPoints.clear()
                        }
                    )
                }
            }
            .pointerInput(Unit) {
                // Clear selections when tapping empty canvas space
                detectTapGestures(
                    onTap = {
                        onSelectText(null)
                        onSelectSticker(null)
                    }
                )
            }
    ) {
        // Frame to contain full scale image + its overlay grids
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .rotate(if (showBeforeOnly) 0f else orientationDegrees.toFloat())
                    .scale(
                        scaleX = if (showBeforeOnly) 1f else (if (isFlippedH) -1f else 1f),
                        scaleY = if (showBeforeOnly) 1f else (if (isFlippedV) -1f else 1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                
                // Active draft bitmap
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageSrc)
                        .crossfade(true)
                        .allowHardware(false) // Required for matrix modifications
                        .build(),
                    contentDescription = "Active Canvas Photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(1.2f)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp)),
                    colorFilter = ColorFilter.colorMatrix(finalColorMatrix)
                )

                // Sub-Canvas for drawing strokes (Dynamic vector graphics overlays)
                if (!showBeforeOnly) {
                    Canvas(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        // 1. Draw Saved Strokes
                        drawings.forEach { stroke ->
                            val path = Path()
                            val points = stroke.points
                            if (points.isNotEmpty()) {
                                path.moveTo(points[0].x, points[0].y)
                                for (i in 1 until points.size) {
                                    path.lineTo(points[i].x, points[i].y)
                                }
                                drawPath(
                                    path = path,
                                    color = if (stroke.isEraser) Color(25, 25, 25) else safeParseColor(stroke.colorHex, Color.White),
                                    style = Stroke(
                                        width = stroke.strokeWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }

                        // 2. Draw active currently-being-dragged brush path
                        if (activePathPoints.size > 1) {
                            val activePath = Path()
                            activePath.moveTo(activePathPoints[0].x, activePathPoints[0].y)
                            for (i in 1 until activePathPoints.size) {
                                activePath.lineTo(activePathPoints[i].x, activePathPoints[i].y)
                            }
                            drawPath(
                                path = activePath,
                                color = if (isEraser) Color(25, 25, 25) else brushColor,
                                style = Stroke(
                                    width = brushSize,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }

                // 3. Absolute positioned Text overlays
                if (!showBeforeOnly) {
                    texts.forEach { textLayer ->
                        var localX by remember { mutableStateOf(textLayer.xOffset) }
                        var localY by remember { mutableStateOf(textLayer.yOffset) }
                        
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(textLayer.xOffset.roundToInt(), textLayer.yOffset.roundToInt()) }
                                .pointerInput(textLayer.id) {
                                    detectDragGestures(
                                        onDragStart = { onSelectText(textLayer.id) },
                                        onDragEnd = {},
                                        onDragCancel = {},
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            onDragText(textLayer.id, dragAmount.x, dragAmount.y)
                                        }
                                    )
                                }
                                .wrapContentSize()
                                .background(
                                    color = if (textLayer.hasBackground) safeParseColor(textLayer.backgroundColorHex, Color.Transparent) else Color.Transparent,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .border(
                                    width = if (selectedTextId == textLayer.id) 2.dp else 0.dp,
                                    color = if (selectedTextId == textLayer.id) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(4.dp)
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = textLayer.text,
                                    color = safeParseColor(textLayer.colorHex, Color.White),
                                    fontSize = textLayer.fontSize.sp,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                
                                if (selectedTextId == textLayer.id) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.error)
                                            .clickable { onRemoveText(textLayer.id) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove annotation",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Sticker/Emoji Draggable Overlays
                if (!showBeforeOnly) {
                    stickers.forEach { stickerLayer ->
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(stickerLayer.xOffset.roundToInt(), stickerLayer.yOffset.roundToInt()) }
                                .rotate(stickerLayer.rotation)
                                .scale(stickerLayer.scale)
                                .pointerInput(stickerLayer.id) {
                                    detectDragGestures(
                                        onDragStart = { onSelectSticker(stickerLayer.id) },
                                        onDragEnd = {},
                                        onDragCancel = {},
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            onDragSticker(stickerLayer.id, dragAmount.x, dragAmount.y)
                                        }
                                    )
                                }
                                .wrapContentSize()
                                .border(
                                    width = if (selectedStickerId == stickerLayer.id) 1.5.dp else 0.dp,
                                    color = if (selectedStickerId == stickerLayer.id) MaterialTheme.colorScheme.secondary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = stickerLayer.sticker,
                                    fontSize = 40.sp
                                )

                                if (selectedStickerId == stickerLayer.id) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Quick Action Buttons
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.error)
                                                .clickable { onRemoveSticker(stickerLayer.id) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete sticker",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.secondary)
                                                .clickable { 
                                                    // Quick Scale Up
                                                    onScaleSticker(stickerLayer.id, 0.2f, 15f)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Scale sticker",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Vignette dark gradient edge overlay
                if (vignette > 0f) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color.Transparent, 
                                        Color.Black.copy(alpha = vignette * 0.95f)
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

// Matrix helper mathematically compounding values in color matrix formats
fun getAdjustedColorMatrix(
    filterName: String,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    temperature: Float,
    exposure: Float
): ColorMatrix {
    val matrix = ColorMatrix()

    // 1. Filters Preset
    when (filterName) {
        "Sepia" -> {
            matrix.set(ColorMatrix(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        "Grayscale" -> {
            matrix.setToSaturation(0f)
        }
        "Vintage" -> {
            matrix.set(ColorMatrix(floatArrayOf(
                0.95f, 0f, 0f, 0f, 20f,
                0f, 0.90f, 0f, 0f, 10f,
                0f, 0f, 0.80f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        "Cinematic" -> {
            matrix.set(ColorMatrix(floatArrayOf(
                0.9f, 0f, 0f, 0f, 5f,
                0f, 0.95f, 0f, 0f, 5f,
                0f, 0f, 1.15f, 0f, 15f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        "Cyberpunk" -> {
            matrix.set(ColorMatrix(floatArrayOf(
                1.3f, 0f, 0f, 0f, 35f,
                0f, 0.7f, 0f, 0f, -30f,
                0f, 0f, 1.5f, 0f, 45f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        "Noir" -> {
            matrix.set(ColorMatrix(floatArrayOf(
                1.5f * 0.213f, 1.5f * 0.715f, 1.5f * 0.072f, 0f, -40f,
                1.5f * 0.213f, 1.5f * 0.715f, 1.5f * 0.072f, 0f, -40f,
                1.5f * 0.213f, 1.5f * 0.715f, 1.5f * 0.072f, 0f, -40f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        "Warm Sunset" -> {
            matrix.set(ColorMatrix(floatArrayOf(
                1.2f, 0f, 0f, 0f, 25f,
                0f, 1.0f, 0f, 0f, 15f,
                0f, 0f, 0.75f, 0f, -20f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        "Cold Frost" -> {
            matrix.set(ColorMatrix(floatArrayOf(
                0.85f, 0f, 0f, 0f, -10f,
                0f, 0.95f, 0f, 0f, 5f,
                0f, 0f, 1.3f, 0f, 30f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
    }

    // 2. Brightness Slider
    if (brightness != 0f) {
        val bShift = brightness * 255f
        val bMatrix = ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, bShift,
            0f, 1f, 0f, 0f, bShift,
            0f, 0f, 1f, 0f, bShift,
            0f, 0f, 0f, 1f, 0f
        ))
        matrix.set(multiplyColorMatrices(matrix, bMatrix))
    }

    // 3. Exposure Slider
    if (exposure != 0f) {
        val expMultiplier = 1f + exposure
        val eMatrix = ColorMatrix(floatArrayOf(
            expMultiplier, 0f, 0f, 0f, 0f,
            0f, expMultiplier, 0f, 0f, 0f,
            0f, 0f, expMultiplier, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        matrix.set(multiplyColorMatrices(matrix, eMatrix))
    }

    // 4. Contrast Slider
    if (contrast != 0f) {
        val scale = 1f + contrast
        val translate = (-0.5f * scale + 0.5f) * 255f
        val cMatrix = ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        matrix.set(multiplyColorMatrices(matrix, cMatrix))
    }

    // 5. Saturation Slider
    if (saturation != 0f) {
        val sVal = 1f + saturation
        val sMatrix = ColorMatrix().apply { setToSaturation(sVal) }
        matrix.set(multiplyColorMatrices(matrix, sMatrix))
    }

    // 6. Temperature Slider
    if (temperature != 0f) {
        val rShift = temperature * 32f
        val bShift = -temperature * 32f
        val tMatrix = ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, rShift,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, bShift,
            0f, 0f, 0f, 1f, 0f
        ))
        matrix.set(multiplyColorMatrices(matrix, tMatrix))
    }

    return matrix
}

private fun multiplyColorMatrices(a: ColorMatrix, b: ColorMatrix): ColorMatrix {
    val result = FloatArray(20)
    val arrA = a.values
    val arrB = b.values

    for (row in 0..3) {
        for (col in 0..4) {
            val i = row * 5 + col
            if (col == 4) {
                result[i] = arrA[row*5 + 0] * arrB[0*5 + 4] +
                            arrA[row*5 + 1] * arrB[1*5 + 4] +
                            arrA[row*5 + 2] * arrB[2*5 + 4] +
                            arrA[row*5 + 3] * arrB[3*5 + 4] +
                            arrA[row*5 + 4]
            } else {
                result[i] = arrA[row*5 + 0] * arrB[0*5 + col] +
                            arrA[row*5 + 1] * arrB[1*5 + col] +
                            arrA[row*5 + 2] * arrB[2*5 + col] +
                            arrA[row*5 + 3] * arrB[3*5 + col]
            }
        }
    }
    return ColorMatrix(result)
}

fun safeParseColor(hex: String, defaultColor: Color = Color.White): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        defaultColor
    }
}
