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
    var selectedModel: String = "gemini-2.0-flash"
    var availableModels: List<String> = emptyList()

    /**
     * Queries Google AI Studio to retrieve the exact models supported by the provided API key.
     */
    suspend fun fetchAvailableModels(key: String = apiKey): List<String> = withContext(Dispatchers.IO) {
        if (key.isBlank()) return@withContext emptyList()
        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models?key=$key"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 10000

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(responseText)
                val modelsArray = json.optJSONArray("models")
                val supported = mutableListOf<String>()
                if (modelsArray != null) {
                    for (i in 0 until modelsArray.length()) {
                        val m = modelsArray.getJSONObject(i)
                        val name = m.optString("name").removePrefix("models/")
                        val methods = m.optJSONArray("supportedGenerationMethods")
                        var canGenerate = false
                        if (methods != null) {
                            for (j in 0 until methods.length()) {
                                if (methods.optString(j) == "generateContent") {
                                    canGenerate = true
                                    break
                                }
                            }
                        }
                        if (canGenerate) {
                            supported.add(name)
                        }
                    }
                }
                availableModels = supported

                // Automatically prioritize a fast, modern Flash model
                val prioritized = supported.firstOrNull { it == "gemini-2.0-flash" }
                    ?: supported.firstOrNull { it == "gemini-1.5-flash" }
                    ?: supported.firstOrNull { it.contains("flash") && !it.contains("lite") && !it.contains("thinking") }
                    ?: supported.firstOrNull { it.contains("flash") }
                    ?: supported.firstOrNull()

                if (prioritized != null) {
                    selectedModel = prioritized
                }
                supported
            } else {
                conn.disconnect()
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Executes content generation against Gemini with automatic fallback across viable models.
     */
    suspend fun askGemini(prompt: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext "Gemini API key is not configured. Please tap the key icon above to set your API key."
        }

        // Candidate fallback priority list
        val candidates = mutableListOf<String>()
        if (selectedModel.isNotBlank()) candidates.add(selectedModel)
        candidates.addAll(listOf(
            "gemini-2.0-flash",
            "gemini-1.5-flash",
            "gemini-2.0-flash-lite",
            "gemini-3.8-flash",
            "gemini-3.6-flash",
            "gemini-1.5-pro"
        ))
        val candidateModels = candidates.distinct()

        var lastError = "No response"

        for (model in candidateModels) {
            try {
                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
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

                val code = conn.responseCode
                if (code == 200) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.disconnect()

                    // Update selectedModel to the working model
                    selectedModel = model

                    val json = JSONObject(responseText)
                    val candidatesArr = json.optJSONArray("candidates")
                    if (candidatesArr != null && candidatesArr.length() > 0) {
                        val content = candidatesArr.getJSONObject(0).optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "No response content.")
                        }
                    }
                    return@withContext "Empty response from Gemini ($model)."
                } else {
                    val error = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
                    conn.disconnect()
                    lastError = "Model $model failed ($code): $error"

                    // If authentication or permission failed, stop trying further models
                    if (code == 403 || code == 401) {
                        return@withContext "Gemini API Authentication Error ($code): Please check your API key."
                    }
                    // For 404 (model not found / deprecated) or 400 (unsupported), loop to next model
                }
            } catch (e: Exception) {
                lastError = "Connection error with $model: ${e.message}"
            }
        }

        "All Gemini model candidates failed. Last error: $lastError"
    }
}
