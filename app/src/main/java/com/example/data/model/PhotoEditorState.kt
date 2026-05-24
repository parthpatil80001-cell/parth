package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

@JsonClass(generateAdapter = true)
data class PointData(
    val x: Float,
    val y: Float
)

@JsonClass(generateAdapter = true)
data class BrushStrokeData(
    val points: List<PointData>,
    val colorHex: String,
    val strokeWidth: Float,
    val isEraser: Boolean = false
)

@JsonClass(generateAdapter = true)
data class TextLayerData(
    val id: String,
    val text: String,
    val colorHex: String,
    val fontSize: Float,
    val xOffset: Float,
    val yOffset: Float,
    val backgroundColorHex: String = "#00000000",
    val hasBackground: Boolean = false
)

@JsonClass(generateAdapter = true)
data class StickerLayerData(
    val id: String,
    val sticker: String, // Emoji or Icon Name
    val scale: Float,
    val xOffset: Float,
    val yOffset: Float,
    val rotation: Float
)

object EditorSerializer {
    private val moshi: Moshi = Moshi.Builder().build()
    
    private val strokeListAdapter = moshi.adapter<List<BrushStrokeData>>(
        Types.newParameterizedType(List::class.java, BrushStrokeData::class.java)
    )
    
    private val textListAdapter = moshi.adapter<List<TextLayerData>>(
        Types.newParameterizedType(List::class.java, TextLayerData::class.java)
    )
    
    private val stickerListAdapter = moshi.adapter<List<StickerLayerData>>(
        Types.newParameterizedType(List::class.java, StickerLayerData::class.java)
    )

    fun strokesToJson(strokes: List<BrushStrokeData>): String {
        return try {
            strokeListAdapter.toJson(strokes)
        } catch (e: Exception) {
            "[]"
        }
    }

    fun jsonToStrokes(json: String?): List<BrushStrokeData> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            strokeListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun textsToJson(texts: List<TextLayerData>): String {
        return try {
            textListAdapter.toJson(texts)
        } catch (e: Exception) {
            "[]"
        }
    }

    fun jsonToTexts(json: String?): List<TextLayerData> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            textListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun stickersToJson(stickers: List<StickerLayerData>): String {
        return try {
            stickerListAdapter.toJson(stickers)
        } catch (e: Exception) {
            "[]"
        }
    }

    fun jsonToStickers(json: String?): List<StickerLayerData> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            stickerListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
