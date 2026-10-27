package com.nightbreedlabs.pulse.network

import com.nightbreedlabs.pulse.model.RokuInfo
import com.nightbreedlabs.pulse.model.VizioInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object SmartTvManager {

    private val unsafeSslContext: SSLContext by lazy {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
        })
        SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, SecureRandom())
        }
    }

    suspend fun queryVizio(ip: String): VizioInfo = withContext(Dispatchers.IO) {
        var inputRes = "Unknown"
        var sysInfoRes = ""
        var isOnline = false

        try {
            val url = URL("https://$ip:7345/menu_native/dynamic/tv_settings/devices/current_input")
            val conn = url.openConnection() as HttpsURLConnection
            conn.sslSocketFactory = unsafeSslContext.socketFactory
            conn.hostnameVerifier = javax.net.ssl.HostnameVerifier { _, _ -> true }
            conn.connectTimeout = 2500
            conn.readTimeout = 2500

            if (conn.responseCode == 200) {
                isOnline = true
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                inputRes = reader.readText()
                reader.close()
            }
            conn.disconnect()
        } catch (_: Exception) {
            // Port 9000 fallback test
            var s: Socket? = null
            try {
                s = Socket()
                s.connect(InetSocketAddress(ip, 9000), 1000)
                isOnline = true
            } catch (_: Exception) {}
            finally { try { s?.close() } catch (_: Exception) {} }
        }

        try {
            val url = URL("https://$ip:7345/menu_native/dynamic/tv_settings/settings/system/information")
            val conn = url.openConnection() as HttpsURLConnection
            conn.sslSocketFactory = unsafeSslContext.socketFactory
            conn.hostnameVerifier = javax.net.ssl.HostnameVerifier { _, _ -> true }
            conn.connectTimeout = 2500
            conn.readTimeout = 2500
            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                sysInfoRes = reader.readText()
                reader.close()
            }
            conn.disconnect()
        } catch (_: Exception) {}

        VizioInfo(
            ip = ip,
            isOnline = isOnline,
            currentInput = inputRes,
            systemInfo = sysInfoRes
        )
    }

    suspend fun queryRoku(ip: String): RokuInfo = withContext(Dispatchers.IO) {
        var friendlyName = "Roku TV"
        var modelName = "Unknown"
        var isOnline = false
        val apps = mutableListOf<String>()

        try {
            val url = URL("http://$ip:8060/query/device-info")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            if (conn.responseCode == 200) {
                isOnline = true
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                Regex("<friendly-device-name>(.*?)</friendly-device-name>").find(text)?.let {
                    friendlyName = it.groupValues[1]
                }
                Regex("<model-name>(.*?)</model-name>").find(text)?.let {
                    modelName = it.groupValues[1]
                }
            }
            conn.disconnect()
        } catch (_: Exception) {}

        try {
            val url = URL("http://$ip:8060/query/apps")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                Regex("<app id=\"[^\"]*\"[^>]*>(.*?)</app>").findAll(text).forEach {
                    apps.add(it.groupValues[1])
                }
            }
            conn.disconnect()
        } catch (_: Exception) {}

        RokuInfo(
            ip = ip,
            isOnline = isOnline,
            friendlyName = friendlyName,
            modelName = modelName,
            appsList = apps
        )
    }

    suspend fun sendRokuKey(ip: String, key: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$ip:8060/keypress/$key")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 2000
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (_: Exception) {
            false
        }
    }
}
