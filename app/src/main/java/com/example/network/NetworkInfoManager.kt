package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

data class NetworkStatus(
    val isWifiEnabled: Boolean = false,
    val isWifiConnected: Boolean = false,
    val wifiSsid: String? = null,
    val wifiRssi: Int = 0,
    val wifiLinkSpeed: Int = 0,
    val isCellularConnected: Boolean = false,
    val isVpnConnected: Boolean = false,
    val activeTransportName: String = "None",
    val isInternetReachable: Boolean = false,
    val isProbing: Boolean = false,
    val lastProbeResult: String = "Not tested"
)

class NetworkInfoManager(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private val _status = MutableStateFlow(NetworkStatus())
    val status: StateFlow<NetworkStatus> = _status.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun startListening() {
        refreshStatus()

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                refreshStatus()
            }

            override fun onLost(network: Network) {
                refreshStatus()
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                refreshStatus()
            }
        }

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            // fallback
        }
    }

    fun stopListening() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                // ignore
            }
            networkCallback = null
        }
    }

    fun refreshStatus() {
        val isWifiRadioOn = wifiManager?.isWifiEnabled ?: false
        val activeNetwork = connectivityManager.activeNetwork
        val caps = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }

        val hasWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val hasCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val hasVpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true

        var ssid: String? = null
        var rssi = 0
        var speed = 0

        if (hasWifi && wifiManager != null) {
            @Suppress("DEPRECATION")
            val info: WifiInfo? = wifiManager.connectionInfo
            info?.let {
                val rawSsid = it.ssid
                ssid = if (rawSsid != null && rawSsid != "<unknown ssid>" && rawSsid != "\"\"") {
                    rawSsid.removeSurrounding("\"")
                } else {
                    "Wi-Fi Active"
                }
                rssi = it.rssi
                speed = it.linkSpeed
            }
        }

        val transportName = when {
            hasVpn -> "VPN Loopback Tunnel"
            hasWifi -> "Wi-Fi ($ssid)"
            hasCellular -> "Cellular Mobile Data"
            activeNetwork != null -> "Other Interface"
            else -> "Disconnected"
        }

        val hasInternetCapability = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        _status.value = _status.value.copy(
            isWifiEnabled = isWifiRadioOn,
            isWifiConnected = hasWifi,
            wifiSsid = ssid,
            wifiRssi = rssi,
            wifiLinkSpeed = speed,
            isCellularConnected = hasCellular,
            isVpnConnected = hasVpn,
            activeTransportName = transportName,
            isInternetReachable = hasInternetCapability
        )
    }

    suspend fun performRealConnectivityProbe(): Boolean = withContext(Dispatchers.IO) {
        _status.value = _status.value.copy(isProbing = true, lastProbeResult = "Probing external network...")
        try {
            // Test 1: Direct TCP socket probe with strict timeout
            val socket = Socket()
            val socketAddress = InetSocketAddress("1.1.1.1", 53)
            socket.connect(socketAddress, 1500)
            socket.close()

            // Test 2: HTTP 204 request
            val url = URL("https://connectivitycheck.gstatic.com/generate_204")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            conn.requestMethod = "HEAD"
            val responseCode = conn.responseCode
            conn.disconnect()

            val reachable = responseCode in 200..399
            _status.value = _status.value.copy(
                isProbing = false,
                isInternetReachable = reachable,
                lastProbeResult = if (reachable) "Reachable (HTTP $responseCode)" else "Failed (HTTP $responseCode)"
            )
            reachable
        } catch (e: Exception) {
            _status.value = _status.value.copy(
                isProbing = false,
                isInternetReachable = false,
                lastProbeResult = "Blocked / Unreachable: ${e.javaClass.simpleName}"
            )
            false
        }
    }
}
