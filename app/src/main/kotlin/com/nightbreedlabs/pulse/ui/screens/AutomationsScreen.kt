package com.nightbreedlabs.pulse.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightbreedlabs.pulse.ui.components.TaskCard
import com.nightbreedlabs.pulse.ui.theme.*
import com.nightbreedlabs.pulse.viewmodel.PulseViewModel

@Composable
fun AutomationsScreen(
    viewModel: PulseViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.automatedTasks.collectAsState()
    var wolMac by remember { mutableStateOf("") }
    var wolSentMessage by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Master Trigger Banner
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TokyoOutlineVariant, RoundedCornerShape(26.dp)),
                shape = RoundedCornerShape(26.dp),
                color = TokyoSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Automated Diagnostics",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TokyoTextPrimary
                            )
                            Text(
                                text = "One-tap background network health & verification",
                                fontSize = 12.sp,
                                color = TokyoTextSecondary
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                tasks.forEach { task -> viewModel.executeTask(task.id) }
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = TokyoPrimaryContainer,
                                contentColor = TokyoPrimary
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Rounded.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run All", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Wake-on-LAN Tool Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TokyoOutlineVariant, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                color = TokyoSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = TokyoWarning,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Wake-on-LAN Transmitter",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TokyoTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = wolMac,
                        onValueChange = { wolMac = it },
                        label = { Text("Target MAC Address (e.g., AA:BB:CC:DD:EE:FF)") },
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

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = wolSentMessage,
                            fontSize = 11.sp,
                            color = TokyoSuccess
                        )

                        Button(
                            onClick = {
                                if (wolMac.isNotBlank()) {
                                    viewModel.sendWakeOnLan(wolMac)
                                    wolSentMessage = "Magic packet broadcasted!"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TokyoWarning,
                                contentColor = TokyoOnPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Send WOL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Task Cards
        items(tasks, key = { it.id }) { task ->
            TaskCard(
                task = task,
                onExecute = { viewModel.executeTask(task.id) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
