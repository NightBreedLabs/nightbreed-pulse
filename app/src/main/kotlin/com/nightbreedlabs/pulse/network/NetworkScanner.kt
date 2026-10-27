package com.nightbreedlabs.pulse.network

import com.nightbreedlabs.pulse.model.DeviceType
import com.nightbreedlabs.pulse.model.NetworkDevice
import com.nightbreedlabs.pulse.model.WifiNetworkInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

object NetworkScanner {

    private val KEY_PORTS = intArrayOf(
        22,    // SSH (Bazzite, ZimaOS, Router, NucBoxG9)
        53,    // DNS (AdGuard Home / Router)
        80,    // HTTP Web
        443,   // HTTPS Web
        445,   // Windows SMB / Samba
        3002,  // Gitea
        3389,  // Windows RDP
        5555,  // Android TV ADB
        7345,  // Vizio SmartCast
        8008,  // Google Cast
        8060,  // Roku ECP
        8080,  // Proxy / Web Services
        8096,  // Emby
        8097,  // Jellyfin
        9000,  // Vizio / Portainer
        61208  // Glances Monitoring
    )

    // Known Tailscale mesh nodes
    val TAILNET_PEERS = listOf(
        Pair("100.75.95.112", "bazzite-jose (Workstation)"),
        Pair("100.85.75.107", "gl-be6500 (Home Router / Gateway)"),
        Pair("100.122.36.77", "zimaos (Main Server / Gitea)"),
        Pair("100.85.95.17", "nucboxg9 (Media Server)"),
        Pair("100.111.53.42", "google-tv-living-room"),
        Pair("100.119.120.79", "google-bedroom-tv")
    )

    fun getLocalWifiInfo(): WifiNetworkInfo {
        var wifiIp: String? = null
        var tailscaleIp: String? = null
        var cellularIp: String? = null

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (!iface.isUp || iface.isLoopback) continue

                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    val host = addr.hostAddress ?: ""
                    if (!host.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+")) || host.startsWith("127.")) continue

                    val name = iface.name.lowercase()
                    when {
                        name.startsWith("wlan") || name.startsWith("wifi") || name.contains("wlan") -> {
                            wifiIp = host
                        }
                        name.contains("tun") || name.contains("tailscale") || host.startsWith("100.") -> {
                            tailscaleIp = host
                        }
                        name.startsWith("rmnet") || name.startsWith("wwan") || name.startsWith("ccmni") -> {
                            cellularIp = host
                        }
                        else -> {
                            if (wifiIp == null && !host.startsWith("100.")) {
                                wifiIp = host
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return when {
            // Case 1: Local Wi-Fi is active
            wifiIp != null -> {
                val parts = wifiIp.split(".")
                val subnet = "${parts[0]}.${parts[1]}.${parts[2]}.0/24"
                val gateway = "${parts[0]}.${parts[1]}.${parts[2]}.1"
                WifiNetworkInfo(
                    ip = wifiIp,
                    subnet = subnet,
                    gateway = gateway,
                    isConnected = true,
                    connectionType = if (tailscaleIp != null) "Wi-Fi + Tailscale" else "Local Wi-Fi",
                    tailscaleIp = tailscaleIp
                )
            }
            // Case 2: Tailscale VPN active over cellular
            tailscaleIp != null -> {
                WifiNetworkInfo(
                    ip = tailscaleIp,
                    subnet = "192.168.8.0/24",
                    gateway = "192.168.8.1",
                    isConnected = true,
                    connectionType = "Tailscale VPN",
                    tailscaleIp = tailscaleIp
                )
            }
            // Case 3: Cellular data only
            cellularIp != null -> {
                WifiNetworkInfo(
                    ip = cellularIp,
                    subnet = "192.168.8.0/24",
                    gateway = "192.168.8.1",
                    isConnected = true,
                    connectionType = "Cellular Data",
                    tailscaleIp = null
                )
            }
            else -> WifiNetworkInfo(
                ip = "127.0.0.1",
                subnet = "192.168.8.0/24",
                gateway = "192.168.8.1",
                isConnected = false,
                connectionType = "Offline"
            )
        }
    }

    suspend fun scanSubnet(
        subnetPrefix: String,
        gatewayIp: String,
        onProgress: (Float, String) -> Unit
    ): List<NetworkDevice> = withContext(Dispatchers.IO) {
        val discovered = mutableListOf<NetworkDevice>()
        val semaphore = Semaphore(32) // Concurrency throttle
        val totalHosts = 254
        var completed = 0

        coroutineScope {
            val tasks = (1..totalHosts).map { hostNum ->
                val ip = "$subnetPrefix.$hostNum"
                async {
                    semaphore.withPermit {
                        val device = probeHost(ip, isGateway = (ip == gatewayIp))
                        if (device != null) {
                            synchronized(discovered) {
                                discovered.add(device)
                            }
                        }
                        completed++
                        if (completed % 12 == 0 || completed == totalHosts) {
                            onProgress(completed.toFloat() / totalHosts, "Scanned $completed of $totalHosts hosts ($ip)")
                        }
                    }
                }
            }
            tasks.awaitAll()
        }

        discovered.sortedWith(compareBy({ it.deviceType != DeviceType.GATEWAY }, { it.ip }))
    }

    suspend fun scanTailnetPeers(
        onProgress: (Float, String) -> Unit
    ): List<NetworkDevice> = withContext(Dispatchers.IO) {
        val discovered = mutableListOf<NetworkDevice>()
        val total = TAILNET_PEERS.size
        var completed = 0

        coroutineScope {
            val tasks = TAILNET_PEERS.map { (ip, label) ->
                async {
                    val device = probeHost(ip, isGateway = ip.endsWith(".107"))
                    if (device != null) {
                        synchronized(discovered) {
                            discovered.add(device.copy(hostname = label))
                        }
                    }
                    completed++
                    onProgress(completed.toFloat() / total, "Checking $label ($ip)...")
                }
            }
            tasks.awaitAll()
        }

        discovered.sortedBy { it.ip }
    }

    private fun probeHost(ip: String, isGateway: Boolean): NetworkDevice? {
        val openPorts = mutableListOf<Int>()
        var minLatency = Long.MAX_VALUE

        // Port scanning across key diagnostic ports (280ms timeout for cellular latency)
        for (port in KEY_PORTS) {
            val start = System.currentTimeMillis()
            var socket: Socket? = null
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(ip, port), 280)
                val latency = System.currentTimeMillis() - start
                openPorts.add(port)
                if (latency < minLatency) minLatency = latency
            } catch (_: Exception) {
            } finally {
                try { socket?.close() } catch (_: Exception) {}
            }
        }

        // If no ports responded, check reachable ping
        if (openPorts.isEmpty()) {
            try {
                val start = System.currentTimeMillis()
                val reachable = InetAddress.getByName(ip).isReachable(300)
                if (reachable) {
                    minLatency = System.currentTimeMillis() - start
                } else if (!isGateway) {
                    return null
                }
            } catch (_: Exception) {
                if (!isGateway) return null
            }
        }

        if (minLatency == Long.MAX_VALUE) minLatency = 12

        val deviceType = when {
            isGateway || ip.endsWith(".1") || ip.endsWith(".107") -> DeviceType.GATEWAY
            openPorts.contains(7345) || openPorts.contains(9000) -> DeviceType.VIZIO_TV
            openPorts.contains(8060) -> DeviceType.ROKU_TV
            openPorts.contains(5555) -> DeviceType.ANDROID_TV
            openPorts.contains(445) || openPorts.contains(3389) -> DeviceType.WINDOWS_PC
            openPorts.contains(22) || openPorts.contains(3002) || openPorts.contains(8096) || openPorts.contains(8097) || openPorts.contains(61208) -> DeviceType.LINUX_SERVER
            openPorts.contains(8008) || openPorts.contains(8009) -> DeviceType.GOOGLE_CAST
            else -> DeviceType.GENERIC
        }

        return NetworkDevice(
            ip = ip,
            deviceType = deviceType,
            openPorts = openPorts,
            latencyMs = minLatency,
            hostname = try { InetAddress.getByName(ip).canonicalHostName } catch (_: Exception) { null }
        )
    }
}
