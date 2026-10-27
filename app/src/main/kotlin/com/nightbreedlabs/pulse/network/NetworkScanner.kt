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

    private val KEY_PORTS = intArrayOf(80, 443, 445, 3389, 5555, 7345, 8008, 8060, 9000)

    fun getLocalWifiInfo(): WifiNetworkInfo {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isUp && !iface.isLoopback && !iface.name.contains("tun")) {
                    val addresses = iface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val addr = addresses.nextElement()
                        val host = addr.hostAddress ?: ""
                        if (host.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+")) && !host.startsWith("127.")) {
                            val parts = host.split(".")
                            val subnet = "${parts[0]}.${parts[1]}.${parts[2]}.0/24"
                            val gateway = "${parts[0]}.${parts[1]}.${parts[2]}.1"
                            return WifiNetworkInfo(
                                ip = host,
                                subnet = subnet,
                                gateway = gateway,
                                isConnected = true
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return WifiNetworkInfo()
    }

    suspend fun scanSubnet(
        subnetPrefix: String,
        gatewayIp: String,
        onProgress: (Float, String) -> Unit
    ): List<NetworkDevice> = withContext(Dispatchers.IO) {
        val discovered = mutableListOf<NetworkDevice>()
        val semaphore = Semaphore(28) // concurrency limit to prevent sluggishness
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
                        if (completed % 15 == 0 || completed == totalHosts) {
                            onProgress(completed.toFloat() / totalHosts, "Scanned $completed of $totalHosts hosts ($ip)")
                        }
                    }
                }
            }
            tasks.awaitAll()
        }

        discovered.sortedWith(compareBy({ it.deviceType != DeviceType.GATEWAY }, { it.ip }))
    }

    private fun probeHost(ip: String, isGateway: Boolean): NetworkDevice? {
        val openPorts = mutableListOf<Int>()
        var minLatency = Long.MAX_VALUE

        // Quick check across diagnostic ports
        for (port in KEY_PORTS) {
            val start = System.currentTimeMillis()
            var socket: Socket? = null
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(ip, port), 180)
                val latency = System.currentTimeMillis() - start
                openPorts.add(port)
                if (latency < minLatency) minLatency = latency
            } catch (_: Exception) {
                // Closed or filtered
            } finally {
                try {
                    socket?.close()
                } catch (_: Exception) {}
            }
        }

        // If no ports responded, try reachability ping
        if (openPorts.isEmpty()) {
            try {
                val start = System.currentTimeMillis()
                val reachable = InetAddress.getByName(ip).isReachable(200)
                if (reachable) {
                    minLatency = System.currentTimeMillis() - start
                } else if (!isGateway) {
                    return null
                }
            } catch (_: Exception) {
                if (!isGateway) return null
            }
        }

        if (minLatency == Long.MAX_VALUE) minLatency = 5

        val deviceType = when {
            isGateway -> DeviceType.GATEWAY
            openPorts.contains(7345) || openPorts.contains(9000) -> DeviceType.VIZIO_TV
            openPorts.contains(8060) -> DeviceType.ROKU_TV
            openPorts.contains(5555) -> DeviceType.ANDROID_TV
            openPorts.contains(445) || openPorts.contains(3389) -> DeviceType.WINDOWS_PC
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
