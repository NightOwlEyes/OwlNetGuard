package com.nightowleyes.owlnetguard.ui

import android.content.Intent
import android.content.res.Configuration
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nightowleyes.owlnetguard.data.AppInfo
import com.nightowleyes.owlnetguard.utils.AppListLoader
import com.nightowleyes.owlnetguard.data.PreferencesManager
import com.nightowleyes.owlnetguard.ui.components.FacetedAmbientBackground
import com.nightowleyes.owlnetguard.ui.components.FloatingHudSettingsCard
import com.nightowleyes.owlnetguard.ui.components.HeaderSection
import com.nightowleyes.owlnetguard.ui.components.IsolationMasterCard
import com.nightowleyes.owlnetguard.ui.components.EmptySelectedAppsView
import com.nightowleyes.owlnetguard.ui.components.LoadingStateView
import com.nightowleyes.owlnetguard.ui.components.SelectedAppsHeaderBar
import com.nightowleyes.owlnetguard.ui.components.SwipeableSelectedAppItem
import com.nightowleyes.owlnetguard.ui.dialogs.AppPickerDialog
import com.nightowleyes.owlnetguard.ui.dialogs.CheckUpdateDialog
import com.nightowleyes.owlnetguard.ui.dialogs.InfoDialogType
import com.nightowleyes.owlnetguard.ui.dialogs.OpenSourceLicensesDialog
import com.nightowleyes.owlnetguard.ui.dialogs.PrivacyPolicyDialog
import com.nightowleyes.owlnetguard.ui.dialogs.UsageAccessPromptDialog
import com.nightowleyes.owlnetguard.ui.theme.SlateBackground
import com.nightowleyes.owlnetguard.utils.ForegroundAppDetector
import com.nightowleyes.owlnetguard.utils.NetworkSettingsHelper

@Composable
fun MainScreen(
    onRequestVpnPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val stateVersion by PreferencesManager.stateVersion.collectAsState()

    var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAppPickerDialog by remember { mutableStateOf(false) }

    var showInfoMenu by remember { mutableStateOf(false) }
    var activeDialog by remember { mutableStateOf(InfoDialogType.NONE) }

    var isVpnEnabled by remember { mutableStateOf(PreferencesManager.isVpnEnabled(context)) }
    var isBubbleOverlayEnabled by remember { mutableStateOf(PreferencesManager.isBubbleOverlayEnabled(context)) }
    var isBubbleIsolated by remember { mutableStateOf(PreferencesManager.isBubbleIsolated(context)) }
    var bubblePackages by remember { mutableStateOf(PreferencesManager.getBubblePackages(context)) }
    var activePackages by remember { mutableStateOf(PreferencesManager.getActivePackages(context)) }
    var isolationMode by remember { mutableIntStateOf(PreferencesManager.getIsolationMode(context)) }

    var canDrawOverlays by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasUsageStatsPermission by remember { mutableStateOf(ForegroundAppDetector.hasUsageStatsPermission(context)) }

    var showBubbleIsolate by remember { mutableStateOf(PreferencesManager.isBubbleIsolateVisible(context)) }
    var showBubbleWifi by remember { mutableStateOf(PreferencesManager.isBubbleWifiVisible(context)) }
    var showBubbleData by remember { mutableStateOf(PreferencesManager.isBubbleDataVisible(context)) }
    var showBubbleAirplane by remember { mutableStateOf(PreferencesManager.isBubbleAirplaneVisible(context)) }
    var isAutoUnisolateOnReconnect by remember { mutableStateOf(PreferencesManager.isAutoUnisolateOnReconnect(context)) }

    fun refreshPrefsState() {
        isVpnEnabled = PreferencesManager.isVpnEnabled(context)
        isBubbleOverlayEnabled = PreferencesManager.isBubbleOverlayEnabled(context)
        isBubbleIsolated = PreferencesManager.isBubbleIsolated(context)
        bubblePackages = PreferencesManager.getBubblePackages(context)
        activePackages = PreferencesManager.getActivePackages(context)
        isolationMode = PreferencesManager.getIsolationMode(context)

        canDrawOverlays = Settings.canDrawOverlays(context)
        hasUsageStatsPermission = ForegroundAppDetector.hasUsageStatsPermission(context)

        showBubbleIsolate = PreferencesManager.isBubbleIsolateVisible(context)
        showBubbleWifi = PreferencesManager.isBubbleWifiVisible(context)
        showBubbleData = PreferencesManager.isBubbleDataVisible(context)
        showBubbleAirplane = PreferencesManager.isBubbleAirplaneVisible(context)
        isAutoUnisolateOnReconnect = PreferencesManager.isAutoUnisolateOnReconnect(context)
    }

    LaunchedEffect(stateVersion) {
        refreshPrefsState()
    }

    LaunchedEffect(Unit) {
        isLoading = true
        val loaded = AppListLoader(context).loadApps()
        val installedSet = loaded.mapTo(LinkedHashSet()) { it.packageName }
        PreferencesManager.pruneUninstalledPackages(context, installedSet)
        allApps = loaded
        isLoading = false
        refreshPrefsState()

        // Proactively check if Foreground Only mode is selected without Usage Access permission
        if (PreferencesManager.getIsolationMode(context) == PreferencesManager.ISOLATION_MODE_FOREGROUND &&
            !ForegroundAppDetector.hasUsageStatsPermission(context)
        ) {
            activeDialog = InfoDialogType.USAGE_ACCESS_PROMPT
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                NetworkSettingsHelper.updateStatus(context)
                refreshPrefsState()
                PreferencesManager.syncAllServices(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val overlayLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshPrefsState()
        if (Settings.canDrawOverlays(context)) {
            PreferencesManager.setBubbleOverlayEnabled(context, true)
        }
    }

    val usageStatsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshPrefsState()
        if (ForegroundAppDetector.hasUsageStatsPermission(context)) {
            PreferencesManager.setIsolationMode(context, PreferencesManager.ISOLATION_MODE_FOREGROUND)
        }
    }

    fun launchUsageStatsSettings() {
        try {
            usageStatsLauncher.launch(
                Intent(
                    Settings.ACTION_USAGE_ACCESS_SETTINGS,
                    "package:${context.packageName}".toUri()
                )
            )
        } catch (_: Exception) {
            try {
                usageStatsLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            } catch (_: Exception) {
                ForegroundAppDetector.openUsageAccessSettings(context)
            }
        }
    }

    // Selected apps in the main list ("Ứng dụng đã chọn")
    val selectedApps = remember(allApps, bubblePackages, activePackages) {
        allApps
            .filter { bubblePackages.contains(it.packageName) }
            .map { app ->
                app.copy(
                    isInBubble = true,
                    isBlocked = activePackages.contains(app.packageName)
                )
            }
    }

    val activeSelectedCount = remember(selectedApps) {
        selectedApps.count { it.isBlocked }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SlateBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Ambient Low-Poly Faceted Prism Background Mesh
            FacetedAmbientBackground()

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Bar with Exact Faceted Apex Owl Logo + Info Menu
                HeaderSection(
                    isIsolated = isVpnEnabled && isBubbleIsolated,
                    showInfoMenu = showInfoMenu,
                    onToggleInfoMenu = { showInfoMenu = !showInfoMenu },
                    onDismissInfoMenu = { showInfoMenu = false },
                    onSelectDialog = { dialogType ->
                        showInfoMenu = false
                        activeDialog = dialogType
                    }
                )

                val controlsContent: @Composable () -> Unit = {
                    // 1. Top Section: Cách ly ứng dụng
                    IsolationMasterCard(
                        isVpnEnabled = isVpnEnabled,
                        isIsolationActive = isBubbleIsolated,
                        activeAppsCount = activeSelectedCount,
                        totalSelectedCount = selectedApps.size,
                        onToggleVpn = { enabled ->
                            if (enabled && !PreferencesManager.isVpnPermissionGranted(context)) {
                                onRequestVpnPermission()
                            } else {
                                PreferencesManager.setVpnEnabled(context, enabled)
                                if (enabled) {
                                    PreferencesManager.setAppIsolationActive(context, true)
                                } else {
                                    // Tự động làm mờ/tắt Phím nổi khi tắt Cách ly ứng dụng
                                    PreferencesManager.setBubbleOverlayEnabled(context, false)
                                }
                            }
                        }
                    )

                    // 2. Second Section: Phím nổi thông minh (HUD) & Cài đặt phụ thuộc
                    FloatingHudSettingsCard(
                        isVpnEnabled = isVpnEnabled,
                        totalSelectedCount = selectedApps.size,
                        canDrawOverlays = canDrawOverlays,
                        isOverlayEnabled = isBubbleOverlayEnabled,
                        showIsolate = showBubbleIsolate,
                        showWifi = showBubbleWifi,
                        showData = showBubbleData,
                        showAirplane = showBubbleAirplane,
                        isolationMode = isolationMode,
                        hasUsageStatsPermission = hasUsageStatsPermission,
                        isAutoUnisolate = isAutoUnisolateOnReconnect,
                        onToggleMasterOverlay = { checked ->
                            if (checked && !Settings.canDrawOverlays(context)) {
                                try {
                                    overlayLauncher.launch(
                                        Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            "package:${context.packageName}".toUri()
                                        )
                                    )
                                } catch (_: Exception) {
                                    onRequestOverlayPermission()
                                }
                            } else {
                                PreferencesManager.setBubbleOverlayEnabled(context, checked)
                            }
                        },
                        onToggleIsolate = { PreferencesManager.setBubbleIsolateVisible(context, it) },
                        onToggleWifi = { PreferencesManager.setBubbleWifiVisible(context, it) },
                        onToggleData = { PreferencesManager.setBubbleDataVisible(context, it) },
                        onToggleAirplane = { PreferencesManager.setBubbleAirplaneVisible(context, it) },
                        onToggleAutoUnisolate = { PreferencesManager.setAutoUnisolateOnReconnect(context, it) },
                        onSelectIsolationMode = { mode ->
                            PreferencesManager.setIsolationMode(context, mode)
                            if (mode == PreferencesManager.ISOLATION_MODE_FOREGROUND &&
                                !ForegroundAppDetector.hasUsageStatsPermission(context)
                            ) {
                                activeDialog = InfoDialogType.USAGE_ACCESS_PROMPT
                            }
                        },
                        onRequestOverlayPermission = onRequestOverlayPermission,
                        onRequestUsageStatsPermission = {
                            activeDialog = InfoDialogType.USAGE_ACCESS_PROMPT
                        }
                    )
                }

                val appsContent: @Composable (Modifier) -> Unit = { paneModifier ->
                    Column(modifier = paneModifier) {
                        // Unified Header: "Ứng dụng đã chọn" + Button "Chọn ứng dụng"
                        SelectedAppsHeaderBar(
                            selectedCount = selectedApps.size,
                            activeCount = activeSelectedCount,
                            onOpenAppPicker = { showAppPickerDialog = true }
                        )

                        // Selected Apps List (with Hexagon Active/Pause toggle & Swipe left/right to remove)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            when {
                                isLoading -> {
                                    LoadingStateView()
                                }
                                selectedApps.isEmpty() -> {
                                    EmptySelectedAppsView(
                                        onOpenAppPicker = { showAppPickerDialog = true }
                                    )
                                }
                                else -> {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(bottom = 24.dp, top = 4.dp)
                                    ) {
                                        items(
                                            items = selectedApps,
                                            key = { it.packageName }
                                        ) { app ->
                                            SwipeableSelectedAppItem(
                                                app = app,
                                                isVpnIsolated = isVpnEnabled && isBubbleIsolated,
                                                onToggleActive = {
                                                    PreferencesManager.togglePackageActive(
                                                        context,
                                                        app.packageName
                                                    )
                                                },
                                                onRemoveFromList = {
                                                    PreferencesManager.removePackageFromBubble(
                                                        context,
                                                        app.packageName
                                                    )
                                                    Toast.makeText(
                                                        context,
                                                        "Đã bỏ ${app.name} khỏi danh sách",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1.08f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                        ) {
                            controlsContent()
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        appsContent(
                            Modifier
                                .weight(0.92f)
                                .fillMaxHeight()
                        )
                    }
                } else {
                    // Portrait Mode: Single unified scrollable surface ("Làm phẳng mục ứng dụng đã chọn")
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        controlsContent()

                        // Unified Header: "Ứng dụng đã chọn" + Button "Chọn ứng dụng"
                        SelectedAppsHeaderBar(
                            selectedCount = selectedApps.size,
                            activeCount = activeSelectedCount,
                            onOpenAppPicker = { showAppPickerDialog = true }
                        )

                        when {
                            isLoading -> {
                                LoadingStateView()
                            }
                            selectedApps.isEmpty() -> {
                                EmptySelectedAppsView(
                                    onOpenAppPicker = { showAppPickerDialog = true }
                                )
                            }
                            else -> {
                                selectedApps.forEach { app ->
                                    key(app.packageName) {
                                        SwipeableSelectedAppItem(
                                            app = app,
                                            isVpnIsolated = isVpnEnabled && isBubbleIsolated,
                                            onToggleActive = {
                                                PreferencesManager.togglePackageActive(
                                                    context,
                                                    app.packageName
                                                )
                                            },
                                            onRemoveFromList = {
                                                PreferencesManager.removePackageFromBubble(
                                                    context,
                                                    app.packageName
                                                )
                                                Toast.makeText(
                                                    context,
                                                    "Đã bỏ ${app.name} khỏi danh sách",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }

            // App Picker Modal Dialog ("Bảng chọn ứng dụng")
            if (showAppPickerDialog) {
                AppPickerDialog(
                    allApps = allApps,
                    selectedPackages = bubblePackages,
                    onToggleSelectApp = { pkg ->
                        PreferencesManager.togglePackageInBubble(context, pkg)
                    },
                    onDismiss = { showAppPickerDialog = false }
                )
            }

            // Info & Permission Dialogs
            when (activeDialog) {
                InfoDialogType.USAGE_ACCESS_PROMPT -> {
                    UsageAccessPromptDialog(
                        onConfirm = {
                            activeDialog = InfoDialogType.NONE
                            launchUsageStatsSettings()
                        },
                        onDismiss = {
                            activeDialog = InfoDialogType.NONE
                            if (!ForegroundAppDetector.hasUsageStatsPermission(context)) {
                                PreferencesManager.setIsolationMode(
                                    context,
                                    PreferencesManager.ISOLATION_MODE_ALL
                                )
                            }
                        }
                    )
                }
                InfoDialogType.PRIVACY_POLICY -> {
                    PrivacyPolicyDialog(onDismiss = { activeDialog = InfoDialogType.NONE })
                }
                InfoDialogType.OPEN_SOURCE_LICENSES -> {
                    OpenSourceLicensesDialog(
                        onDismiss = { activeDialog = InfoDialogType.NONE },
                        onOpenGitHub = {
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, MainActivity.GITHUB_REPO_URL.toUri())
                                )
                            } catch (_: Exception) {
                            }
                        }
                    )
                }
                InfoDialogType.CHECK_UPDATE -> {
                    CheckUpdateDialog(
                        onDismiss = { activeDialog = InfoDialogType.NONE },
                        onOpenReleases = { url ->
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, url.toUri())
                                )
                            } catch (_: Exception) {
                            }
                        }
                    )
                }
                InfoDialogType.NONE -> {}
            }
        }
    }
}
