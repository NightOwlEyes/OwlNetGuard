package com.nightowleyes.owlnetguard.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NetworkStatus(
    val isWifiEnabled: Boolean = false,
    val isWifiConnected: Boolean = false,
    val isCellularConnected: Boolean = false,
    val isAirplaneModeOn: Boolean = false
)

object NetworkSettingsHelper {

    private val _status = MutableStateFlow(NetworkStatus())
    val status: StateFlow<NetworkStatus> = _status.asStateFlow()

    private var isRegistered = false

    fun init(context: Context) {
        if (isRegistered) return
        val appCtx = context.applicationContext
        isRegistered = true

        updateStatus(appCtx)

        // Safe register of ConnectivityManager callbacks
        try {
            val cm = appCtx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                // 1. Default network callback (fires on network switches/losses)
                cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) { updateStatus(appCtx) }
                    override fun onLost(network: Network) { updateStatus(appCtx) }
                    override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { updateStatus(appCtx) }
                })

                // 2. Comprehensive network callback for all physical network interfaces
                val request = NetworkRequest.Builder().build()
                cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) { updateStatus(appCtx) }
                    override fun onLost(network: Network) { updateStatus(appCtx) }
                    override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { updateStatus(appCtx) }
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Safe register broadcast receiver for Wi-Fi and Airplane mode state changes
        try {
            val filter = IntentFilter().apply {
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
                addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
                addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                @Suppress("DEPRECATION")
                addAction(ConnectivityManager.CONNECTIVITY_ACTION)
                addAction("android.intent.action.ANY_DATA_STATE")
                addAction("android.telephony.action.CARRIER_SIGNAL_DEFAULT_NETWORK_AVAILABLE")
                addAction("android.telephony.action.DATA_ENABLED_CHANGED")
            }
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    if (c != null) {
                        updateStatus(c.applicationContext)
                    }
                }
            }
            ContextCompat.registerReceiver(
                appCtx,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Register ContentObserver for immediate Mobile Data / Wi-Fi / Airplane toggle detection
        try {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    updateStatus(appCtx)
                }
            }
            appCtx.contentResolver.registerContentObserver(
                Settings.Global.getUriFor("mobile_data"),
                true,
                observer
            )
            try {
                appCtx.contentResolver.registerContentObserver(
                    Settings.Global.getUriFor("mobile_data0"),
                    true,
                    observer
                )
                appCtx.contentResolver.registerContentObserver(
                    Settings.Global.getUriFor("mobile_data1"),
                    true,
                    observer
                )
            } catch (_: Exception) {
            }
            appCtx.contentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON),
                false,
                observer
            )
            appCtx.contentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.WIFI_ON),
                false,
                observer
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateStatus(context: Context) {
        val appCtx = context.applicationContext
        var wifiConnected = false
        var cellTransportConnected = false

        val cm = try {
            appCtx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        } catch (_: Exception) { null }

        if (cm != null) {
            try {
                // Query all physical and virtual networks so VPN doesn't mask Wi-Fi / Cell hardware state
                @Suppress("DEPRECATION")
                val allNets = cm.allNetworks
                for (net in allNets) {
                    val caps = cm.getNetworkCapabilities(net) ?: continue
                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        wifiConnected = true
                    }
                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    ) {
                        cellTransportConnected = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val wm = try {
            appCtx.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        } catch (_: Exception) { null }

        val wifiEnabled = try {
            val state = wm?.wifiState
            when (state) {
                WifiManager.WIFI_STATE_ENABLED, WifiManager.WIFI_STATE_ENABLING -> true
                WifiManager.WIFI_STATE_DISABLED, WifiManager.WIFI_STATE_DISABLING -> false
                else -> wm?.isWifiEnabled ?: wifiConnected
            }
        } catch (_: Exception) {
            wifiConnected
        }

        if (!wifiEnabled) {
            wifiConnected = false
        }

        val airplaneModeOn = try {
            Settings.Global.getInt(
                appCtx.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                0
            ) != 0
        } catch (_: Exception) {
            false
        }

        val tm = try {
            appCtx.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        } catch (_: Exception) { null }

        val simAbsent = try {
            val simState = tm?.simState
            simState == TelephonyManager.SIM_STATE_ABSENT || simState == TelephonyManager.SIM_STATE_NOT_READY
        } catch (_: Exception) {
            false
        }

        // 1. Primary check: TelephonyManager.isDataEnabled (API 26+)
        val tmDataEnabled: Boolean? = try {
            tm?.isDataEnabled
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }

        // 2. Query Settings.Global for sub-specific or global mobile_data keys
        val settingsDataEnabled: Boolean? = try {
            val cr = appCtx.contentResolver
            val md = Settings.Global.getInt(cr, "mobile_data", -1)
            val md0 = Settings.Global.getInt(cr, "mobile_data0", -1)
            val md1 = Settings.Global.getInt(cr, "mobile_data1", -1)
            when {
                md == 0 || md0 == 0 || md1 == 0 -> false
                md == 1 || md0 == 1 || md1 == 1 -> true
                else -> null
            }
        } catch (_: Exception) {
            null
        }

        val cellConnected = when {
            airplaneModeOn || simAbsent -> false
            tmDataEnabled != null -> tmDataEnabled
            settingsDataEnabled != null -> settingsDataEnabled
            else -> cellTransportConnected
        }

        _status.value = NetworkStatus(
            isWifiEnabled = wifiEnabled,
            isWifiConnected = wifiConnected,
            isCellularConnected = cellConnected,
            isAirplaneModeOn = airplaneModeOn
        )
    }

    /**
     * Opens official Android Wi-Fi panel or settings overlay (100% Play Store compliant).
     */
    fun openWifiPanel(context: Context) {
        val appCtx = context.applicationContext
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val panelIntent = Intent(Settings.Panel.ACTION_WIFI).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                appCtx.startActivity(panelIntent)
            } else {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                appCtx.startActivity(intent)
            }
        } catch (_: Exception) {
            openGeneralWirelessSettings(appCtx)
        }
    }

    /**
     * Opens official Android Internet Connectivity panel (Wi-Fi + Mobile Data combined bottom sheet).
     */
    fun openDataPanel(context: Context) {
        val appCtx = context.applicationContext
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val panelIntent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                appCtx.startActivity(panelIntent)
            } else {
                val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                appCtx.startActivity(intent)
            }
        } catch (_: Exception) {
            openGeneralWirelessSettings(appCtx)
        }
    }

    /**
     * Opens official Android Airplane mode settings.
     */
    fun openAirplaneSettings(context: Context) {
        val appCtx = context.applicationContext
        try {
            val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            appCtx.startActivity(intent)
        } catch (_: Exception) {
            openGeneralWirelessSettings(appCtx)
        }
    }

    private fun openGeneralWirelessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Không thể mở cài đặt mạng: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
