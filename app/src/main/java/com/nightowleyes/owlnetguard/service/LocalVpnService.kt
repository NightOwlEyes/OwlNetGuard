package com.nightowleyes.owlnetguard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.nightowleyes.owlnetguard.R
import com.nightowleyes.owlnetguard.data.PreferencesManager
import com.nightowleyes.owlnetguard.receiver.NotificationActionReceiver
import com.nightowleyes.owlnetguard.ui.MainActivity
import java.io.FileInputStream
import kotlin.concurrent.thread

class LocalVpnService : VpnService() {

    @Volatile
    private var vpnInterface: ParcelFileDescriptor? = null

    @Volatile
    private var isRunning: Boolean = false

    @Volatile
    private var currentActiveBlockedSet: Set<String> = emptySet()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate() {
        super.onCreate()
        registerNetworkCallback()
        startOrUpdateForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_VPN) {
            stopVpnInternal()
            return START_NOT_STICKY
        }

        if (!shouldRunBackgroundVpn(this)) {
            stopVpnInternal()
            return START_NOT_STICKY
        }

        applyVpnTunnelSynchronized()

        return START_STICKY
    }

    @Synchronized
    private fun applyVpnTunnelSynchronized() {
        if (!shouldRunBackgroundVpn(this)) {
            stopVpnInternal()
            return
        }

        val effectiveBlocked = PreferencesManager.getEffectiveBlockedPackages(this)
            .filterTo(LinkedHashSet()) { it != packageName }
        val allManagedPackages = PreferencesManager.getBubblePackages(this)
            .filterTo(LinkedHashSet()) { it != packageName }
        currentActiveBlockedSet = effectiveBlocked

        startOrUpdateForegroundNotification()

        try {
            val builder = Builder()
                .setSession("OwlNetGuard")
                .setMtu(1500)
                .setBlocking(true)
                .addAddress("10.215.173.1", 30)
                .addAddress("fd00:1:fd00:1:fd00:1:fd00:1", 128)

            var addedCount = 0
            if (effectiveBlocked.isNotEmpty()) {
                for (pkg in effectiveBlocked) {
                    try {
                        builder.addAllowedApplication(pkg)
                        addedCount++
                    } catch (_: Exception) {
                    }
                }
                // Route traffic of isolated apps into the silent sinkhole
                builder.addRoute("0.0.0.0", 1)
                builder.addRoute("128.0.0.0", 1)
                builder.addRoute("::", 1)
                builder.addRoute("8000::", 1)
                builder.addDnsServer("10.215.173.2")
            } else {
                // Standby / Unblocked:
                // Pre-bind all managed apps into the tunnel without adding sinkhole routes.
                // Traffic falls through to the underlying network so the apps retain internet
                // while already registered with the VPN interface, preventing abrupt socket drops.
                if (allManagedPackages.isNotEmpty()) {
                    for (pkg in allManagedPackages) {
                        try {
                            builder.addAllowedApplication(pkg)
                            addedCount++
                        } catch (_: Exception) {
                        }
                    }
                }
                if (addedCount == 0) {
                    try {
                        builder.addAllowedApplication(packageName)
                    } catch (_: Exception) {
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            try {
                val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                val activeNet = cm?.activeNetwork
                if (activeNet != null) {
                    builder.setUnderlyingNetworks(arrayOf(activeNet))
                }
            } catch (_: Exception) {
            }

            val oldFd = vpnInterface
            vpnInterface = builder.establish()
            try {
                oldFd?.close()
            } catch (_: Exception) {
            }

            if (!isRunning) {
                isRunning = true
                startSinkholeDrainThread()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun registerNetworkCallback() {
        if (networkCallback != null) return
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    updateUnderlyingNetwork(network)
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                        updateUnderlyingNetwork(network)
                    }
                }

                override fun onLost(network: Network) {
                    val active = cm.activeNetwork
                    updateUnderlyingNetwork(active)
                }
            }
            cm.registerNetworkCallback(request, callback)
            networkCallback = callback
        } catch (_: Exception) {
        }
    }

    private fun updateUnderlyingNetwork(network: Network?) {
        try {
            if (network != null) {
                setUnderlyingNetworks(arrayOf(network))
            } else {
                setUnderlyingNetworks(null)
            }
        } catch (_: Exception) {
        }
    }

    private fun startSinkholeDrainThread() {
        thread(start = true, isDaemon = true, name = "VpnSinkholeDrain") {
            val buffer = ByteArray(32767)
            var currentPfd: ParcelFileDescriptor? = null
            var inputStream: FileInputStream? = null

            while (isRunning) {
                val pfd = vpnInterface
                if (pfd == null) {
                    try {
                        Thread.sleep(100L)
                    } catch (_: InterruptedException) {
                        break
                    }
                } else if (pfd !== currentPfd || inputStream == null) {
                    try {
                        inputStream = FileInputStream(pfd.fileDescriptor)
                        currentPfd = pfd
                        if (inputStream.read(buffer) < 0) {
                            Thread.sleep(50L)
                        }
                    } catch (_: Exception) {
                        currentPfd = pfd
                        inputStream = null
                        try {
                            Thread.sleep(50L)
                        } catch (_: InterruptedException) {
                            break
                        }
                    }
                } else {
                    try {
                        if (inputStream.read(buffer) < 0) {
                            Thread.sleep(50L)
                        }
                    } catch (_: Exception) {
                        inputStream = null
                        try {
                            Thread.sleep(50L)
                        } catch (_: InterruptedException) {
                            break
                        }
                    }
                }
            }
        }
    }

    private fun startOrUpdateForegroundNotification() {
        val channelId = "vpn_blocker_channel"
        val nm = getSystemService(NotificationManager::class.java)
        if (nm != null) {
            val channel = NotificationChannel(
                channelId,
                "OwlNetGuard VPN Nền",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
                description = "Dịch vụ VPN nền hỗ trợ cách ly ứng dụng"
            }
            nm.createNotificationChannel(channel)
        }

        val isIsolationActive = PreferencesManager.isAppIsolationActive(this)
        val effectiveCount = currentActiveBlockedSet.size
        val mode = PreferencesManager.getIsolationMode(this)

        val title = when {
            !isIsolationActive -> "VPN Nền đang trực (Đang tạm dừng cách ly)"
            effectiveCount > 0 -> "Đang cách ly mạng ($effectiveCount ứng dụng)"
            else -> "VPN Nền đang trực (Chờ kích hoạt ứng dụng)"
        }

        val subtitle = if (mode == PreferencesManager.ISOLATION_MODE_FOREGROUND) {
            "Phím nổi: Bật/tắt riêng app đang mở"
        } else {
            "Phím nổi: Bật/tắt tất cả ứng dụng"
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIsolateIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_TOGGLE_ISOLATE
        }
        val toggleIsolatePending = PendingIntent.getBroadcast(
            this,
            101,
            toggleIsolateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = try {
            BitmapFactory.decodeResource(
                resources,
                if (effectiveCount > 0) R.drawable.logo_faceted_apex_isolated else R.drawable.logo_faceted_apex
            )
        } catch (_: Exception) {
            null
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                if (isIsolationActive) R.drawable.ic_shield_normal else R.drawable.ic_shield_isolated,
                if (isIsolationActive) "Tạm dừng cách ly" else "Tiếp tục cách ly",
                toggleIsolatePending
            )
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (_: Exception) {
            }
        }
    }

    @Synchronized
    private fun stopVpnInternal() {
        isRunning = false
        currentActiveBlockedSet = emptySet()
        try {
            vpnInterface?.close()
        } catch (_: Exception) {
        }
        vpnInterface = null
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {
        }
        stopSelf()
    }

    override fun onDestroy() {
        networkCallback?.let {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            try {
                cm?.unregisterNetworkCallback(it)
            } catch (_: Exception) {
            }
            networkCallback = null
        }
        stopVpnInternal()
        super.onDestroy()
    }

    override fun onRevoke() {
        PreferencesManager.setVpnEnabled(this, false)
        stopVpnInternal()
        super.onRevoke()
    }

    companion object {
        const val ACTION_APPLY_VPN = "com.nightowleyes.owlnetguard.ACTION_APPLY_VPN"
        const val ACTION_STOP_VPN = "com.nightowleyes.owlnetguard.ACTION_STOP_VPN"
        private const val NOTIFICATION_ID = 1002

        /**
         * "Cách ly ứng dụng" maintains the Background VPN service.
         */
        fun shouldRunBackgroundVpn(context: Context): Boolean {
            return PreferencesManager.isVpnEnabled(context)
        }

        fun applyOrStop(context: Context): Boolean {
            val appCtx = context.applicationContext
            if (!shouldRunBackgroundVpn(appCtx)) {
                stop(appCtx)
                return true
            }
            if (prepare(appCtx) != null) {
                return false
            }
            return try {
                val intent = Intent(appCtx, LocalVpnService::class.java).apply {
                    action = ACTION_APPLY_VPN
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appCtx.startForegroundService(intent)
                } else {
                    appCtx.startService(intent)
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        fun stop(context: Context) {
            val appCtx = context.applicationContext
            try {
                val intent = Intent(appCtx, LocalVpnService::class.java).apply {
                    action = ACTION_STOP_VPN
                }
                appCtx.startService(intent)
            } catch (_: Exception) {
            }
        }
    }
}
