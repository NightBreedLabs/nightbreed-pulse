package com.nightbreedlabs.pulse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nightbreedlabs.pulse.model.*
import com.nightbreedlabs.pulse.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PulseViewModel : ViewModel() {

    private val _wifiInfo = MutableStateFlow(WifiNetworkInfo())
    val wifiInfo: StateFlow<WifiNetworkInfo> = _wifiInfo.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<NetworkDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<NetworkDevice>> = _discoveredDevices.asStateFlow()

    private val _targetSubnetPrefix = MutableStateFlow("192.168.8")
    val targetSubnetPrefix: StateFlow<String> = _targetSubnetPrefix.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _scanStatusText = MutableStateFlow("Ready to scan")
    val scanStatusText: StateFlow<String> = _scanStatusText.asStateFlow()

    private val _automatedTasks = MutableStateFlow<List<DiagnosticTask>>(emptyList())
    val automatedTasks: StateFlow<List<DiagnosticTask>> = _automatedTasks.asStateFlow()

    private val _activeVizio = MutableStateFlow<VizioInfo?>(null)
    val activeVizio: StateFlow<VizioInfo?> = _activeVizio.asStateFlow()

    private val _activeRoku = MutableStateFlow<RokuInfo?>(null)
    val activeRoku: StateFlow<RokuInfo?> = _activeRoku.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _currentAiModel = MutableStateFlow(GeminiDiagnostics.selectedModel)
    val currentAiModel: StateFlow<String> = _currentAiModel.asStateFlow()

    private val _currentThinkingLevel = MutableStateFlow(GeminiDiagnostics.thinkingLevel)
    val currentThinkingLevel: StateFlow<String> = _currentThinkingLevel.asStateFlow()

    private val _availableAiModels = MutableStateFlow<List<String>>(emptyList())
    val availableAiModels: StateFlow<List<String>> = _availableAiModels.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private var scanJob: Job? = null

    init {
        refreshNetworkInfo()
        initializeDefaultTasks()
        _chatMessages.value = listOf(
            ChatMessage(
                id = "init",
                isUser = false,
                text = "Welcome to NightBreed Pulse. I am your local network diagnostic assistant. Ask me to analyze scan results, inspect smart TVs, or troubleshoot PC reachability."
            )
        )
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun setTargetSubnetPrefix(prefix: String) {
        val clean = prefix.trim().removeSuffix(".")
        _targetSubnetPrefix.value = clean
    }

    fun refreshNetworkInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            val info = NetworkScanner.getLocalWifiInfo()
            _wifiInfo.value = info
            if (info.connectionType.contains("Wi-Fi")) {
                val parts = info.ip.split(".")
                if (parts.size >= 3) {
                    _targetSubnetPrefix.value = "${parts[0]}.${parts[1]}.${parts[2]}"
                }
            } else {
                _targetSubnetPrefix.value = "192.168.8"
            }
        }
    }

    fun startSubnetScan(customPrefix: String? = null) {
        if (_isScanning.value) return
        _isScanning.value = true
        _scanProgress.value = 0f

        val prefixToScan = (customPrefix ?: _targetSubnetPrefix.value).trim().removeSuffix(".")
        _scanStatusText.value = "Starting discovery on $prefixToScan.0/24..."

        scanJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val gateway = if (prefixToScan == "192.168.8") "192.168.8.1" else "$prefixToScan.1"

                val results = NetworkScanner.scanSubnet(
                    subnetPrefix = prefixToScan,
                    gatewayIp = gateway,
                    onProgress = { progress, status ->
                        _scanProgress.value = progress
                        _scanStatusText.value = status
                    }
                )
                _discoveredDevices.value = results
                _scanStatusText.value = "Scan complete: Found ${results.size} devices on $prefixToScan.0/24"
            } catch (e: Exception) {
                _scanStatusText.value = "Scan interrupted: ${e.message}"
            } finally {
                _isScanning.value = false
                _scanProgress.value = 1f
                System.gc()
            }
        }
    }

    fun startTailnetScan() {
        if (_isScanning.value) return
        _isScanning.value = true
        _scanProgress.value = 0f
        _scanStatusText.value = "Scanning Tailscale mesh nodes..."

        scanJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val results = NetworkScanner.scanTailnetPeers { progress, status ->
                    _scanProgress.value = progress
                    _scanStatusText.value = status
                }
                _discoveredDevices.value = results
                _scanStatusText.value = "Tailnet scan complete: Found ${results.size} active nodes"
            } catch (e: Exception) {
                _scanStatusText.value = "Tailnet scan interrupted: ${e.message}"
            } finally {
                _isScanning.value = false
                _scanProgress.value = 1f
                System.gc()
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _isScanning.value = false
        _scanStatusText.value = "Scan stopped by user"
    }

    private fun initializeDefaultTasks() {
        _automatedTasks.value = listOf(
            DiagnosticTask(
                id = "task_gateway",
                title = "Gateway & Routing Latency",
                category = "Infrastructure",
                description = "Measures round-trip ICMP & socket latency to the local router gateway."
            ),
            DiagnosticTask(
                id = "task_dns",
                title = "DNS Resolution Benchmark",
                category = "Infrastructure",
                description = "Tests lookup speed and resolution accuracy against Google & Cloudflare."
            ),
            DiagnosticTask(
                id = "task_wan",
                title = "Public WAN IP & Geolocation",
                category = "Infrastructure",
                description = "Queries external egress IP to detect VPN leaks or ISP gateway changes."
            ),
            DiagnosticTask(
                id = "task_termux",
                title = "Termux Bridge Service Check",
                category = "Termux Bridge",
                description = "Verifies if the background Termux SSH daemon is active on port 8022."
            ),
            DiagnosticTask(
                id = "task_vizio",
                title = "SmartCast TV Diagnostics",
                category = "Smart TVs",
                description = "Queries Vizio API ports 7345/9000 for power state and active HDMI input."
            ),
            DiagnosticTask(
                id = "task_roku",
                title = "Roku TV ECP Diagnostics",
                category = "Smart TVs",
                description = "Fetches device info, Wi-Fi MAC, and lists all active & installed apps."
            ),
            DiagnosticTask(
                id = "task_windows",
                title = "Windows PC File Sharing & RDP",
                category = "Windows Diagnostics",
                description = "Checks SMB (445), Remote Desktop (3389), and WinRM (5985) ports."
            )
        )
    }

    fun executeTask(taskId: String, targetIp: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            updateTaskStatus(taskId, TaskStatus.RUNNING, "Executing diagnostic...")
            val start = System.currentTimeMillis()

            try {
                val output = when (taskId) {
                    "task_gateway" -> InfrastructureDiagnostics.benchmarkGateway(_wifiInfo.value.gateway)
                    "task_dns" -> InfrastructureDiagnostics.benchmarkDns()
                    "task_wan" -> "Public WAN IP: " + InfrastructureDiagnostics.getPublicWanIp()
                    "task_termux" -> {
                        val ok = InfrastructureDiagnostics.checkTermuxBridge()
                        if (ok) "Termux SSH bridge (127.0.0.1:8022) is ONLINE and responding."
                        else "Termux bridge is OFFLINE. Start it in Termux using 'sshd'."
                    }
                    "task_vizio" -> {
                        val ip = targetIp.ifBlank { findFirstDeviceOf(DeviceType.VIZIO_TV) }
                        if (ip.isBlank()) "No Vizio TV discovered yet. Run a network scan first."
                        else {
                            val info = SmartTvManager.queryVizio(ip)
                            _activeVizio.value = info
                            "Vizio TV at $ip: Online=${info.isOnline}, Input=${info.currentInput}"
                        }
                    }
                    "task_roku" -> {
                        val ip = targetIp.ifBlank { findFirstDeviceOf(DeviceType.ROKU_TV) }
                        if (ip.isBlank()) "No Roku TV discovered yet. Run a network scan first."
                        else {
                            val info = SmartTvManager.queryRoku(ip)
                            _activeRoku.value = info
                            "Roku (${info.friendlyName} - ${info.modelName}): Apps=${info.appsList.size} installed"
                        }
                    }
                    "task_windows" -> {
                        val ip = targetIp.ifBlank { findFirstDeviceOf(DeviceType.WINDOWS_PC) }
                        if (ip.isBlank()) "No Windows PC discovered yet. Run a network scan first."
                        else {
                            val ports = WindowsDiagnostics.checkWindowsServices(ip)
                            ports.entries.joinToString("\n") { (k, v) -> " - $k: ${if (v) "OPEN" else "CLOSED"}" }
                        }
                    }
                    else -> "Unknown task"
                }

                val duration = System.currentTimeMillis() - start
                updateTaskStatus(taskId, TaskStatus.SUCCESS, output, duration)
            } catch (e: Exception) {
                updateTaskStatus(taskId, TaskStatus.FAILED, "Error: ${e.message}", System.currentTimeMillis() - start)
            } finally {
                // Immediate resource release
                System.gc()
            }
        }
    }

    private fun findFirstDeviceOf(type: DeviceType): String {
        return _discoveredDevices.value.firstOrNull { it.deviceType == type }?.ip ?: ""
    }

    private fun updateTaskStatus(taskId: String, status: TaskStatus, log: String, duration: Long = 0) {
        _automatedTasks.value = _automatedTasks.value.map {
            if (it.id == taskId) {
                it.copy(status = status, outputLog = log, executionTimeMs = duration)
            } else it
        }
    }

    fun inspectVizio(ip: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = SmartTvManager.queryVizio(ip)
            _activeVizio.value = res
        }
    }

    fun inspectRoku(ip: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = SmartTvManager.queryRoku(ip)
            _activeRoku.value = res
        }
    }

    fun sendRokuKey(ip: String, key: String) {
        viewModelScope.launch(Dispatchers.IO) {
            SmartTvManager.sendRokuKey(ip, key)
            inspectRoku(ip)
        }
    }

    fun sendWakeOnLan(mac: String) {
        viewModelScope.launch(Dispatchers.IO) {
            WindowsDiagnostics.sendWakeOnLan(mac)
        }
    }

    fun sendAiPrompt(prompt: String) {
        val userMsg = ChatMessage(id = System.currentTimeMillis().toString(), isUser = true, text = prompt)
        _chatMessages.value = _chatMessages.value + userMsg
        _isAiThinking.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Build diagnostic context
                val deviceContext = _discoveredDevices.value.joinToString("\n") { dev ->
                    "${dev.ip} (${dev.deviceType.label}, open ports: ${dev.openPorts})"
                }
                val fullPrompt = """
                    You are NightBreed Pulse, an expert network diagnostic AI.
                    Current Network Context:
                    Subnet: ${_wifiInfo.value.subnet}
                    Gateway: ${_wifiInfo.value.gateway}
                    Active Devices:
                    $deviceContext

                    User Query:
                    $prompt

                    Provide a concise, bullet-pointed diagnostic analysis and recommended actions.
                """.trimIndent()

                val response = GeminiDiagnostics.askGemini(fullPrompt)
                _currentAiModel.value = GeminiDiagnostics.selectedModel
                val aiMsg = ChatMessage(id = (System.currentTimeMillis() + 1).toString(), isUser = false, text = response)
                _chatMessages.value = _chatMessages.value + aiMsg
            } catch (e: Exception) {
                val errorMsg = ChatMessage(id = (System.currentTimeMillis() + 1).toString(), isUser = false, text = "Failed to query Gemini: ${e.message}")
                _chatMessages.value = _chatMessages.value + errorMsg
            } finally {
                _isAiThinking.value = false
            }
        }
    }

    fun refreshAiModels(apiKey: String = GeminiDiagnostics.apiKey) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = GeminiDiagnostics.fetchAvailableModels(apiKey)
            _availableAiModels.value = list
            _currentAiModel.value = GeminiDiagnostics.selectedModel
        }
    }

    fun selectAiModel(model: String) {
        GeminiDiagnostics.selectedModel = model
        _currentAiModel.value = model
    }

    fun selectThinkingLevel(level: String) {
        GeminiDiagnostics.thinkingLevel = level
        _currentThinkingLevel.value = level
    }
}
