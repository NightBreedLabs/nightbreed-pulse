package com.nightbreedlabs.pulse.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

object InfrastructureDiagnostics {

    suspend fun benchmarkGateway(gatewayIp: String): String = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        var reachable = false
        try {
            val addr = InetAddress.getByName(gatewayIp)
            reachable = addr.isReachable(1000)
        } catch (_: Exception) {}

        if (!reachable) {
            // Try socket port 80/53
            for (p in intArrayOf(80, 53, 443)) {
                var s: Socket? = null
                try {
                    s = Socket()
                    s.connect(InetSocketAddress(gatewayIp, p), 500)
                    reachable = true
                    break
                } catch (_: Exception) {}
                finally { try { s?.close() } catch (_: Exception) {} }
            }
        }
        val latency = System.currentTimeMillis() - start
        if (reachable) "Gateway ($gatewayIp) is healthy. Latency: ${latency}ms"
        else "Gateway ($gatewayIp) is unreachable or filtering ICMP."
    }

    suspend fun benchmarkDns(): String = withContext(Dispatchers.IO) {
        val domain = "google.com"
        val start = System.currentTimeMillis()
        var resolvedIp = "Failed"
        try {
            val addr = InetAddress.getByName(domain)
            resolvedIp = addr.hostAddress ?: "Unknown"
        } catch (_: Exception) {}
        val time = System.currentTimeMillis() - start
        "DNS Resolved $domain -> $resolvedIp in ${time}ms"
    }

    suspend fun getPublicWanIp(): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.ipify.org")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 2500
            conn.readTimeout = 2500
            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader().use { it.readText().trim() }
            } else {
                "HTTP ${conn.responseCode}"
            }
        } catch (e: Exception) {
            "Unavailable (${e.message ?: "timeout"})"
        }
    }

    suspend fun checkTermuxBridge(): Boolean = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.connect(InetSocketAddress("127.0.0.1", 8022), 500)
            true
        } catch (_: Exception) {
            false
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }
}
