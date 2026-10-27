package com.nightbreedlabs.pulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightbreedlabs.pulse.model.DeviceType
import com.nightbreedlabs.pulse.model.NetworkDevice
import com.nightbreedlabs.pulse.ui.theme.*

@Composable
fun DeviceCard(
    device: NetworkDevice,
    onInspect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (typeColor, typeIcon) = when (device.deviceType) {
        DeviceType.GATEWAY -> Pair(TokyoSuccess, Icons.Rounded.Router)
        DeviceType.VIZIO_TV -> Pair(TokyoWarning, Icons.Rounded.Tv)
        DeviceType.ROKU_TV -> Pair(TokyoSecondary, Icons.Rounded.Tv)
        DeviceType.ANDROID_TV -> Pair(TokyoCyan, Icons.Rounded.SmartDisplay)
        DeviceType.WINDOWS_PC -> Pair(TokyoPrimary, Icons.Rounded.Computer)
        DeviceType.LINUX_SERVER -> Pair(TokyoCyan, Icons.Rounded.Dns)
        DeviceType.GOOGLE_CAST -> Pair(TokyoTertiary, Icons.Rounded.Cast)
        DeviceType.GENERIC -> Pair(TokyoTextSecondary, Icons.Rounded.Lan)
    }

    Surface(
        modifier = modifier
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(typeColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = device.ip,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TokyoTextPrimary
                        )
                        Text(
                            text = device.deviceType.label,
                            fontSize = 12.sp,
                            color = typeColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Latency Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TokyoSurfaceVariant
                ) {
                    Text(
                        text = "${device.latencyMs} ms",
                        fontSize = 11.sp,
                        color = TokyoTextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (device.openPorts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    device.openPorts.take(5).forEach { port ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TokyoSurfaceVariant
                        ) {
                            Text(
                                text = "Port $port",
                                fontSize = 10.sp,
                                color = TokyoTextMuted,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(
                    onClick = onInspect,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = TokyoPrimaryContainer,
                        contentColor = TokyoPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Inspect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
