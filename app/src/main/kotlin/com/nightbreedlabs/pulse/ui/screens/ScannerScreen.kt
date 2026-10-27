package com.nightbreedlabs.pulse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
                        Column {
                            Text(
                                text = "Subnet Discovery",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TokyoTextPrimary
                            )
                            Text(
                                text = "${wifiInfo.subnet} • ${devices.size} Discovered",
                                fontSize = 13.sp,
                                color = TokyoTextSecondary
                            )
                        }

                        Button(
                            onClick = {
                                if (isScanning) viewModel.cancelScan()
                                else viewModel.startSubnetScan()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isScanning) TokyoError else TokyoPrimary,
                                contentColor = TokyoOnPrimary
                            ),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
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
                    selected = selectedFilter == DeviceType.VIZIO_TV || selectedFilter == DeviceType.ROKU_TV,
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
                        text = "Tap 'Scan' to discover all active devices on ${wifiInfo.subnet}",
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
