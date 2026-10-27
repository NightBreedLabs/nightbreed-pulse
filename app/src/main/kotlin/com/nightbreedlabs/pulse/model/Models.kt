package com.nightbreedlabs.pulse.model

enum class DeviceType(val label: String) {
    GATEWAY("Router / Gateway"),
    VIZIO_TV("Vizio SmartCast TV"),
    ROKU_TV("Roku TV / Streamer"),
    ANDROID_TV("Android TV / ADB"),
    WINDOWS_PC("Windows PC"),
    LINUX_SERVER("Linux Server / Service"),
    GOOGLE_CAST("Google Cast"),
    GENERIC("Network Device")
}

data class NetworkDevice(
    val ip: String,
    val mac: String? = null,
    val hostname: String? = null,
    val vendor: String? = null,
    val deviceType: DeviceType = DeviceType.GENERIC,
    val openPorts: List<Int> = emptyList(),
    val latencyMs: Long = 0,
    val details: Map<String, String> = emptyMap()
)

enum class TaskStatus { IDLE, RUNNING, SUCCESS, FAILED }

data class DiagnosticTask(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val status: TaskStatus = TaskStatus.IDLE,
    val outputLog: String = "",
    val executionTimeMs: Long = 0
)

data class WifiNetworkInfo(
    val ip: String = "192.168.8.189",
    val subnet: String = "192.168.8.0/24",
    val gateway: String = "192.168.8.1",
    val isConnected: Boolean = true,
    val connectionType: String = "Wi-Fi",
    val tailscaleIp: String? = null
)

data class VizioInfo(
    val ip: String,
    val isOnline: Boolean,
    val powerState: String = "Unknown",
    val currentInput: String = "Unknown",
    val systemInfo: String = ""
)

data class RokuInfo(
    val ip: String,
    val isOnline: Boolean,
    val friendlyName: String = "Unknown",
    val modelName: String = "Unknown",
    val appsList: List<String> = emptyList()
)

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
