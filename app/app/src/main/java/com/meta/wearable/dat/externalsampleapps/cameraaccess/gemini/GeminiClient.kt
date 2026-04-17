/*
 * Meta Ray-Ban visual assistant — Gemini API client.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.gemini

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object GeminiClient {
  private const val TAG = "GeminiClient"
  private const val MODEL = "gemini-2.5-flash"
  private const val ENDPOINT =
      "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"
  private const val JPEG_QUALITY = 85
  private const val MAX_EDGE_PX = 1024

  suspend fun describe(bitmap: Bitmap, apiKey: String, prompt: String): Result<String> =
      withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
          return@withContext Result.failure(IllegalStateException("GEMINI_API_KEY is empty"))
        }
        runCatching {
          val resized = resizeForUpload(bitmap)
          val base64 = bitmapToBase64Jpeg(resized)
          val payload = buildPayload(prompt, base64)
          val response = post(apiKey, payload)
          extractText(response)
        }
      }

  private fun resizeForUpload(bitmap: Bitmap): Bitmap {
    val maxEdge = maxOf(bitmap.width, bitmap.height)
    if (maxEdge <= MAX_EDGE_PX) return bitmap
    val scale = MAX_EDGE_PX.toFloat() / maxEdge
    val w = (bitmap.width * scale).toInt()
    val h = (bitmap.height * scale).toInt()
    return Bitmap.createScaledBitmap(bitmap, w, h, true)
  }

  private fun bitmapToBase64Jpeg(bitmap: Bitmap): String {
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
    return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
  }

  private fun buildPayload(prompt: String, imageBase64: String): String {
    val imagePart =
        JSONObject()
            .put(
                "inline_data",
                JSONObject().put("mime_type", "image/jpeg").put("data", imageBase64),
            )
    val textPart = JSONObject().put("text", prompt)
    val parts = JSONArray().put(imagePart).put(textPart)
    val content = JSONObject().put("parts", parts)
    return JSONObject().put("contents", JSONArray().put(content)).toString()
  }

  private fun post(apiKey: String, body: String): String {
    val url = URL("$ENDPOINT?key=$apiKey")
    val conn = url.openConnection() as HttpURLConnection
    return try {
      conn.requestMethod = "POST"
      conn.doOutput = true
      conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
      conn.connectTimeout = 15_000
      conn.readTimeout = 60_000
      conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

      val code = conn.responseCode
      val stream = if (code in 200..299) conn.inputStream else conn.errorStream
      val text = stream.bufferedReader().use { it.readText() }
      if (code !in 200..299) {
        Log.e(TAG, "HTTP $code: $text")
        throw RuntimeException("Gemini API HTTP $code")
      }
      text
    } finally {
      conn.disconnect()
    }
  }

  private fun extractText(responseJson: String): String {
    val root = JSONObject(responseJson)
    val candidates = root.optJSONArray("candidates") ?: throw RuntimeException("No candidates")
    if (candidates.length() == 0) throw RuntimeException("Empty candidates")
    val parts =
        candidates
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
    val builder = StringBuilder()
    for (i in 0 until parts.length()) {
      val text = parts.getJSONObject(i).optString("text", "")
      if (text.isNotEmpty()) {
        if (builder.isNotEmpty()) builder.append("\n")
        builder.append(text)
      }
    }
    return builder.toString().ifBlank { throw RuntimeException("No text in response") }
  }
}
