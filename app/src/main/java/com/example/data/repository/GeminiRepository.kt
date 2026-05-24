package com.example.data.repository

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

class GeminiRepository {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        
    private val requestAdapter = moshi.adapter(GeminiRequest::class.java)
    private val responseAdapter = moshi.adapter(GeminiResponse::class.java)

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun getEditAdvice(prompt: String, bitmap: Bitmap?): String = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext "API Key is missing or default. Please configure your GEMINI_API_KEY in the AI Studio Secrets panel. Meanwhile, I can simulate an editor review: Your composition is wonderful! Try increasing contrast +0.15 and applying the Cinematic Preset for a dramatic neon style."
            }

            val parts = mutableListOf<GeminiPart>()
            parts.add(GeminiPart(text = "$prompt\n\nAnalyze this image relative to photo adjustments (brightness, contrast, saturation, filters) and suggest specific, practical parameters to enhance it. Keep your advice brief, inspiring, and list standard tweaks."))

            if (bitmap != null) {
                val base64Image = bitmapToBase64(bitmap)
                parts.add(GeminiPart(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image)))
            }

            val requestPayload = GeminiRequest(
                contents = listOf(GeminiContent(parts = parts))
            )

            val jsonString = requestAdapter.toJson(requestPayload)
            
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val requestBody = jsonString.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    Log.e("GeminiRepository", "Error API call: $bodyStr")
                    return@withContext "I'm experiencing connectivity issues checking the photo details. Try boosting the contrast slightly and applying the cyber preset to make this look amazing!"
                }
                val bodyStr = response.body?.string() ?: return@withContext "Empty response from advisor."
                val geminiResponse = responseAdapter.fromJson(bodyStr)
                val advice = geminiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                advice ?: "Your image already looks spectacular! Try applying a Warm Preset and adding a custom white text overlay."
            }
        } catch (e: Exception) {
            Log.e("GeminiRepository", "Ex: ${e.message}", e)
            "Could not reach AI co-pilot: ${e.localizedMessage}. Try increasing saturation +20% and exposure +15% manually to make this scenery pop!"
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Resize slightly to avoid large transfer payloads
        val scale = if (bitmap.width > 800 || bitmap.height > 800) {
            val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
            if (bitmap.width > bitmap.height) {
                Bitmap.createScaledBitmap(bitmap, 800, (800 / ratio).toInt(), true)
            } else {
                Bitmap.createScaledBitmap(bitmap, (800 * ratio).toInt(), 800, true)
            }
        } else {
            bitmap
        }
        scale.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
