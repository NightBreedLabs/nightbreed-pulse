package com.nightbreedlabs.pulse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightbreedlabs.pulse.model.DeviceType
import com.nightbreedlabs.pulse.ui.components.DeviceCard
import com.nightbreedlabs.pulse.ui.theme.*
import com.nightbreedlabs.pulse.viewmodel.PulseViewModel

@Composable
fun ScannerScreen(
    viewModel: PulseViewModel,
    onInspectDevice: (String, DeviceType) -> Unit,
    modifier: Modifier = Modifier
) {
    val wifiInfo by viewModel.wifiInfo.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val scanStatusText by viewModel.scanStatusText.collectAsState()
    val devices by viewModel.discoveredDevices.collectAsState()
    val targetSubnetPrefix by viewModel.targetSubnetPrefix.collectAsState()

    var showCustomInput by remember { mutableStateOf(false) }
    var customInputText by remember { mutableStateOf(targetSubnetPrefix) }
    var selectedFilter by remember { mutableStateOf<DeviceType?>(null) }

    val filteredDevices = remember(devices, selectedFilter) {
        if (selectedFilter == null) devices
        else devices.filter { it.deviceType == selectedFilter }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Quick Stats & Control Card
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
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                text = "Subnet Discovery",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TokyoTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Target: $targetSubnetPrefix.0/24 • ${devices.size} Discovered",
                                fontSize = 12.sp,
                                color = TokyoTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = {
                                if (isScanning) viewModel.cancelScan()
                                else viewModel.startSubnetScan()
                            },
                            modifier = Modifier
                                .defaultMinSize(minWidth = 92.dp)
                                .height(42.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isScanning) TokyoError else TokyoPrimary,
                                contentColor = TokyoOnPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isScanning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isScanning) "Stop" else "Scan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Target Subnet Presets
                    Text(
                        text = "Target Subnet Presets:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TokyoTextMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Home LAN via Tailscale
                        item {
                            val isSelected = targetSubnetPrefix == "192.168.8"
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) TokyoPrimaryContainer else TokyoSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.setTargetSubnetPrefix("192.168.8")
                                        customInputText = "192.168.8"
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Home,
                                        contentDescription = null,
                                        tint = if (isSelected) TokyoPrimary else TokyoTextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Home (192.168.8)",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) TokyoPrimary else TokyoTextPrimary
                                    )
                                }
                            }
                        }

                        // Tailnet Nodes Scan
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = TokyoSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.startTailnetScan()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VpnKey,
                                        contentDescription = null,
                                        tint = TokyoCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Tailnet Mesh",
                                        fontSize = 11.sp,
                                        color = TokyoCyan
                                    )
                                }
                            }
                        }

                        // Local / Cousin's LAN (192.168.1)
                        item {
                            val isSelected = targetSubnetPrefix == "192.168.1"
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) TokyoPrimaryContainer else TokyoSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.setTargetSubnetPrefix("192.168.1")
                                        customInputText = "192.168.1"
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Wifi,
                                        contentDescription = null,
                                        tint = if (isSelected) TokyoPrimary else TokyoTextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "LAN (192.168.1)",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) TokyoPrimary else TokyoTextPrimary
                                    )
                                }
                            }
                        }

                        // Custom Subnet Toggle
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = TokyoSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showCustomInput = !showCustomInput }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = null,
                                        tint = TokyoSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Custom",
                                        fontSize = 11.sp,
                                        color = TokyoSecondary
                                    )
                                }
                            }
                        }
                    }

                    if (showCustomInput) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customInputText,
                                onValueChange = {
                                    customInputText = it
                                    viewModel.setTargetSubnetPrefix(it)
                                },
                                placeholder = { Text("e.g. 192.168.8", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TokyoPrimary,
                                    unfocusedBorderColor = TokyoOutline,
                                    focusedTextColor = TokyoTextPrimary,
                                    unfocusedTextColor = TokyoTextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = {
                                    viewModel.setTargetSubnetPrefix(customInputText)
                                    showCustomInput = false
                                }
                            ) {
                                Text("Set", color = TokyoPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (isScanning) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { scanProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = TokyoPrimary,
                            trackColor = TokyoSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = scanStatusText,
                            fontSize = 11.sp,
                            color = TokyoTextMuted
                        )
                    }
                }
            }
        }

        // Filter Pills
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("All (${devices.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TokyoPrimaryContainer,
                        selectedLabelColor = TokyoPrimary,
                        containerColor = TokyoSurfaceVariant,
                        labelColor = TokyoTextSecondary
                    ),
                    border = null,
                    shape = RoundedCornerShape(14.dp)
                )

                FilterChip(
                    selected = selectedFilter == DeviceType.LINUX_SERVER || selectedFilter == DeviceType.GATEWAY,
                    onClick = {
                        selectedFilter = if (selectedFilter == DeviceType.LINUX_SERVER) null else DeviceType.LINUX_SERVER
                    },
                    label = { Text("Servers") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TokyoPrimaryContainer,
                        selectedLabelColor = TokyoPrimary,
                        containerColor = TokyoSurfaceVariant,
                        labelColor = TokyoTextSecondary
                    ),
                    border = null,
                    shape = RoundedCornerShape(14.dp)
                )

                FilterChip(
                    selected = selectedFilter == DeviceType.VIZIO_TV || selectedFilter == DeviceType.ROKU_TV || selectedFilter == DeviceType.ANDROID_TV,
                    onClick = {
                        selectedFilter = if (selectedFilter == DeviceType.VIZIO_TV) null else DeviceType.VIZIO_TV
                    },
                    label = { Text("TVs") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TokyoPrimaryContainer,
                        selectedLabelColor = TokyoPrimary,
                        containerColor = TokyoSurfaceVariant,
                        labelColor = TokyoTextSecondary
                    ),
                    border = null,
                    shape = RoundedCornerShape(14.dp)
                )

                FilterChip(
                    selected = selectedFilter == DeviceType.WINDOWS_PC,
                    onClick = {
                        selectedFilter = if (selectedFilter == DeviceType.WINDOWS_PC) null else DeviceType.WINDOWS_PC
                    },
                    label = { Text("PCs") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TokyoPrimaryContainer,
                        selectedLabelColor = TokyoPrimary,
                        containerColor = TokyoSurfaceVariant,
                        labelColor = TokyoTextSecondary
                    ),
                    border = null,
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }

        // Discovered Devices List
        items(filteredDevices, key = { it.ip }) { device ->
            DeviceCard(
                device = device,
                onInspect = { onInspectDevice(device.ip, device.deviceType) }
            )
        }

        if (devices.isEmpty() && !isScanning) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Targeting $targetSubnetPrefix.0/24 • Tap 'Scan' to discover devices",
                        color = TokyoTextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
