package com.nightbreedlabs.pulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nightbreedlabs.pulse.model.WifiNetworkInfo
import com.nightbreedlabs.pulse.ui.theme.*

@Composable
fun HeaderBar(
    wifiInfo: WifiNetworkInfo,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = TokyoBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // NightBreedLabs Branding
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(TokyoCyan, TokyoPrimary)
                                )
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NightBreedLabs",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TokyoTextSecondary,
                        letterSpacing = 1.sp
                    )
                }

                // Connection Status Pill
                val isTailscale = wifiInfo.connectionType.contains("Tailscale")
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = TokyoSurfaceVariant,
                    modifier = Modifier.border(1.dp, TokyoOutlineVariant, RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isTailscale) Icons.Rounded.VpnKey else Icons.Rounded.Wifi,
                            contentDescription = if (isTailscale) "Tailscale" else "Wi-Fi",
                            tint = if (isTailscale) TokyoCyan else TokyoPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isTailscale) (wifiInfo.tailscaleIp ?: wifiInfo.ip) else wifiInfo.ip,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TokyoTextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // App Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pulse Network Hub",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TokyoTextPrimary
                    )
                    Text(
                        text = "${wifiInfo.connectionType} • Subnet: ${wifiInfo.subnet}",
                        fontSize = 12.sp,
                        color = TokyoTextMuted
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(TokyoSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh",
                        tint = TokyoPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
