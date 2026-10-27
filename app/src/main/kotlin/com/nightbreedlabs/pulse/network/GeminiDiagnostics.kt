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
    var selectedModel: String = "gemini-3.8-flash"
    var thinkingLevel: String = "medium"
    var availableModels: List<String> = emptyList()
    var lastError: String = ""

    fun isDeprecatedOrRestricted(model: String): Boolean {
        val lower = model.lowercase()
        return lower.contains("1.5") || lower.contains("2.0") || lower.contains("2.5")
    }

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

            val code = conn.responseCode
            if (code == 200) {
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

                // Automatically prioritize modern, active Gemini 3 Flash models
                val prioritized = supported.firstOrNull { it == "gemini-3.8-flash" }
                    ?: supported.firstOrNull { it == "gemini-3.5-flash" }
                    ?: supported.firstOrNull { it == "gemini-3.5-flash-lite" }
                    ?: supported.firstOrNull { it == "gemini-3.1-flash-lite" }
                    ?: supported.firstOrNull { it.startsWith("gemini-3") && it.contains("flash") }
                    ?: supported.firstOrNull { it.contains("flash") && !isDeprecatedOrRestricted(it) }
                    ?: supported.firstOrNull { !isDeprecatedOrRestricted(it) }
                    ?: supported.firstOrNull()

                if (prioritized != null && (isDeprecatedOrRestricted(selectedModel) || !supported.contains(selectedModel))) {
                    selectedModel = prioritized
                }
                supported
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
                conn.disconnect()
                lastError = "Model detection failed ($code): $err"
                emptyList()
            }
        } catch (e: Exception) {
            lastError = "Model detection error: ${e.message}"
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

        // Dynamically fetch available models if not already discovered
        if (availableModels.isEmpty()) {
            fetchAvailableModels(apiKey)
        }

        // Candidate fallback priority list
        val candidates = mutableListOf<String>()

        // 1. If currently selected model is valid and not deprecated, prioritize it
        if (selectedModel.isNotBlank() && !isDeprecatedOrRestricted(selectedModel)) {
            candidates.add(selectedModel)
        }

        // 2. Add detected available models that support generateContent
        if (availableModels.isNotEmpty()) {
            candidates.addAll(availableModels.filter { it.contains("flash") && !isDeprecatedOrRestricted(it) })
            candidates.addAll(availableModels.filter { !isDeprecatedOrRestricted(it) })
            candidates.addAll(availableModels)
        }

        // 3. Fallback to Gemini 3 series standards
        candidates.addAll(listOf(
            "gemini-3.8-flash",
            "gemini-3.5-flash",
            "gemini-3.5-flash-lite",
            "gemini-3.1-flash-lite"
        ))

        val candidateModels = candidates.distinct()

        var lastFailureDetail = if (lastError.isNotBlank()) lastError else "No response"

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

                // Inject thinkingLevel configuration for Gemini 3 models (e.g. medium reasoning)
                if (thinkingLevel.isNotBlank()) {
                    val thinkingConfig = JSONObject().put("thinkingLevel", thinkingLevel.lowercase())
                    val generationConfig = JSONObject().put("thinkingConfig", thinkingConfig)
                    requestBody.put("generationConfig", generationConfig)
                }

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
                            val textParts = mutableListOf<String>()
                            var fallbackThought = ""
                            for (p in 0 until parts.length()) {
                                val partObj = parts.getJSONObject(p)
                                val text = partObj.optString("text", "")
                                val isThought = partObj.optBoolean("thought", false)
                                if (text.isNotBlank()) {
                                    if (!isThought) {
                                        textParts.add(text)
                                    } else {
                                        fallbackThought = text
                                    }
                                }
                            }
                            val finalAnswer = if (textParts.isNotEmpty()) textParts.joinToString("\n\n") else fallbackThought
                            if (finalAnswer.isNotBlank()) {
                                return@withContext finalAnswer
                            }
                        }
                    }
                    return@withContext "Empty response from Gemini ($model)."
                } else {
                    val error = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
                    conn.disconnect()
                    lastFailureDetail = "Model $model failed ($code): $error"

                    // If request failed specifically due to thinkingConfig, retry immediately without generationConfig
                    if (code == 400 && (error.contains("thinking") || error.contains("generationConfig"))) {
                        try {
                            val retryConn = url.openConnection() as HttpURLConnection
                            retryConn.requestMethod = "POST"
                            retryConn.setRequestProperty("Content-Type", "application/json")
                            retryConn.connectTimeout = 8000
                            retryConn.readTimeout = 15000
                            retryConn.doOutput = true

                            val simpleBody = JSONObject().put("contents", contentsArray)
                            OutputStreamWriter(retryConn.outputStream).use { writer ->
                                writer.write(simpleBody.toString())
                                writer.flush()
                            }
                            if (retryConn.responseCode == 200) {
                                val retryText = retryConn.inputStream.bufferedReader().use { it.readText() }
                                retryConn.disconnect()
                                selectedModel = model
                                val retryJson = JSONObject(retryText)
                                val cArr = retryJson.optJSONArray("candidates")
                                val text = cArr?.getJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.getJSONObject(0)?.optString("text", "")
                                if (!text.isNullOrBlank()) return@withContext text
                            } else {
                                retryConn.disconnect()
                            }
                        } catch (_: Exception) {}
                    }

                    // If authentication or permission failed, stop trying further models immediately
                    if (code == 403 || code == 401) {
                        return@withContext "Gemini API Authentication Error ($code): Please verify your API key.\n\nDetails: $error"
                    }
                    // For 404 (model deprecated / not found) or 400 (unsupported), loop to next candidate
                }
            } catch (e: Exception) {
                lastFailureDetail = "Connection error with $model: ${e.message}"
            }
        }

        "All Gemini model candidates failed.\n\nDiagnostic details:\n$lastFailureDetail"
    }
}
