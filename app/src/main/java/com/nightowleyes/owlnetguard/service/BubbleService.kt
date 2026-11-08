package com.nightowleyes.owlnetguard.service

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
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
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.nightowleyes.owlnetguard.R
import com.nightowleyes.owlnetguard.data.PreferencesManager
import com.nightowleyes.owlnetguard.receiver.NotificationActionReceiver
import com.nightowleyes.owlnetguard.ui.MainActivity
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
        ISOLATE, WIFI, DATA, AIRPLANE
    }

    private var windowManager: WindowManager? = null
    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val networkChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context != null) {
                NetworkSettingsHelper.updateStatus(context)
                val netStatus = NetworkSettingsHelper.status.value
                updateNetworkViews(netStatus)
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

    private var btnActionWifi: View? = null
    private var imgActionWifi: ImageView? = null
    private var slashActionWifi: ImageView? = null

    private var btnActionData: View? = null
    private var imgActionData: ImageView? = null
    private var slashActionData: ImageView? = null

    private var btnActionAirplane: View? = null
    private var imgActionAirplane: ImageView? = null
    private var slashActionAirplane: ImageView? = null

    private var btnActionCollapse: View? = null
    private var imgActionCollapse: ImageView? = null

    private var windowParams: WindowManager.LayoutParams? = null
    private var isAdded: Boolean = false
    private var isDockedRight: Boolean = true
    private var isExpanded: Boolean = false
    private var lastVisualIsolatedState: Boolean? = null

    private val idleFadeRunnable = Runnable {
        rootContainer?.animate()?.alpha(0.55f)?.setDuration(250L)?.start()
    }

    // Network Reconnect & Auto-Unisolate Tracking State
    // Only triggers when ALL networks (both Wi-Fi and Mobile Data) were completely off, then turned back on
    private var wasFullyOffline: Boolean = false
    private var hudLastActiveApp: String? = null

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

        // Only trigger auto-unisolate when transitioning from COMPLETELY OFFLINE
        // (both Wi-Fi and Mobile Data were OFF / Airplane ON) -> to ONLINE (at least one turned back ON)
        val reconnectedFromFullOffline = wasFullyOffline && !currentlyAllOff
        wasFullyOffline = currentlyAllOff

        if (reconnectedFromFullOffline) {
            handleAutoUnisolateOnReconnect()
        }
    }

    private val foregroundStateWatcher = object : Runnable {
        override fun run() {
            try {
                if (shouldShowBubble(this@BubbleService)) {
                    // Check network status & auto-unisolate on reconnect
                    NetworkSettingsHelper.updateStatus(this@BubbleService)
                    val net = NetworkSettingsHelper.status.value
                    updateNetworkViews(net)
                    checkNetworkReconnect(net)

                    // Track foreground user app for HUD reconnection target
                    val activeApps = PreferencesManager.getActivePackages(this@BubbleService)
                    if (activeApps.isNotEmpty()) {
                        val currentFg = ForegroundAppDetector.getForegroundPackage(this@BubbleService)
                        if (currentFg != null && !ForegroundAppDetector.isSystemOrSettingsOrLauncher(this@BubbleService, currentFg)) {
                            if (activeApps.contains(currentFg)) {
                                hudLastActiveApp = currentFg
                            }
                        }
                    }

                    // Update visual isolate state
                    val currentVisual = computeBubbleVisualIsolated()
                    if (currentVisual != lastVisualIsolatedState) {
                        applyBubbleVisualState(currentVisual)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                handler.postDelayed(this, 500L)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NetworkSettingsHelper.init(this)
        startForegroundWithNotification()
        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            setupQuickBall()
            updateQuickBallState()

            // Safe register local broadcast receiver for immediate reaction to Wi-Fi / connectivity
            try {
                val filter = IntentFilter().apply {
                    addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
                    addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
                    addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                    @Suppress("DEPRECATION")
                    addAction(ConnectivityManager.CONNECTIVITY_ACTION)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(networkChangeReceiver, filter, Context.RECEIVER_EXPORTED)
                } else {
                    registerReceiver(networkChangeReceiver, filter)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Instantly sync UI when settings or app list changes
            serviceScope.launch {
                PreferencesManager.stateVersion.collect {
                    updateQuickBallState()
                }
            }

            val initNet = NetworkSettingsHelper.status.value
            wasFullyOffline = isAllNetworkOff(initNet)

            // Instantly sync network icons & check auto-unisolate when status changes
            serviceScope.launch {
                NetworkSettingsHelper.status.collect { netStatus ->
                    updateNetworkViews(netStatus)
                    checkNetworkReconnect(netStatus)
                }
            }

            handler.post(foregroundStateWatcher)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Không thể hiển thị bong bóng: ${e.message}", Toast.LENGTH_SHORT).show()
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!shouldShowBubble(this)) {
            removeQuickBall()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundWithNotification()
        updateQuickBallState()
        handler.removeCallbacks(foregroundStateWatcher)
        handler.post(foregroundStateWatcher)
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val params = windowParams ?: return
        val dm = resources.displayMetrics
        val hudWidth = rootContainer?.width?.takeIf { it > 0 } ?: (if (isExpanded) 180 else 48 * dm.density).toInt()
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

        // 1. Collapsed State Views
        cardCollapsedBubble = view.findViewById(R.id.cardCollapsedBubble)
        imgCollapsedOwl = view.findViewById(R.id.imgCollapsedOwl)

        // 2. Expanded State Views
        panelExpanded = view.findViewById(R.id.panelExpanded)
        btnActionIsolate = view.findViewById(R.id.btnActionIsolate)
        imgBubbleIcon = view.findViewById(R.id.imgBubbleIcon)

        btnActionWifi = view.findViewById(R.id.btnActionWifi)
        imgActionWifi = view.findViewById(R.id.imgActionWifi)
        slashActionWifi = view.findViewById(R.id.slashActionWifi)

        btnActionData = view.findViewById(R.id.btnActionData)
        imgActionData = view.findViewById(R.id.imgActionData)
        slashActionData = view.findViewById(R.id.slashActionData)

        btnActionAirplane = view.findViewById(R.id.btnActionAirplane)
        imgActionAirplane = view.findViewById(R.id.imgActionAirplane)
        slashActionAirplane = view.findViewById(R.id.slashActionAirplane)

        btnActionCollapse = view.findViewById(R.id.btnActionCollapse)
        imgActionCollapse = view.findViewById(R.id.imgActionCollapse)

        // Tapping the single collapsed bubble expands into the action bar
        cardCollapsedBubble?.setOnClickListener {
            expandBubble()
        }

        // Action buttons: execute action and DO NOT collapse!
        btnActionIsolate?.setOnClickListener {
            executeAction(ActionType.ISOLATE)
            scheduleIdleFade()
        }
        btnActionWifi?.setOnClickListener {
            executeAction(ActionType.WIFI)
            scheduleIdleFade()
        }
        btnActionData?.setOnClickListener {
            executeAction(ActionType.DATA)
            scheduleIdleFade()
        }
        btnActionAirplane?.setOnClickListener {
            executeAction(ActionType.AIRPLANE)
            scheduleIdleFade()
        }

        // Dedicated Collapse button: ONLY tapping this button collapses back to the single bubble
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

        var initialParamsX = 0
        var initialParamsY = 0

        // Unified Drag Listener: Allows smooth dragging from ANYWHERE on the HUD (background or buttons)
        view.dragListener = object : DraggableBubbleContainer.DragListener {
            override fun onDragStart(rawX: Float, rawY: Float) {
                handler.removeCallbacks(idleFadeRunnable)
                rootContainer?.animate()?.alpha(1.0f)?.setDuration(100L)?.start()
                val p = windowParams ?: return
                initialParamsX = p.x
                initialParamsY = p.y
            }

            override fun onDragMove(dx: Float, dy: Float) {
                val p = windowParams ?: return
                val metrics = resources.displayMetrics
                val w = rootContainer?.width?.takeIf { it > 0 } ?: (48 * metrics.density).toInt()
                val h = rootContainer?.height?.takeIf { it > 0 } ?: (48 * metrics.density).toInt()
                val maxX = (metrics.widthPixels - w).coerceAtLeast(0)
                val maxY = (metrics.heightPixels - h).coerceAtLeast(0)

                p.x = (initialParamsX + dx).toInt().coerceIn(0, maxX)
                p.y = (initialParamsY + dy).toInt().coerceIn(0, maxY)
                updateWindowViewLayout()
            }

            override fun onDragEnd() {
                snapToNearestEdge()
                scheduleIdleFade()
            }

            override fun onLongPressDismiss() {
                Toast.makeText(
                    this@BubbleService,
                    "Đã tắt phím nổi (có thể bật lại trong ứng dụng hoặc thông báo)",
                    Toast.LENGTH_SHORT
                ).show()
                PreferencesManager.setBubbleOverlayEnabled(this@BubbleService, false)
                removeQuickBall()
            }
        }
    }

    private fun expandBubble() {
        isExpanded = true
        cardCollapsedBubble?.visibility = View.GONE
        panelExpanded?.visibility = View.VISIBLE

        val params = windowParams ?: return
        val dm = resources.displayMetrics
        rootContainer?.measure(
            View.MeasureSpec.makeMeasureSpec(dm.widthPixels, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(dm.heightPixels, View.MeasureSpec.AT_MOST)
        )
        val expandedWidth = rootContainer?.measuredWidth ?: (180 * dm.density).toInt()
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
            rootContainer?.width?.takeIf { it > 0 } ?: (180 * dm.density).toInt()
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
        val list = ArrayList<ActionType>(4)
        if (PreferencesManager.isBubbleIsolateVisible(this)) list.add(ActionType.ISOLATE)
        if (PreferencesManager.isBubbleWifiVisible(this)) list.add(ActionType.WIFI)
        if (PreferencesManager.isBubbleDataVisible(this)) list.add(ActionType.DATA)
        if (PreferencesManager.isBubbleAirplaneVisible(this)) list.add(ActionType.AIRPLANE)
        return list
    }

    /**
     * Executes the requested action without automatically collapsing the bubble.
     */
    private fun executeAction(type: ActionType) {
        when (type) {
            ActionType.ISOLATE -> {
                if (!PreferencesManager.isVpnEnabled(this)) {
                    PreferencesManager.setVpnEnabled(this, true)
                }

                val mode = PreferencesManager.getIsolationMode(this)
                val bubblePackages = PreferencesManager.getBubblePackages(this)
                val activeApps = PreferencesManager.getActivePackages(this)

                if (mode == PreferencesManager.ISOLATION_MODE_FOREGROUND) {
                    val fg = ForegroundAppDetector.getForegroundPackage(this)
                    if (fg != null && bubblePackages.contains(fg)) {
                        val appLabel = try {
                            val appInfo = packageManager.getApplicationInfo(fg, 0)
                            packageManager.getApplicationLabel(appInfo).toString()
                        } catch (_: Exception) {
                            fg
                        }

                        val nowActive = PreferencesManager.togglePackageActive(this, fg)
                        val activeCount = PreferencesManager.getActivePackages(this).size
                        val msg = if (nowActive) {
                            "Đã bật cách ly mạng: $appLabel"
                        } else {
                            "Đã mở lại mạng cho $appLabel ($activeCount ứng dụng khác vẫn được cách ly)"
                        }
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    } else {
                        // On Launcher, inside OwlNetGuard, or other unmanaged app:
                        // Toggle overall active state
                        if (activeApps.isEmpty() && bubblePackages.isEmpty()) {
                            Toast.makeText(
                                this,
                                "Chưa có ứng dụng nào được kích hoạt trong danh sách!",
                                Toast.LENGTH_SHORT
                            ).show()
                            return
                        }
                        val isolated = PreferencesManager.toggleAppIsolationActive(this)
                        val msg = if (isolated) {
                            "Đang cách ly mạng ${activeApps.size} ứng dụng đã chọn"
                        } else {
                            "Đã tạm dừng cách ly tất cả ứng dụng"
                        }
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    if (activeApps.isEmpty() && bubblePackages.isEmpty()) {
                        Toast.makeText(
                            this,
                            "Chưa có ứng dụng nào được kích hoạt trong danh sách!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }

                    val isolated = PreferencesManager.toggleAppIsolationActive(this)
                    val msg = if (isolated) {
                        "Đang cách ly mạng ${activeApps.size} ứng dụng đã chọn"
                    } else {
                        "Đã tạm dừng cách ly tất cả ứng dụng"
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
                updateQuickBallState()
            }
            ActionType.WIFI -> {
                val activeApps = PreferencesManager.getActivePackages(this)
                hudLastActiveApp = ForegroundAppDetector.getLastUserForegroundPackage(this, activeApps)
                    ?: ForegroundAppDetector.getForegroundPackage(this)?.takeIf { activeApps.contains(it) }
                NetworkSettingsHelper.openWifiPanel(this)
            }
            ActionType.DATA -> {
                val activeApps = PreferencesManager.getActivePackages(this)
                hudLastActiveApp = ForegroundAppDetector.getLastUserForegroundPackage(this, activeApps)
                    ?: ForegroundAppDetector.getForegroundPackage(this)?.takeIf { activeApps.contains(it) }
                NetworkSettingsHelper.openDataPanel(this)
            }
            ActionType.AIRPLANE -> {
                val activeApps = PreferencesManager.getActivePackages(this)
                hudLastActiveApp = ForegroundAppDetector.getLastUserForegroundPackage(this, activeApps)
                    ?: ForegroundAppDetector.getForegroundPackage(this)?.takeIf { activeApps.contains(it) }
                NetworkSettingsHelper.openAirplaneSettings(this)
            }
        }
    }

    private fun handleAutoUnisolateOnReconnect() {
        if (!PreferencesManager.isAutoUnisolateOnReconnect(this)) return

        val mode = PreferencesManager.getIsolationMode(this)
        val selectedApps = PreferencesManager.getBubblePackages(this)
        val activeApps = PreferencesManager.getActivePackages(this)

        if (activeApps.isEmpty()) return

        if (mode == PreferencesManager.ISOLATION_MODE_FOREGROUND) {
            val currentFg = ForegroundAppDetector.getForegroundPackage(this)
            var targetPkg: String? = if (currentFg != null && activeApps.contains(currentFg)) {
                currentFg
            } else {
                null
            }

            if (targetPkg == null && hudLastActiveApp != null && activeApps.contains(hudLastActiveApp)) {
                targetPkg = hudLastActiveApp
            }

            if (targetPkg == null) {
                targetPkg = ForegroundAppDetector.getLastUserForegroundPackage(this, activeApps)
            }

            if (targetPkg == null && activeApps.size == 1) {
                targetPkg = activeApps.first()
            }

            if (targetPkg != null) {
                PreferencesManager.setPackageActive(this, targetPkg, false)
                hudLastActiveApp = null
                val appLabel = try {
                    val appInfo = packageManager.getApplicationInfo(targetPkg, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    targetPkg
                }
                Toast.makeText(
                    this,
                    "Đã tự động hủy cách ly cho $appLabel sau khi bật lại mạng",
                    Toast.LENGTH_SHORT
                ).show()
                updateQuickBallState()
            }
        } else {
            // Mode All: Only valid if exactly 1 app is selected
            if (selectedApps.size == 1) {
                val singlePkg = selectedApps.first()
                if (activeApps.contains(singlePkg) || PreferencesManager.isAppIsolationActive(this)) {
                    PreferencesManager.setPackageActive(this, singlePkg, false)
                    PreferencesManager.setAppIsolationActive(this, false)
                    hudLastActiveApp = null
                    val appLabel = try {
                        val appInfo = packageManager.getApplicationInfo(singlePkg, 0)
                        packageManager.getApplicationLabel(appInfo).toString()
                    } catch (_: Exception) {
                        singlePkg
                    }
                    Toast.makeText(
                        this,
                        "Đã tự động hủy cách ly cho $appLabel sau khi bật lại mạng",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateQuickBallState()
                }
            }
        }
    }

    private fun computeBubbleVisualIsolated(): Boolean {
        if (!PreferencesManager.isVpnEnabled(this)) return false
        if (!PreferencesManager.isAppIsolationActive(this)) return false

        val activeSet = PreferencesManager.getActivePackages(this)
        if (activeSet.isEmpty()) return false

        val mode = PreferencesManager.getIsolationMode(this)
        return if (mode == PreferencesManager.ISOLATION_MODE_FOREGROUND) {
            val fg = ForegroundAppDetector.getForegroundPackage(this)
            val bubbleSet = PreferencesManager.getBubblePackages(this)
            // If viewing a managed app, reflect that specific app's isolation state
            if (fg != null && bubbleSet.contains(fg)) {
                activeSet.contains(fg)
            } else {
                // When on Launcher, inside OwlNetGuard, or in unmanaged apps:
                // Since VPN is active and apps are isolated in background, reflect Active state (RED)
                true
            }
        } else {
            true
        }
    }

    private fun applyBubbleVisualState(visualIsolated: Boolean) {
        lastVisualIsolatedState = visualIsolated

        // 1. Collapsed Bubble visuals
        cardCollapsedBubble?.setBackgroundResource(
            if (visualIsolated) R.drawable.bg_floating_bubble_isolated else R.drawable.bg_floating_bubble
        )
        imgCollapsedOwl?.setImageResource(
            if (visualIsolated) R.drawable.logo_faceted_apex_isolated else R.drawable.logo_faceted_apex
        )

        // 2. Expanded Owl Button visuals
        btnActionIsolate?.setBackgroundResource(
            if (visualIsolated) R.drawable.bg_floating_bubble_isolated else R.drawable.bg_floating_bubble
        )
        imgBubbleIcon?.setImageResource(
            if (visualIsolated) R.drawable.logo_faceted_apex_isolated else R.drawable.logo_faceted_apex
        )

        // 3. Collapse chevron orientation based on dock side
        imgActionCollapse?.rotation = if (isDockedRight) 0f else 180f
    }

    private fun updateNetworkViews(netStatus: NetworkStatus) {
        val enabledActions = getEnabledActions()

        btnActionWifi?.visibility =
            if (enabledActions.contains(ActionType.WIFI)) View.VISIBLE else View.GONE
        imgActionWifi?.setImageResource(
            if (netStatus.isWifiEnabled) R.drawable.ic_wifi else R.drawable.ic_wifi_off
        )
        imgActionWifi?.alpha = if (netStatus.isWifiEnabled) 1.0f else 0.45f
        slashActionWifi?.visibility = if (netStatus.isWifiEnabled) View.GONE else View.VISIBLE

        btnActionData?.visibility =
            if (enabledActions.contains(ActionType.DATA)) View.VISIBLE else View.GONE
        imgActionData?.setImageResource(
            if (netStatus.isCellularConnected) R.drawable.ic_mobile_data else R.drawable.ic_mobile_data_off
        )
        imgActionData?.alpha = if (netStatus.isCellularConnected) 1.0f else 0.45f
        slashActionData?.visibility = if (netStatus.isCellularConnected) View.GONE else View.VISIBLE

        btnActionAirplane?.visibility =
            if (enabledActions.contains(ActionType.AIRPLANE)) View.VISIBLE else View.GONE
        imgActionAirplane?.setImageResource(
            if (netStatus.isAirplaneModeOn) R.drawable.ic_airplane else R.drawable.ic_airplane_off
        )
        imgActionAirplane?.alpha = if (netStatus.isAirplaneModeOn) 1.0f else 0.45f
        slashActionAirplane?.visibility = if (netStatus.isAirplaneModeOn) View.GONE else View.VISIBLE
    }

    fun updateQuickBallState() {
        if (!shouldShowBubble(this)) {
            removeQuickBall()
            return
        }

        NetworkSettingsHelper.updateStatus(this)
        val netStatus = NetworkSettingsHelper.status.value
        val visualIsolated = computeBubbleVisualIsolated()

        applyBubbleVisualState(visualIsolated)
        updateNetworkViews(netStatus)

        val enabledActions = getEnabledActions()
        btnActionIsolate?.visibility =
            if (enabledActions.contains(ActionType.ISOLATE)) View.VISIBLE else View.GONE

        // Switch collapsed / expanded views
        if (isExpanded) {
            cardCollapsedBubble?.visibility = View.GONE
            panelExpanded?.visibility = View.VISIBLE
        } else {
            cardCollapsedBubble?.visibility = View.VISIBLE
            panelExpanded?.visibility = View.GONE
        }

        // Dedicated Collapse button is always present in expanded state
        btnActionCollapse?.visibility = View.VISIBLE

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

    private fun startForegroundWithNotification() {
        val channelId = "owl_bubble_control_channel"
        val nm = getSystemService(NotificationManager::class.java)
        if (nm != null) {
            val channel = NotificationChannel(
                channelId,
                "Thanh điều khiển OwlNetGuard",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Điều khiển nhanh bong bóng nổi và cách ly ứng dụng"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }

        val isIsolated = PreferencesManager.isBubbleIsolated(this)
        val activeCount = PreferencesManager.getActivePackages(this).size

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPending = PendingIntent.getActivity(
            this,
            200,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIsolateIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_TOGGLE_ISOLATE
        }
        val toggleIsolatePending = PendingIntent.getBroadcast(
            this,
            201,
            toggleIsolateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hideBubbleIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_TOGGLE_BUBBLE_VISIBILITY
        }
        val hideBubblePending = PendingIntent.getBroadcast(
            this,
            202,
            hideBubbleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (isIsolated) {
            "Chế độ cách ly đang bật ($activeCount ứng dụng kích hoạt)"
        } else {
            "Bong bóng nổi đang trực ($activeCount ứng dụng kích hoạt)"
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("OwlNetGuard Bong bóng nổi")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentPending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                if (isIsolated) R.drawable.ic_shield_normal else R.drawable.ic_shield_isolated,
                if (isIsolated) "Tắt cách ly" else "Bật cách ly",
                toggleIsolatePending
            )
            .addAction(
                R.drawable.ic_bubble_inactive,
                "Tắt phím nổi",
                hideBubblePending
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

    override fun onDestroy() {
        serviceScope.cancel()
        try {
            unregisterReceiver(networkChangeReceiver)
        } catch (_: Exception) {
        }
        removeQuickBall()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001

        fun shouldShowBubble(context: Context): Boolean {
            return PreferencesManager.isBubbleOverlayEnabled(context) &&
                Settings.canDrawOverlays(context)
        }

        fun sync(context: Context) {
            val appCtx = context.applicationContext
            if (shouldShowBubble(appCtx)) {
                try {
                    val intent = Intent(appCtx, BubbleService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        appCtx.startForegroundService(intent)
                    } else {
                        appCtx.startService(intent)
                    }
                } catch (_: Exception) {
                }
            } else {
                try {
                    appCtx.stopService(Intent(appCtx, BubbleService::class.java))
                } catch (_: Exception) {
                }
            }
        }
    }
}
