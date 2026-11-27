package com.nightowleyes.owlnetguard.service

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import com.nightowleyes.owlnetguard.R
import com.nightowleyes.owlnetguard.data.PreferencesManager
import com.nightowleyes.owlnetguard.utils.ForegroundAppDetector
import com.nightowleyes.owlnetguard.utils.NetworkSettingsHelper
import com.nightowleyes.owlnetguard.utils.NetworkStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BubbleService : Service() {

    private enum class ActionType {
        ISOLATE, DISRUPT
    }

    private var windowManager: WindowManager? = null
    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val networkChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context != null) {
                NetworkSettingsHelper.updateStatus(context)
                val netStatus = NetworkSettingsHelper.status.value
                checkNetworkReconnect(netStatus)
            }
        }
    }

    private var rootContainer: DraggableBubbleContainer? = null

    // Collapsed State Views
    private var cardCollapsedBubble: View? = null
    private var imgCollapsedOwl: ImageView? = null

    // Expanded State Views
    private var panelExpanded: LinearLayout? = null
    private var btnActionIsolate: View? = null
    private var imgBubbleIcon: ImageView? = null

    private var btnActionDisrupt: View? = null
    private var imgActionDisrupt: ImageView? = null

    private var btnActionCollapse: View? = null
    private var imgActionCollapse: ImageView? = null

    private var windowParams: WindowManager.LayoutParams? = null
    private var initialDragParamsX = 0
    private var initialDragParamsY = 0
    private var isAdded: Boolean = false
    private var isDockedRight: Boolean = true
    private var isExpanded: Boolean = false
    private var lastVisualIsolatedState: Boolean? = null

    private val idleFadeRunnable = Runnable {
        rootContainer?.animate()?.alpha(0.55f)?.setDuration(250L)?.start()
    }

    // Network Reconnect & Auto-Unisolate Tracking State
    private var wasFullyOffline: Boolean = false
    private var hudLastActiveApp: String? = null
    private var hudPausedByUserSessionApp: String? = null

    // Double-tap confirmation to cancel/pause isolation ("Hủy cách ly")
    private var lastUnisolateConfirmTimestamp: Long = 0L
    private var lastUnisolateConfirmPackage: String? = null

    private fun isAllNetworkOff(net: NetworkStatus): Boolean {
        val isAirplane = net.isAirplaneModeOn
        val isWifiOn = !isAirplane && (net.isWifiEnabled || net.isWifiConnected)
        val isCellularOn = !isAirplane && net.isCellularConnected
        return !isWifiOn && !isCellularOn
    }

    private fun checkNetworkReconnect(net: NetworkStatus) {
        val currentlyAllOff = isAllNetworkOff(net)

        if (!PreferencesManager.isAutoUnisolateOnReconnect(this)) {
            wasFullyOffline = currentlyAllOff
            return
        }

        val reconnectedFromFullOffline = wasFullyOffline && !currentlyAllOff
        wasFullyOffline = currentlyAllOff

        if (reconnectedFromFullOffline) {
            handleAutoUnisolate("sau khi bật lại mạng")
        }
    }

    private val foregroundStateWatcher = object : Runnable {
        override fun run() {
            try {
                if (shouldShowBubble(this@BubbleService)) {
                    NetworkSettingsHelper.updateStatus(this@BubbleService)
                    val net = NetworkSettingsHelper.status.value
                    checkNetworkReconnect(net)

                    val mode = PreferencesManager.getIsolationMode(this@BubbleService)
                    val bubblePackages = PreferencesManager.getBubblePackages(this@BubbleService)
                    val activeApps = PreferencesManager.getActivePackages(this@BubbleService)

                    if (mode == PreferencesManager.ISOLATION_MODE_FOREGROUND) {
                        val fg = ForegroundAppDetector.getForegroundPackage(this@BubbleService)
                        val isTransientOrExcluded = ForegroundAppDetector.isTransientOverlayOrSystemOrExcluded(this@BubbleService, fg)

                        if (!isTransientOrExcluded) {
                            val isLauncher = ForegroundAppDetector.isHomeLauncher(this@BubbleService, fg)
                            if (isLauncher) {
                                hudPausedByUserSessionApp = null
                                hudLastActiveApp = null
                                PreferencesManager.setForegroundTunnelPackage(this@BubbleService, null)
                            } else if (fg != null && bubblePackages.contains(fg)) {
                                if (fg != hudLastActiveApp) {
                                    hudPausedByUserSessionApp = null
                                    hudLastActiveApp = fg
                                    PreferencesManager.setForegroundTunnelPackage(this@BubbleService, fg)
                                } else {
                                    if (!activeApps.contains(fg) && fg != hudPausedByUserSessionApp) {
                                        PreferencesManager.setActiveForegroundApp(this@BubbleService, fg)
                                    }
                                }
                            } else {
                                hudPausedByUserSessionApp = null
                                hudLastActiveApp = null
                                PreferencesManager.setForegroundTunnelPackage(this@BubbleService, null)
                            }
                        }
                    } else {
                        if (activeApps.isNotEmpty()) {
                            val currentFg = ForegroundAppDetector.getForegroundPackage(this@BubbleService)
                            if (currentFg != null && !ForegroundAppDetector.isSystemOrSettingsOrLauncher(this@BubbleService, currentFg)) {
                                if (activeApps.contains(currentFg)) {
                                    hudLastActiveApp = currentFg
                                }
                            }
                        }
                    }

                    val currentVisual = computeBubbleVisualIsolated()
                    if (currentVisual != lastVisualIsolatedState) {
                        applyBubbleVisualState(currentVisual)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                handler.postDelayed(this, 250L)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NetworkSettingsHelper.init(this)
        try {
            (getSystemService(NOTIFICATION_SERVICE) as? NotificationManager)?.cancel(1001)
        } catch (_: Exception) {
        }
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            setupQuickBall()
            updateQuickBallState()

            try {
                val filter = IntentFilter().apply {
                    addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
                    addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
                    addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                    @Suppress("DEPRECATION")
                    addAction(ConnectivityManager.CONNECTIVITY_ACTION)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(networkChangeReceiver, filter, RECEIVER_EXPORTED)
                } else {
                    registerReceiver(networkChangeReceiver, filter)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            serviceScope.launch {
                PreferencesManager.stateVersion.collect {
                    updateQuickBallState()
                }
            }

            val initNet = NetworkSettingsHelper.status.value
            wasFullyOffline = isAllNetworkOff(initNet)

            serviceScope.launch {
                NetworkSettingsHelper.status.collect { netStatus ->
                    checkNetworkReconnect(netStatus)
                }
            }

            handler.post(foregroundStateWatcher)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Không thể hiển thị phím nổi: ${e.message}", Toast.LENGTH_SHORT).show()
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!shouldShowBubble(this)) {
            removeQuickBall()
            stopSelf()
            return START_NOT_STICKY
        }
        updateQuickBallState()
        handler.removeCallbacks(foregroundStateWatcher)
        handler.post(foregroundStateWatcher)
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val params = windowParams ?: return
        val dm = resources.displayMetrics
        val hudWidth = rootContainer?.width?.takeIf { it > 0 } ?: (if (isExpanded) 140 else 48 * dm.density).toInt()
        val hudHeight = rootContainer?.height?.takeIf { it > 0 } ?: (48 * dm.density).toInt()
        val maxX = (dm.widthPixels - hudWidth).coerceAtLeast(0)
        val maxY = (dm.heightPixels - hudHeight).coerceAtLeast(0)

        params.x = params.x.coerceIn(0, maxX)
        params.y = params.y.coerceIn(0, maxY)
        snapToNearestEdge()
        updateQuickBallState()
    }

    @SuppressLint("InflateParams")
    private fun setupQuickBall() {
        removeQuickBall()

        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.layout_floating_bubble, null) as DraggableBubbleContainer
        rootContainer = view

        cardCollapsedBubble = view.findViewById(R.id.cardCollapsedBubble)
        imgCollapsedOwl = view.findViewById(R.id.imgCollapsedOwl)

        panelExpanded = view.findViewById(R.id.panelExpanded)
        btnActionIsolate = view.findViewById(R.id.btnActionIsolate)
        imgBubbleIcon = view.findViewById(R.id.imgBubbleIcon)

        btnActionDisrupt = view.findViewById(R.id.btnActionDisrupt)
        imgActionDisrupt = view.findViewById(R.id.imgActionDisrupt)

        btnActionCollapse = view.findViewById(R.id.btnActionCollapse)
        imgActionCollapse = view.findViewById(R.id.imgActionCollapse)

        cardCollapsedBubble?.setOnClickListener {
            val enabled = getEnabledActions()
            if (enabled.size <= 1) {
                val singleAction = enabled.firstOrNull() ?: ActionType.ISOLATE
                executeAction(singleAction)
                scheduleIdleFade()
            } else {
                expandBubble()
            }
        }

        btnActionIsolate?.setOnClickListener {
            executeAction(ActionType.ISOLATE)
            scheduleIdleFade()
        }
        btnActionDisrupt?.setOnClickListener {
            executeAction(ActionType.DISRUPT)
            scheduleIdleFade()
        }
        btnActionCollapse?.setOnClickListener {
            collapseBubble()
        }

        val dm = resources.displayMetrics
        val marginEdge = (6 * dm.density).toInt()
        val initialY = (dm.heightPixels * 0.28f).toInt()
        val collapsedWidth = (48 * dm.density).toInt()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (dm.widthPixels - collapsedWidth - marginEdge).coerceAtLeast(marginEdge)
            y = initialY
        }
        windowParams = params
        isDockedRight = true
        isExpanded = false

        view.dragListener = object : DraggableBubbleContainer.DragListener {
            override fun onDragStart(rawX: Float, rawY: Float) {
                handler.removeCallbacks(idleFadeRunnable)
                rootContainer?.animate()?.alpha(1.0f)?.setDuration(100L)?.start()
                val p = windowParams ?: return
                initialDragParamsX = p.x
                initialDragParamsY = p.y
            }

            override fun onDragMove(dx: Float, dy: Float) {
                val p = windowParams ?: return
                val metrics = resources.displayMetrics
                val w = rootContainer?.width?.takeIf { it > 0 } ?: (48 * metrics.density).toInt()
                val h = rootContainer?.height?.takeIf { it > 0 } ?: (48 * metrics.density).toInt()
                val maxX = (metrics.widthPixels - w).coerceAtLeast(0)
                val maxY = (metrics.heightPixels - h).coerceAtLeast(0)

                p.x = (initialDragParamsX + dx).toInt().coerceIn(0, maxX)
                p.y = (initialDragParamsY + dy).toInt().coerceIn(0, maxY)
                updateWindowViewLayout()
            }

            override fun onDragEnd() {
                snapToNearestEdge()
                scheduleIdleFade()
            }

            override fun onLongPressDismiss() {
                Toast.makeText(
                    this@BubbleService,
                    "Đã tắt phím nổi và dừng VPN nền",
                    Toast.LENGTH_SHORT
                ).show()
                PreferencesManager.setPopupEnabled(this@BubbleService, false)
                removeQuickBall()
            }
        }
    }

    private fun expandBubble() {
        val enabled = getEnabledActions()
        if (enabled.size <= 1) {
            executeAction(enabled.firstOrNull() ?: ActionType.ISOLATE)
            scheduleIdleFade()
            return
        }

        isExpanded = true
        cardCollapsedBubble?.visibility = View.GONE
        panelExpanded?.visibility = View.VISIBLE

        val params = windowParams ?: return
        val dm = resources.displayMetrics
        rootContainer?.measure(
            View.MeasureSpec.makeMeasureSpec(dm.widthPixels, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(dm.heightPixels, View.MeasureSpec.AT_MOST)
        )
        val expandedWidth = rootContainer?.measuredWidth ?: (140 * dm.density).toInt()
        val marginEdge = (6 * dm.density).toInt()
        val maxX = (dm.widthPixels - expandedWidth - marginEdge).coerceAtLeast(marginEdge)
        if (params.x > maxX) {
            params.x = maxX
        }
        imgActionCollapse?.rotation = if (isDockedRight) 0f else 180f
        updateWindowViewLayout()
        updateQuickBallState()
        scheduleIdleFade()
    }

    private fun collapseBubble() {
        lastUnisolateConfirmTimestamp = 0L
        lastUnisolateConfirmPackage = null
        isExpanded = false
        panelExpanded?.visibility = View.GONE
        cardCollapsedBubble?.visibility = View.VISIBLE
        updateWindowViewLayout()
        snapToNearestEdge()
        updateQuickBallState()
        scheduleIdleFade()
    }

    private fun snapToNearestEdge() {
        val params = windowParams ?: return
        val dm = resources.displayMetrics
        val currentWidth = if (isExpanded) {
            rootContainer?.width?.takeIf { it > 0 } ?: (140 * dm.density).toInt()
        } else {
            (48 * dm.density).toInt()
        }
        val marginEdge = (6 * dm.density).toInt()
        val maxX = (dm.widthPixels - currentWidth - marginEdge).coerceAtLeast(marginEdge)

        val centerX = params.x + currentWidth / 2
        isDockedRight = centerX >= dm.widthPixels / 2

        val targetX = if (isDockedRight) maxX else marginEdge
        imgActionCollapse?.rotation = if (isDockedRight) 0f else 180f

        val startX = params.x
        val animator = ValueAnimator.ofInt(startX, targetX).apply {
            duration = 200L
            addUpdateListener { animation ->
                params.x = animation.animatedValue as Int
                updateWindowViewLayout()
            }
        }
        animator.start()
    }

    private fun scheduleIdleFade() {
        handler.removeCallbacks(idleFadeRunnable)
        handler.postDelayed(idleFadeRunnable, 3000L)
    }

    private fun getEnabledActions(): List<ActionType> {
        val list = ArrayList<ActionType>(2)
        if (PreferencesManager.isBubbleIsolateVisible(this)) list.add(ActionType.ISOLATE)
        if (PreferencesManager.isBubbleDisruptVisible(this)) list.add(ActionType.DISRUPT)
        if (list.isEmpty()) list.add(ActionType.ISOLATE)
        return list
    }

    private fun executeAction(type: ActionType) {
        when (type) {
            ActionType.ISOLATE -> executeIsolateAction()
            ActionType.DISRUPT -> executeNetworkDisruption()
        }
    }

    private fun executeIsolateAction() {
        val mode = PreferencesManager.getIsolationMode(this)
        val bubblePackages = PreferencesManager.getBubblePackages(this)
        val activeApps = PreferencesManager.getActivePackages(this)

        if (mode == PreferencesManager.ISOLATION_MODE_FOREGROUND) {
            if (!ForegroundAppDetector.hasUsageStatsPermission(this)) {
                Toast.makeText(
                    this,
                    "Vui lòng cấp quyền 'Truy cập dữ liệu sử dụng' cho OwlNetGuard!",
                    Toast.LENGTH_LONG
                ).show()
                ForegroundAppDetector.openUsageAccessSettings(this)
                return
            }

            val targetPkg = PreferencesManager.getForegroundTunnelPackage()

            if (targetPkg != null) {
                val isCurrentlyIsolated = activeApps.contains(targetPkg)
                if (isCurrentlyIsolated) {
                    val now = System.currentTimeMillis()
                    if (now - lastUnisolateConfirmTimestamp > 2500L || lastUnisolateConfirmPackage != targetPkg) {
                        lastUnisolateConfirmTimestamp = now
                        lastUnisolateConfirmPackage = targetPkg
                        Toast.makeText(
                            this,
                            "Nhấn thêm lần nữa để tạm dừng cách ly",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
                    lastUnisolateConfirmTimestamp = 0L
                    lastUnisolateConfirmPackage = null
                } else {
                    lastUnisolateConfirmTimestamp = 0L
                    lastUnisolateConfirmPackage = null
                }

                val appLabel = getAppLabel(targetPkg)
                val nowActive = PreferencesManager.togglePackageActive(this, targetPkg)
                hudPausedByUserSessionApp = if (!nowActive) targetPkg else null

                val msg = if (nowActive) {
                    "Đã ngắt mạng: $appLabel"
                } else {
                    "Đã mở lại mạng cho $appLabel"
                }
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                updateQuickBallState()
            } else {
                Toast.makeText(
                    this,
                    "Chỉ có thể bật/tắt cách ly khi đang mở ứng dụng trong danh sách!",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        } else {
            if (activeApps.isEmpty() && bubblePackages.isEmpty()) {
                Toast.makeText(
                    this,
                    "Chưa có ứng dụng nào được chọn trong danh sách!",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val isCurrentlyIsolated = activeApps.isNotEmpty()
            if (isCurrentlyIsolated) {
                val now = System.currentTimeMillis()
                if (now - lastUnisolateConfirmTimestamp > 2500L) {
                    lastUnisolateConfirmTimestamp = now
                    Toast.makeText(
                        this,
                        "Nhấn thêm lần nữa để tạm dừng cách ly",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                lastUnisolateConfirmTimestamp = 0L
                PreferencesManager.setAllPackagesActive(this, false)
                Toast.makeText(this, "Đã tạm dừng cách ly tất cả ứng dụng", Toast.LENGTH_SHORT).show()
            } else {
                lastUnisolateConfirmTimestamp = 0L
                PreferencesManager.setAllPackagesActive(this, true)
                val updatedActiveCount = PreferencesManager.getActivePackages(this).size
                Toast.makeText(this, "Đang cách ly mạng $updatedActiveCount ứng dụng đã chọn", Toast.LENGTH_SHORT).show()
            }
        }
        updateQuickBallState()
    }

    /**
     * Làm gián đoạn mạng:
     * Chủ động đưa ứng dụng ra khỏi đường hầm VPN trong ~650ms rồi đưa lại vào đường hầm,
     * tạo sự kiện thay đổi giao diện mạng / reset socket tức thì mà không cần tắt mở Wi-Fi hay 4G.
     * Nếu bật "Tự hủy cách ly", đồng thời giải phóng cách ly cho ứng dụng sau khi gián đoạn.
     */
    private fun executeNetworkDisruption() {
        lastUnisolateConfirmTimestamp = 0L
        lastUnisolateConfirmPackage = null

        if (PreferencesManager.isDisruptingNetwork()) return

        val mode = PreferencesManager.getIsolationMode(this)
        val bubblePackages = PreferencesManager.getBubblePackages(this)

        val targetFgPkg: String? = if (mode == PreferencesManager.ISOLATION_MODE_FOREGROUND) {
            if (!ForegroundAppDetector.hasUsageStatsPermission(this)) {
                Toast.makeText(
                    this,
                    "Vui lòng cấp quyền 'Truy cập dữ liệu sử dụng' cho OwlNetGuard!",
                    Toast.LENGTH_LONG
                ).show()
                ForegroundAppDetector.openUsageAccessSettings(this)
                return
            }
            val fgPkg = PreferencesManager.getForegroundTunnelPackage()
            if (fgPkg == null) {
                Toast.makeText(
                    this,
                    "Chỉ có thể làm gián đoạn mạng khi đang mở ứng dụng trong danh sách!",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            fgPkg
        } else {
            if (bubblePackages.isEmpty()) {
                Toast.makeText(
                    this,
                    "Chưa có ứng dụng nào được chọn trong danh sách!",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            null
        }

        // Visual feedback pulse on the disrupt icon
        imgActionDisrupt?.animate()?.alpha(0.35f)?.scaleX(0.85f)?.scaleY(0.85f)?.setDuration(150L)?.start()
        if (!isExpanded && getEnabledActions() == listOf(ActionType.DISRUPT)) {
            imgCollapsedOwl?.animate()?.alpha(0.35f)?.scaleX(0.85f)?.scaleY(0.85f)?.setDuration(150L)?.start()
        }

        // Step 1: Eject target app(s) out of VPN tunnel immediately
        PreferencesManager.setDisruptingNetwork(this, true)

        // Step 2: After brief disruption window, bring app(s) back into tunnel (& auto-unisolate if enabled)
        handler.postDelayed({
            try {
                val autoUnisolate = PreferencesManager.isAutoUnisolateOnReconnect(this@BubbleService)
                var didUnisolate = false

                if (autoUnisolate) {
                    val activeApps = PreferencesManager.getActivePackages(this@BubbleService)
                    if (mode == PreferencesManager.ISOLATION_MODE_FOREGROUND && targetFgPkg != null) {
                        if (activeApps.contains(targetFgPkg)) {
                            PreferencesManager.setPackageActive(this@BubbleService, targetFgPkg, false)
                            hudPausedByUserSessionApp = targetFgPkg
                            didUnisolate = true
                        }
                    } else if (mode == PreferencesManager.ISOLATION_MODE_ALL) {
                        if (activeApps.isNotEmpty()) {
                            PreferencesManager.setAllPackagesActive(this@BubbleService, false)
                            hudLastActiveApp = null
                            didUnisolate = true
                        }
                    }
                }

                // Re-enter VPN tunnel
                PreferencesManager.setDisruptingNetwork(this@BubbleService, false)

                val targetText = if (targetFgPkg != null) getAppLabel(targetFgPkg) else "ứng dụng đã chọn"
                val toastMsg = if (didUnisolate) {
                    "Đã làm gián đoạn mạng & tự hủy cách ly cho $targetText"
                } else {
                    "Đã làm gián đoạn mạng cho $targetText"
                }
                Toast.makeText(this@BubbleService, toastMsg, Toast.LENGTH_SHORT).show()
            } finally {
                imgActionDisrupt?.animate()?.alpha(1.0f)?.scaleX(1.0f)?.scaleY(1.0f)?.setDuration(150L)?.start()
                imgCollapsedOwl?.animate()?.alpha(1.0f)?.scaleX(1.0f)?.scaleY(1.0f)?.setDuration(150L)?.start()
                updateQuickBallState()
            }
        }, 650L)
    }

    private fun handleAutoUnisolate(reasonSuffix: String) {
        if (!PreferencesManager.isAutoUnisolateOnReconnect(this)) return

        val mode = PreferencesManager.getIsolationMode(this)
        val selectedApps = PreferencesManager.getBubblePackages(this)
        val activeApps = PreferencesManager.getActivePackages(this)

        if (selectedApps.isEmpty()) return

        if (mode == PreferencesManager.ISOLATION_MODE_FOREGROUND) {
            val targetPkg = PreferencesManager.getForegroundTunnelPackage()
            if (targetPkg != null && activeApps.contains(targetPkg)) {
                PreferencesManager.setPackageActive(this, targetPkg, false)
                hudPausedByUserSessionApp = targetPkg
                val appLabel = getAppLabel(targetPkg)
                Toast.makeText(
                    this,
                    "Đã tự động hủy cách ly cho $appLabel $reasonSuffix",
                    Toast.LENGTH_SHORT
                ).show()
                updateQuickBallState()
            }
        } else {
            if (activeApps.isNotEmpty()) {
                PreferencesManager.setAllPackagesActive(this, false)
                hudLastActiveApp = null
                val message = if (selectedApps.size == 1) {
                    val appLabel = getAppLabel(selectedApps.first())
                    "Đã tự động hủy cách ly cho $appLabel $reasonSuffix"
                } else {
                    "Đã tự động hủy cách ly cho các ứng dụng đã chọn $reasonSuffix"
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                updateQuickBallState()
            }
        }
    }

    private fun getAppLabel(pkg: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            pkg
        }
    }

    private fun computeBubbleVisualIsolated(): Boolean {
        if (!PreferencesManager.isPopupEnabled(this)) return false
        if (!PreferencesManager.isAppIsolationActive(this)) return false

        val activeSet = PreferencesManager.getActivePackages(this)
        if (activeSet.isEmpty()) return false

        val mode = PreferencesManager.getIsolationMode(this)
        return if (mode == PreferencesManager.ISOLATION_MODE_FOREGROUND) {
            val fgTunnelPkg = PreferencesManager.getForegroundTunnelPackage()
            fgTunnelPkg != null && activeSet.contains(fgTunnelPkg)
        } else {
            true
        }
    }

    private fun applyBubbleVisualState(visualIsolated: Boolean) {
        lastVisualIsolatedState = visualIsolated

        val enabledActions = getEnabledActions()
        val onlyDisrupt = enabledActions == listOf(ActionType.DISRUPT)

        cardCollapsedBubble?.setBackgroundResource(
            if (visualIsolated) R.drawable.bg_floating_bubble_isolated else R.drawable.bg_floating_bubble
        )
        if (onlyDisrupt) {
            imgCollapsedOwl?.setImageResource(R.drawable.ic_network_disrupt)
        } else {
            imgCollapsedOwl?.setImageResource(
                if (visualIsolated) R.drawable.logo_faceted_apex_isolated else R.drawable.logo_faceted_apex
            )
        }

        btnActionIsolate?.setBackgroundResource(
            if (visualIsolated) R.drawable.bg_floating_bubble_isolated else R.drawable.bg_floating_bubble
        )
        imgBubbleIcon?.setImageResource(
            if (visualIsolated) R.drawable.logo_faceted_apex_isolated else R.drawable.logo_faceted_apex
        )

        imgActionCollapse?.rotation = if (isDockedRight) 0f else 180f
    }

    fun updateQuickBallState() {
        if (!shouldShowBubble(this)) {
            removeQuickBall()
            return
        }

        val visualIsolated = computeBubbleVisualIsolated()
        applyBubbleVisualState(visualIsolated)

        val enabledActions = getEnabledActions()
        btnActionIsolate?.visibility =
            if (enabledActions.contains(ActionType.ISOLATE)) View.VISIBLE else View.GONE
        btnActionDisrupt?.visibility =
            if (enabledActions.contains(ActionType.DISRUPT)) View.VISIBLE else View.GONE

        if (enabledActions.size <= 1) {
            isExpanded = false
        }

        if (isExpanded) {
            cardCollapsedBubble?.visibility = View.GONE
            panelExpanded?.visibility = View.VISIBLE
        } else {
            cardCollapsedBubble?.visibility = View.VISIBLE
            panelExpanded?.visibility = View.GONE
        }

        btnActionCollapse?.visibility = if (enabledActions.size > 1) View.VISIBLE else View.GONE

        addOrUpdateWindow()
        scheduleIdleFade()
    }

    private fun addOrUpdateWindow() {
        val view = rootContainer ?: return
        val params = windowParams ?: return
        if (!isAdded) {
            try {
                windowManager?.addView(view, params)
                isAdded = true
            } catch (_: Exception) {
            }
        } else {
            updateWindowViewLayout()
        }
    }

    private fun updateWindowViewLayout() {
        val view = rootContainer ?: return
        val params = windowParams ?: return
        if (isAdded) {
            try {
                windowManager?.updateViewLayout(view, params)
            } catch (_: Exception) {
            }
        }
    }

    private fun removeQuickBall() {
        handler.removeCallbacks(idleFadeRunnable)
        handler.removeCallbacks(foregroundStateWatcher)
        val view = rootContainer
        if (isAdded && view != null) {
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {
            }
        }
        isAdded = false
    }

    override fun onDestroy() {
        serviceScope.cancel()
        try {
            unregisterReceiver(networkChangeReceiver)
        } catch (_: Exception) {
        }
        handler.removeCallbacks(foregroundStateWatcher)
        handler.removeCallbacks(idleFadeRunnable)
        removeQuickBall()
        super.onDestroy()
    }

    companion object {
        fun shouldShowBubble(context: Context): Boolean {
            return PreferencesManager.isPopupEnabled(context) &&
                PreferencesManager.isVpnPermissionGranted(context) &&
                Settings.canDrawOverlays(context)
        }

        fun sync(context: Context) {
            val appCtx = context.applicationContext
            if (shouldShowBubble(appCtx)) {
                try {
                    val intent = Intent(appCtx, BubbleService::class.java)
                    appCtx.startService(intent)
                } catch (_: Exception) {
                }
            } else {
                try {
                    appCtx.stopService(Intent(appCtx, BubbleService::class.java))
                } catch (_: Exception) {
                }
            }
            LocalVpnService.updateNotification(appCtx)
        }
    }
}
