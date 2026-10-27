package com.nightbreedlabs.pulse.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightbreedlabs.pulse.ui.theme.*
import com.nightbreedlabs.pulse.viewmodel.PulseViewModel

@Composable
fun SmartTvScreen(
    viewModel: PulseViewModel,
    modifier: Modifier = Modifier
) {
    val vizioInfo by viewModel.activeVizio.collectAsState()
    val rokuInfo by viewModel.activeRoku.collectAsState()
    var targetIp by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Target IP Bar
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TokyoOutlineVariant, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = TokyoSurface
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Smart TV Inspector",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TokyoTextPrimary
                    )
                    Text(
                        text = "Vizio SmartCast (7345/9000) & Roku ECP (8060)",
                        fontSize = 12.sp,
                        color = TokyoTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = targetIp,
                        onValueChange = { targetIp = it },
                        label = { Text("TV Local IP Address (e.g. 192.168.1.120)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TokyoPrimary,
                            unfocusedBorderColor = TokyoOutline,
                            focusedTextColor = TokyoTextPrimary,
                            unfocusedTextColor = TokyoTextPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { if (targetIp.isNotBlank()) viewModel.inspectVizio(targetIp) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TokyoWarning,
                                contentColor = TokyoOnPrimary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Query Vizio", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { if (targetIp.isNotBlank()) viewModel.inspectRoku(targetIp) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TokyoSecondary,
                                contentColor = TokyoOnPrimary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Query Roku", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Vizio Diagnostic Card
        vizioInfo?.let { vizio ->
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, TokyoOutlineVariant, RoundedCornerShape(22.dp)),
                    shape = RoundedCornerShape(22.dp),
                    color = TokyoSurface
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "VIZIO SMARTCAST",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TokyoWarning
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (vizio.isOnline) TokyoSuccess.copy(alpha = 0.2f) else TokyoError.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (vizio.isOnline) "ONLINE" else "OFFLINE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (vizio.isOnline) TokyoSuccess else TokyoError,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Target IP: ${vizio.ip}", fontSize = 13.sp, color = TokyoTextPrimary)
                        Text(text = "Active Input: ${vizio.currentInput}", fontSize = 12.sp, color = TokyoTextSecondary)

                        if (vizio.systemInfo.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = TokyoSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = vizio.systemInfo,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TokyoTextPrimary,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Roku Diagnostic Card & Keypad
        rokuInfo?.let { roku ->
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, TokyoOutlineVariant, RoundedCornerShape(22.dp)),
                    shape = RoundedCornerShape(22.dp),
                    color = TokyoSurface
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ROKU TV (${roku.friendlyName})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TokyoSecondary
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (roku.isOnline) TokyoSuccess.copy(alpha = 0.2f) else TokyoError.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (roku.isOnline) "ONLINE" else "OFFLINE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (roku.isOnline) TokyoSuccess else TokyoError,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Model: ${roku.modelName}", fontSize = 12.sp, color = TokyoTextSecondary)

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Remote Quick Actions:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TokyoTextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { viewModel.sendRokuKey(roku.ip, "Home") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            FilledTonalButton(
                                onClick = { viewModel.sendRokuKey(roku.ip, "PowerOn") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            FilledTonalButton(
                                onClick = { viewModel.sendRokuKey(roku.ip, "InputHDMI1") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("HDMI 1", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            FilledTonalButton(
                                onClick = { viewModel.sendRokuKey(roku.ip, "InputHDMI2") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("HDMI 2", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (roku.appsList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Installed Apps (${roku.appsList.size}):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TokyoTextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(roku.appsList) { app ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = TokyoSurfaceVariant
                                    ) {
                                        Text(
                                            text = app,
                                            fontSize = 11.sp,
                                            color = TokyoTextSecondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
