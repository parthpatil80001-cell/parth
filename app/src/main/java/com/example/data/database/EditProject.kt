package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "edit_projects")
data class EditProject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val originalImageUriOrTemplate: String, // Template ID (e.g., "sunset", "neon") or Device URI
    val timestamp: Long = System.currentTimeMillis(),
    
    // Quick adjustment state
    val filterName: String = "Normal",
    val brightness: Float = 0f, // -1f to 1f
    val contrast: Float = 0f,   // -1f to 1f
    val saturation: Float = 0f, // -1f to 1f
    val temperature: Float = 0f, // -1f to 1f
    val vignette: Float = 0f, // 0f to 1f
    val exposure: Float = 0f, // -1f to 1f
    val blur: Float = 0f, // 0f to 1f
    
    // Layout rotation / transformations
    val orientationDegrees: Int = 0,
    val isFlippedHorizontally: Boolean = false,
    val isFlippedVertically: Boolean = false,
    
    // Secondary Layer Canvas Data (as serialized JSON)
    val serializedDrawings: String = "[]",
    val serializedTexts: String = "[]",
    val serializedStickers: String = "[]"
)
