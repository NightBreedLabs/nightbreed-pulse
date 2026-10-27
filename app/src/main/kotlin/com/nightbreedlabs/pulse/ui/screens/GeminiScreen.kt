package com.nightbreedlabs.pulse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val messages by viewModel.chatMessages.collectAsState()
    val isThinking by viewModel.isAiThinking.collectAsState()
    var inputQuery by remember { mutableStateOf("") }
    var apiKeyInput by remember { mutableStateOf(GeminiDiagnostics.apiKey) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

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
                    Text(
                        text = "Gemini Diagnostics AI",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TokyoTextPrimary
                    )
                }

                IconButton(
                    onClick = { showApiKeyDialog = !showApiKeyDialog },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Key,
                        contentDescription = "API Key",
                        tint = if (apiKeyInput.isNotBlank()) TokyoSuccess else TokyoWarning,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (showApiKeyDialog) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = TokyoSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Enter Google Gemini API Key:",
                        fontSize = 11.sp,
                        color = TokyoTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            GeminiDiagnostics.apiKey = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TokyoPrimary,
                            unfocusedBorderColor = TokyoOutline,
                            focusedTextColor = TokyoTextPrimary,
                            unfocusedTextColor = TokyoTextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
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
                                    text = "Gemini is analyzing...",
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
