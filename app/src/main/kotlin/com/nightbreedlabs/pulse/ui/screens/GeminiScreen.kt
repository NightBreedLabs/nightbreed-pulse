package com.nightbreedlabs.pulse.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightbreedlabs.pulse.network.GeminiDiagnostics
import com.nightbreedlabs.pulse.ui.theme.*
import com.nightbreedlabs.pulse.viewmodel.PulseViewModel

@Composable
fun GeminiScreen(
    viewModel: PulseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("pulse_prefs", Context.MODE_PRIVATE) }

    val messages by viewModel.chatMessages.collectAsState()
    val isThinking by viewModel.isAiThinking.collectAsState()
    val activeModel by viewModel.currentAiModel.collectAsState()
    val activeThinkingLevel by viewModel.currentThinkingLevel.collectAsState()
    val availableModels by viewModel.availableAiModels.collectAsState()

    var inputQuery by remember { mutableStateOf("") }
    var apiKeyInput by remember { mutableStateOf(GeminiDiagnostics.apiKey) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Restore saved API Key and Model on first launch
    LaunchedEffect(Unit) {
        val savedKey = prefs.getString("gemini_api_key", "") ?: ""
        val rawSavedModel = prefs.getString("gemini_model", "gemini-3.8-flash") ?: "gemini-3.8-flash"
        val savedModel = if (GeminiDiagnostics.isDeprecatedOrRestricted(rawSavedModel)) {
            "gemini-3.8-flash"
        } else {
            rawSavedModel
        }
        if (savedModel != rawSavedModel) {
            prefs.edit().putString("gemini_model", savedModel).apply()
        }

        val savedThinking = prefs.getString("gemini_thinking_level", "medium") ?: "medium"
        GeminiDiagnostics.thinkingLevel = savedThinking
        viewModel.selectThinkingLevel(savedThinking)

        apiKeyInput = savedKey
        GeminiDiagnostics.apiKey = savedKey
        GeminiDiagnostics.selectedModel = savedModel
        viewModel.selectAiModel(savedModel)

        if (savedKey.isNotBlank()) {
            viewModel.refreshAiModels(savedKey)
        }
    }

    LaunchedEffect(activeModel) {
        if (activeModel.isNotBlank() && !GeminiDiagnostics.isDeprecatedOrRestricted(activeModel)) {
            prefs.edit().putString("gemini_model", activeModel).apply()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // AI Header & API Key Row
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, TokyoOutlineVariant, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = TokyoSurface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Psychology,
                        contentDescription = null,
                        tint = TokyoSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Gemini Diagnostics AI",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TokyoTextPrimary
                        )
                        Text(
                            text = "$activeModel • ${activeThinkingLevel.replaceFirstChar { it.uppercase() }} Thinking",
                            fontSize = 10.sp,
                            color = TokyoSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showApiKeyDialog = !showApiKeyDialog },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Key,
                            contentDescription = "API Key & Model",
                            tint = if (apiKeyInput.isNotBlank()) TokyoSuccess else TokyoWarning,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Expanded Configuration Panel
        if (showApiKeyDialog) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = TokyoSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Google Gemini API Key:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TokyoTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it.trim()
                            GeminiDiagnostics.apiKey = apiKeyInput
                            prefs.edit().putString("gemini_api_key", apiKeyInput).apply()
                            if (apiKeyInput.length > 25) {
                                viewModel.refreshAiModels(apiKeyInput)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TokyoPrimary,
                            unfocusedBorderColor = TokyoOutline,
                            focusedTextColor = TokyoTextPrimary,
                            unfocusedTextColor = TokyoTextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        placeholder = { Text("Paste AIzaSy... key", fontSize = 11.sp) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Model:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TokyoTextSecondary
                        )
                        TextButton(
                            onClick = { viewModel.refreshAiModels(apiKeyInput) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh",
                                tint = TokyoPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Detect Models", fontSize = 10.sp, color = TokyoPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Model Selection Chips
                    val defaultModelCandidates = listOf(
                        "gemini-3.8-flash",
                        "gemini-3.5-flash",
                        "gemini-3.5-flash-lite",
                        "gemini-3.1-flash-lite"
                    )
                    val displayModels = if (availableModels.isNotEmpty()) availableModels else defaultModelCandidates

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(displayModels) { model ->
                            val isSelected = (model == activeModel)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) TokyoPrimary else TokyoSurface,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.selectAiModel(model)
                                        prefs.edit().putString("gemini_model", model).apply()
                                    }
                            ) {
                                Text(
                                    text = model,
                                    fontSize = 10.sp,
                                    color = if (isSelected) TokyoOnPrimary else TokyoTextPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Thinking Level (Reasoning Depth):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TokyoTextSecondary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val thinkingLevels = listOf("low", "medium", "high")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        thinkingLevels.forEach { level ->
                            val isSelected = (level.lowercase() == activeThinkingLevel.lowercase())
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) TokyoSecondary else TokyoSurface,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.selectThinkingLevel(level)
                                        prefs.edit().putString("gemini_thinking_level", level).apply()
                                    }
                            ) {
                                Text(
                                    text = "${level.replaceFirstChar { it.uppercase() }} Reasoning",
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) TokyoBackground else TokyoTextPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Diagnostic Action Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val prompts = listOf(
                "Analyze discovered network devices",
                "Diagnose TV HDMI / ARC issue",
                "Windows SMB file sharing not visible",
                "Why is gateway latency high?"
            )
            items(prompts) { prompt ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TokyoSurfaceVariant,
                    modifier = Modifier.border(1.dp, TokyoOutlineVariant, RoundedCornerShape(12.dp))
                ) {
                    TextButton(
                        onClick = { viewModel.sendAiPrompt(prompt) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(prompt, fontSize = 11.sp, color = TokyoTextPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.isUser
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        ),
                        color = if (isUser) TokyoPrimaryContainer else TokyoSurface,
                        modifier = Modifier
                            .widthIn(max = 300.dp)
                            .border(
                                1.dp,
                                if (isUser) TokyoPrimary.copy(alpha = 0.4f) else TokyoOutlineVariant,
                                RoundedCornerShape(18.dp)
                            )
                    ) {
                        Text(
                            text = msg.text,
                            fontSize = 13.sp,
                            color = TokyoTextPrimary,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            if (isThinking) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = TokyoSurface,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = TokyoSecondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Gemini ($activeModel • ${activeThinkingLevel.replaceFirstChar { it.uppercase() }}) is reasoning...",
                                    fontSize = 12.sp,
                                    color = TokyoTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Query Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                placeholder = { Text("Ask Gemini about network, TVs, or PCs...", fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TokyoPrimary,
                    unfocusedBorderColor = TokyoOutline,
                    focusedTextColor = TokyoTextPrimary,
                    unfocusedTextColor = TokyoTextPrimary
                ),
                shape = RoundedCornerShape(20.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputQuery.isNotBlank()) {
                        viewModel.sendAiPrompt(inputQuery)
                        inputQuery = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TokyoPrimary)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Send,
                    contentDescription = "Send",
                    tint = TokyoOnPrimary
                )
            }
        }
    }
}
