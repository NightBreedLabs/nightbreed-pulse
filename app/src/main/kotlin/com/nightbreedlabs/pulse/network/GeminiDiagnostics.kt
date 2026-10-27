package com.nightbreedlabs.pulse.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GeminiDiagnostics {

    var apiKey: String = ""

    suspend fun askGemini(prompt: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext "Gemini API key is not configured. Please enter your API key in the settings/bar above."
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 8000
            conn.readTimeout = 15000
            conn.doOutput = true

            val partsArray = JSONArray().put(JSONObject().put("text", prompt))
            val contentsArray = JSONArray().put(JSONObject().put("parts", partsArray))
            val requestBody = JSONObject().put("contents", contentsArray)

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val json = JSONObject(responseText)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "No response content.")
                    }
                }
                "Empty response from Gemini."
            } else {
                val error = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP ${conn.responseCode}"
                conn.disconnect()
                "Gemini API Error (${conn.responseCode}): $error"
            }
        } catch (e: Exception) {
            "Connection failed: ${e.message}"
        }
    }
}
