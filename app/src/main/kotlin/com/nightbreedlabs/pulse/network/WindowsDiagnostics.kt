package com.nightbreedlabs.pulse.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

object WindowsDiagnostics {

    suspend fun checkWindowsServices(ip: String): Map<String, Boolean> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, Boolean>()
        val ports = mapOf(
            "SMB File Sharing (445)" to 445,
            "Remote Desktop RDP (3389)" to 3389,
            "WinRM Management (5985)" to 5985,
            "NetBIOS Name Service (139)" to 139
        )

        for ((name, port) in ports) {
            var socket: Socket? = null
            try {
                socket = Socket()
                socket.connect(InetSocketAddress(ip, port), 600)
                results[name] = true
            } catch (_: Exception) {
                results[name] = false
            } finally {
                try { socket?.close() } catch (_: Exception) {}
            }
        }
        results
    }

    suspend fun sendWakeOnLan(macAddress: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanMac = macAddress.replace(":", "").replace("-", "")
            if (cleanMac.length != 12) return@withContext false

            val macBytes = ByteArray(6)
            for (i in 0 until 6) {
                macBytes[i] = cleanMac.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }

            val bytes = ByteArray(102)
            for (i in 0 until 6) {
                bytes[i] = 0xFF.toByte()
            }
            for (i in 6 until 102 step 6) {
                System.arraycopy(macBytes, 0, bytes, i, 6)
            }

            val address = InetAddress.getByName("255.255.255.255")
            val packet = DatagramPacket(bytes, bytes.size, address, 9)
            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.send(packet)
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
