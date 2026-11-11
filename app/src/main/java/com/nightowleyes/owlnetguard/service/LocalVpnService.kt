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
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import androidx.core.app.NotificationCompat
import com.nightowleyes.owlnetguard.R
import com.nightowleyes.owlnetguard.data.PreferencesManager
import com.nightowleyes.owlnetguard.receiver.NotificationActionReceiver
import com.nightowleyes.owlnetguard.ui.MainActivity
import kotlin.concurrent.thread

class LocalVpnService : VpnService() {

    @Volatile
    private var vpnInterface: ParcelFileDescriptor? = null

    @Volatile
    private var isDrainThreadRunning: Boolean = false

    @Volatile
    private var lastEstablishedBlocked: Boolean? = null

    @Volatile
    private var lastEstablishedPackages: Set<String> = emptySet()

    override fun onCreate() {
        super.onCreate()
        startOrUpdateForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_VPN) {
            stopVpnInternal()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_UPDATE_NOTIFICATION) {
            if (isRunning && shouldRunBackgroundVpn(this)) {
                startOrUpdateForegroundNotification()
            }
            return START_STICKY
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

        val mode = PreferencesManager.getIsolationMode(this)
        val effectiveBlocked = PreferencesManager.getEffectiveBlockedPackages(this)
            .filterTo(LinkedHashSet()) { it != packageName }
        val allManagedPackages = PreferencesManager.getBubblePackages(this)
            .filterTo(LinkedHashSet()) { it != packageName }

        // Determine targetPackages based on isolation mode:
        // - In Mode All ("Tất cả app đã chọn"): all managed apps remain bound in addAllowedApplication
        //   so their traffic falls through to physical network when unblocked, or to sinkhole when blocked.
        // - In Foreground Mode ("Chỉ app đang mở"): ONLY the currently opened app is put into the tunnel.
        //   When no managed app is open, targetPackages is empty (tunnel closed, direct physical network).
        //   While using the app, targetPackages stays setOf(fgApp) continuously so toggling isolation
        //   does not recreate the tunnel or disconnect sockets. Only when completely exiting the app
        //   ("out hẳn app") is targetPackages cleared back to empty.
        val targetPackages: Set<String> = if (mode == PreferencesManager.ISOLATION_MODE_FOREGROUND) {
            val fgTunnelPkg = PreferencesManager.getForegroundTunnelPackage()
            if (fgTunnelPkg != null && allManagedPackages.contains(fgTunnelPkg)) {
                setOf(fgTunnelPkg)
            } else {
                emptySet()
            }
        } else {
            allManagedPackages.ifEmpty { effectiveBlocked }
        }

        // isBlocked is true if and only if there are packages actively marked for isolation.
        val isBlocked = effectiveBlocked.isNotEmpty()

        startOrUpdateForegroundNotification()

        if (targetPackages.isEmpty()) {
            isRunning = false
            lastEstablishedBlocked = null
            lastEstablishedPackages = emptySet()
            try {
                vpnInterface?.close()
            } catch (_: Exception) {
            }
            vpnInterface = null
            return
        }

        // Avoid redundant establish() calls when neither the route state nor the bound app set changed
        if (vpnInterface != null &&
            isRunning &&
            lastEstablishedBlocked == isBlocked &&
            lastEstablishedPackages == targetPackages
        ) {
            return
        }

        try {
            // Exact GauVPN Builder logic:
            val builder = Builder()
                .setSession("MiniNetworkBlocker")
                .setMtu(1500)
                .setBlocking(true)
                .addAddress("10.215.173.1", 30)
                .addAddress("fd00:1:fd00:1:fd00:1:fd00:1", 128)

            if (isBlocked) {
                // Route traffic of isolated apps into the silent sinkhole
                builder.addRoute("0.0.0.0", 1)
                builder.addRoute("128.0.0.0", 1)
                builder.addRoute("::", 1)
                builder.addRoute("8000::", 1)
                builder.addDnsServer("10.215.173.2")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            // Bind current physical network to builder (GauVPN style - never bind VPN to itself):
            try {
                val cm = getSystemService(CONNECTIVITY_SERVICE) as? ConnectivityManager
                val activeNet = cm?.activeNetwork
                if (activeNet != null) {
                    val caps = cm.getNetworkCapabilities(activeNet)
                    if (caps != null && !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                        builder.setUnderlyingNetworks(arrayOf(activeNet))
                    }
                }
            } catch (_: Exception) {
            }

            var addedCount = 0
            for (pkg in targetPackages) {
                try {
                    builder.addAllowedApplication(pkg)
                    addedCount++
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (addedCount == 0) {
                try {
                    builder.addAllowedApplication(packageName)
                } catch (_: Exception) {
                }
            }

            val oldFd = vpnInterface
            val newFd = builder.establish()
            vpnInterface = newFd
            lastEstablishedBlocked = isBlocked
            lastEstablishedPackages = LinkedHashSet(targetPackages)
            isRunning = newFd != null

            if (newFd != null) {
                ensureDrainThreadStarted()
            }

            if (oldFd != null) {
                try {
                    oldFd.close()
                } catch (_: Exception) {
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun ensureDrainThreadStarted() {
        if (isDrainThreadRunning) return
        isDrainThreadRunning = true
        thread(start = true, isDaemon = true, name = "VpnSinkholeDrain") {
            val buffer = ByteArray(32767)
            val pollFd = StructPollfd()
            val pollFds = arrayOf(pollFd)

            while (isRunning) {
                val pfd = vpnInterface
                if (pfd == null) {
                    try {
                        Thread.sleep(80L)
                    } catch (_: InterruptedException) {
                        break
                    }
                    continue
                }
                try {
                    val fd = pfd.fileDescriptor
                    if (!fd.valid()) {
                        try {
                            Thread.sleep(50L)
                        } catch (_: InterruptedException) {
                            break
                        }
                        continue
                    }
                    pollFd.fd = fd
                    pollFd.events = OsConstants.POLLIN.toShort()
                    pollFd.revents = 0
                    val ready = Os.poll(pollFds, 100)
                    if (!isRunning || vpnInterface !== pfd) continue
                    if (ready > 0) {
                        val revents = pollFd.revents.toInt()
                        if ((revents and (OsConstants.POLLERR or OsConstants.POLLHUP or OsConstants.POLLNVAL)) != 0) {
                            try {
                                Thread.sleep(50L)
                            } catch (_: InterruptedException) {
                                break
                            }
                            continue
                        }
                        if ((revents and OsConstants.POLLIN) != 0) {
                            val bytesRead = Os.read(fd, buffer, 0, buffer.size)
                            if (bytesRead < 0) {
                                Thread.sleep(20L)
                            }
                        }
                    }
                } catch (_: InterruptedException) {
                    break
                } catch (_: Exception) {
                    if (!isRunning || vpnInterface !== pfd) continue
                    try {
                        Thread.sleep(50L)
                    } catch (_: InterruptedException) {
                        break
                    }
                }
            }
            isDrainThreadRunning = false
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
        val effectiveCount = PreferencesManager.getEffectiveBlockedPackages(this)
            .filter { it != packageName }.size
        val mode = PreferencesManager.getIsolationMode(this)
        val isBubbleEnabled = PreferencesManager.isBubbleOverlayEnabled(this)

        val title = when {
            !isIsolationActive -> "VPN Nền đang trực (Đang tạm dừng cách ly)"
            effectiveCount > 0 -> "Đang cách ly mạng ($effectiveCount ứng dụng)"
            else -> "VPN Nền đang trực (Chờ kích hoạt ứng dụng)"
        }

        val modeText = if (mode == PreferencesManager.ISOLATION_MODE_FOREGROUND) {
            "Cách ly: App đang mở"
        } else {
            "Cách ly: Tất cả app đã chọn"
        }
        val bubbleText = if (isBubbleEnabled) "Phím nổi: Bật" else "Phím nổi: Tắt"
        val subtitle = "$modeText • $bubbleText"

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopServiceIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_STOP_SERVICE
        }
        val stopServicePending = PendingIntent.getBroadcast(
            this,
            101,
            stopServiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleBubbleIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_TOGGLE_BUBBLE_VISIBILITY
        }
        val toggleBubblePending = PendingIntent.getBroadcast(
            this,
            102,
            toggleBubbleIntent,
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
                R.drawable.ic_power,
                "Dừng dịch vụ",
                stopServicePending
            )
            .addAction(
                if (isBubbleEnabled) R.drawable.ic_bubble_inactive else R.drawable.ic_bubble_active,
                if (isBubbleEnabled) "Tắt phím nổi" else "Bật phím nổi",
                toggleBubblePending
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
        lastEstablishedBlocked = null
        lastEstablishedPackages = emptySet()
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
        const val ACTION_UPDATE_NOTIFICATION = "com.nightowleyes.owlnetguard.ACTION_UPDATE_NOTIFICATION"
        private const val NOTIFICATION_ID = 1002

        @Volatile
        var isRunning: Boolean = false

        /**
         * "Cách ly ứng dụng" maintains the Background VPN service.
         */
        fun shouldRunBackgroundVpn(context: Context): Boolean {
            return PreferencesManager.isVpnEnabled(context)
        }

        fun updateNotification(context: Context) {
            val appCtx = context.applicationContext
            if (isRunning) {
                try {
                    val intent = Intent(appCtx, LocalVpnService::class.java).apply {
                        action = ACTION_UPDATE_NOTIFICATION
                    }
                    appCtx.startService(intent)
                } catch (_: Exception) {
                }
            }
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
                appCtx.startForegroundService(intent)
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
