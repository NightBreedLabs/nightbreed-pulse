package com.nightbreedlabs.pulse.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nightbreedlabs.pulse.model.DeviceType
import com.nightbreedlabs.pulse.ui.components.HeaderBar
import com.nightbreedlabs.pulse.ui.screens.AutomationsScreen
import com.nightbreedlabs.pulse.ui.screens.GeminiScreen
import com.nightbreedlabs.pulse.ui.screens.ScannerScreen
import com.nightbreedlabs.pulse.ui.screens.SmartTvScreen
import com.nightbreedlabs.pulse.ui.theme.*
import com.nightbreedlabs.pulse.viewmodel.PulseViewModel

data class NavTabItem(
    val title: String,
    val icon: ImageVector,
    val index: Int
)

@Composable
fun MainScreen(
    viewModel: PulseViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val wifiInfo by viewModel.wifiInfo.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    val navTabs = listOf(
        NavTabItem("Scanner", Icons.Rounded.Wifi, 0),
        NavTabItem("Tasks", Icons.Rounded.Bolt, 1),
        NavTabItem("Smart TVs", Icons.Rounded.Tv, 2),
        NavTabItem("Gemini AI", Icons.Rounded.Psychology, 3)
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = TokyoBackground,
        topBar = {
            HeaderBar(
                wifiInfo = wifiInfo,
                onRefresh = { viewModel.refreshNetworkInfo() }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = TokyoSurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                navTabs.forEach { tab ->
                    val isSelected = selectedTab == tab.index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.selectTab(tab.index) },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TokyoOnPrimary,
                            selectedTextColor = TokyoPrimary,
                            indicatorColor = TokyoPrimary,
                            unselectedIconColor = TokyoTextMuted,
                            unselectedTextColor = TokyoTextMuted
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(TokyoBackground)
        ) {
            when (selectedTab) {
                0 -> ScannerScreen(
                    viewModel = viewModel,
                    onInspectDevice = { ip, type ->
                        when (type) {
                            DeviceType.VIZIO_TV -> {
                                viewModel.inspectVizio(ip)
                                viewModel.selectTab(2)
                            }
                            DeviceType.ROKU_TV -> {
                                viewModel.inspectRoku(ip)
                                viewModel.selectTab(2)
                            }
                            DeviceType.WINDOWS_PC -> {
                                viewModel.executeTask("task_windows", targetIp = ip)
                                viewModel.selectTab(1)
                            }
                            else -> {
                                viewModel.selectTab(1)
                            }
                        }
                    }
                )
                1 -> AutomationsScreen(viewModel = viewModel)
                2 -> SmartTvScreen(viewModel = viewModel)
                3 -> GeminiScreen(viewModel = viewModel)
            }
        }
    }
}
