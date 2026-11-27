package com.nightowleyes.owlnetguard.ui

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
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
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nightowleyes.owlnetguard.R
import com.nightowleyes.owlnetguard.data.AppInfo
import com.nightowleyes.owlnetguard.data.PreferencesManager
import com.nightowleyes.owlnetguard.service.QuickTileService
import com.nightowleyes.owlnetguard.ui.components.EmptySelectedAppsView
import com.nightowleyes.owlnetguard.ui.components.FacetedAmbientBackground
import com.nightowleyes.owlnetguard.ui.components.FloatingHudSettingsCard
import com.nightowleyes.owlnetguard.ui.components.HeaderSection
import com.nightowleyes.owlnetguard.ui.components.LoadingStateView
import com.nightowleyes.owlnetguard.ui.components.QuickTileSuggestionBanner
import com.nightowleyes.owlnetguard.ui.components.SelectedAppsHeaderBar
import com.nightowleyes.owlnetguard.ui.components.SwipeableSelectedAppItem
import com.nightowleyes.owlnetguard.ui.components.UpdateAvailableBanner
import com.nightowleyes.owlnetguard.ui.dialogs.AppPickerDialog
import com.nightowleyes.owlnetguard.ui.dialogs.AvailableUpdateInfo
import com.nightowleyes.owlnetguard.ui.dialogs.CheckUpdateDialog
import com.nightowleyes.owlnetguard.ui.dialogs.DisruptInfoDialog
import com.nightowleyes.owlnetguard.ui.dialogs.DonateDialog
import com.nightowleyes.owlnetguard.ui.dialogs.ExcludedAppsDialog
import com.nightowleyes.owlnetguard.ui.dialogs.InfoDialogType
import com.nightowleyes.owlnetguard.ui.dialogs.OpenSourceLicensesDialog
import com.nightowleyes.owlnetguard.ui.dialogs.PrivacyPolicyDialog
import com.nightowleyes.owlnetguard.ui.dialogs.UsageAccessPromptDialog
import com.nightowleyes.owlnetguard.ui.dialogs.fetchAvailableUpdate
import com.nightowleyes.owlnetguard.ui.theme.SlateBackground
import com.nightowleyes.owlnetguard.utils.AppListLoader
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
    var showDisruptInfoDialog by remember { mutableStateOf(false) }

    var isPopupEnabled by remember { mutableStateOf(PreferencesManager.isPopupEnabled(context)) }
    var isBubbleIsolated by remember { mutableStateOf(PreferencesManager.isBubbleIsolated(context)) }
    var bubblePackages by remember { mutableStateOf(PreferencesManager.getBubblePackages(context)) }
    var activePackages by remember { mutableStateOf(PreferencesManager.getActivePackages(context)) }
    var isolationMode by remember { mutableIntStateOf(PreferencesManager.getIsolationMode(context)) }

    var canDrawOverlays by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isVpnGranted by remember { mutableStateOf(PreferencesManager.isVpnPermissionGranted(context)) }
    var hasUsageStatsPermission by remember { mutableStateOf(ForegroundAppDetector.hasUsageStatsPermission(context)) }

    var showBubbleIsolate by remember { mutableStateOf(PreferencesManager.isBubbleIsolateVisible(context)) }
    var showBubbleDisrupt by remember { mutableStateOf(PreferencesManager.isBubbleDisruptVisible(context)) }
    var isAutoUnisolateOnReconnect by remember { mutableStateOf(PreferencesManager.isAutoUnisolateOnReconnect(context)) }
    var excludedOverlayPackages by remember { mutableStateOf(PreferencesManager.getExcludedOverlayPackages(context)) }
    var showExcludedAppsDialog by remember { mutableStateOf(false) }

    var isQsTileSuggestionDismissed by remember {
        mutableStateOf(PreferencesManager.isQsTileSuggestionDismissed(context))
    }
    var availableUpdate by remember { mutableStateOf<AvailableUpdateInfo?>(null) }

    fun refreshPrefsState() {
        isPopupEnabled = PreferencesManager.isPopupEnabled(context)
        isBubbleIsolated = PreferencesManager.isBubbleIsolated(context)
        bubblePackages = PreferencesManager.getBubblePackages(context)
        activePackages = PreferencesManager.getActivePackages(context)
        isolationMode = PreferencesManager.getIsolationMode(context)

        canDrawOverlays = Settings.canDrawOverlays(context)
        isVpnGranted = PreferencesManager.isVpnPermissionGranted(context)
        hasUsageStatsPermission = ForegroundAppDetector.hasUsageStatsPermission(context)

        showBubbleIsolate = PreferencesManager.isBubbleIsolateVisible(context)
        showBubbleDisrupt = PreferencesManager.isBubbleDisruptVisible(context)
        isAutoUnisolateOnReconnect = PreferencesManager.isAutoUnisolateOnReconnect(context)
        excludedOverlayPackages = PreferencesManager.getExcludedOverlayPackages(context)
        isQsTileSuggestionDismissed = PreferencesManager.isQsTileSuggestionDismissed(context)
    }

    LaunchedEffect(stateVersion) {
        refreshPrefsState()
    }

    // Tự động kiểm tra bản cập nhật mới từ GitHub khi người dùng mở app
    LaunchedEffect(Unit) {
        val update = fetchAvailableUpdate()
        if (update != null) {
            availableUpdate = update
            if (PreferencesManager.getDismissedUpdateVersion(context) != update.version &&
                activeDialog == InfoDialogType.NONE
            ) {
                activeDialog = InfoDialogType.CHECK_UPDATE
            }
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        val loaded = AppListLoader(context).loadApps()
        val installedSet = loaded.mapTo(LinkedHashSet()) { it.packageName }
        PreferencesManager.pruneUninstalledPackages(context, installedSet)
        allApps = loaded
        isLoading = false
        refreshPrefsState()

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
            if (PreferencesManager.isVpnPermissionGranted(context)) {
                PreferencesManager.setPopupEnabled(context, true)
            } else {
                onRequestVpnPermission()
            }
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

    fun requestAddQuickSettingsTile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val statusBarManager = context.getSystemService(StatusBarManager::class.java)
                statusBarManager?.requestAddTileService(
                    ComponentName(context, QuickTileService::class.java),
                    "OwlNetGuard",
                    Icon.createWithResource(context, R.drawable.ic_qs_tile_owl),
                    ContextCompat.getMainExecutor(context)
                ) { resultCode ->
                    when (resultCode) {
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> {
                            PreferencesManager.setQsTileSuggestionDismissed(context, true)
                            Toast.makeText(context, "Đã thêm OwlNetGuard vào Cài đặt nhanh!", Toast.LENGTH_SHORT).show()
                        }
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> {
                            PreferencesManager.setQsTileSuggestionDismissed(context, true)
                            Toast.makeText(context, "OwlNetGuard đã có sẵn trên thanh Cài đặt nhanh.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (_: Exception) {
                Toast.makeText(
                    context,
                    "Kéo thanh thông báo xuống -> Chỉnh sửa -> Kéo OwlNetGuard lên Cài đặt nhanh",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(
                context,
                "Kéo thanh thông báo xuống -> Chỉnh sửa -> Kéo OwlNetGuard lên Cài đặt nhanh",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val isPopupActive = isPopupEnabled && canDrawOverlays && isVpnGranted
    val isAppIsolatedOverall = isPopupActive && isBubbleIsolated
    val selectedApps = remember(allApps, bubblePackages, activePackages, isAppIsolatedOverall) {
        allApps
            .filter { bubblePackages.contains(it.packageName) }
            .map { app ->
                app.copy(
                    isInBubble = true,
                    isBlocked = isAppIsolatedOverall && activePackages.contains(app.packageName)
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
            FacetedAmbientBackground()

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                HeaderSection(
                    isIsolated = isPopupActive && activeSelectedCount > 0 && isBubbleIsolated,
                    showInfoMenu = showInfoMenu,
                    onToggleInfoMenu = { showInfoMenu = !showInfoMenu },
                    onDismissInfoMenu = { showInfoMenu = false },
                    onSelectDialog = { dialogType ->
                        showInfoMenu = false
                        activeDialog = dialogType
                    },
                    onOpenDonate = {
                        showInfoMenu = false
                        activeDialog = InfoDialogType.DONATE
                    }
                )

                val controlsContent: @Composable () -> Unit = {
                    // 1. Banner thông báo cập nhật mới (chỉ hiện khi phát hiện bản mới trên GitHub)
                    availableUpdate?.let { update ->
                        UpdateAvailableBanner(
                            updateInfo = update,
                            onOpenUpdate = { activeDialog = InfoDialogType.CHECK_UPDATE },
                            onDismiss = {
                                PreferencesManager.setDismissedUpdateVersion(context, update.version)
                                availableUpdate = null
                            }
                        )
                    }

                    // 2. Đề xuất thêm ô Cài đặt nhanh ở đầu ứng dụng (có thể xóa đi)
                    if (!isQsTileSuggestionDismissed) {
                        QuickTileSuggestionBanner(
                            onAddTile = { requestAddQuickSettingsTile() },
                            onDismiss = {
                                PreferencesManager.setQsTileSuggestionDismissed(context, true)
                            }
                        )
                    }

                    // 3. Thẻ điều khiển chính: Phím nổi (HUD) — Popup bật = VPN bật
                    FloatingHudSettingsCard(
                        isPopupActive = isPopupActive,
                        canDrawOverlays = canDrawOverlays,
                        isVpnGranted = isVpnGranted,
                        activeAppsCount = activeSelectedCount,
                        totalSelectedCount = selectedApps.size,
                        showIsolate = showBubbleIsolate,
                        showDisrupt = showBubbleDisrupt,
                        isolationMode = isolationMode,
                        hasUsageStatsPermission = hasUsageStatsPermission,
                        isAutoUnisolate = isAutoUnisolateOnReconnect,
                        excludedAppsCount = excludedOverlayPackages.size,
                        onToggleMasterPopup = { checked ->
                            if (!checked) {
                                PreferencesManager.setPopupEnabled(context, false)
                            } else {
                                if (!Settings.canDrawOverlays(context)) {
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
                                } else if (!PreferencesManager.isVpnPermissionGranted(context)) {
                                    onRequestVpnPermission()
                                } else {
                                    PreferencesManager.setPopupEnabled(context, true)
                                }
                            }
                        },
                        onToggleIsolate = { checked ->
                            val ok = PreferencesManager.setBubbleIsolateVisible(context, checked)
                            if (!ok) {
                                Toast.makeText(
                                    context,
                                    "Phải giữ ít nhất 1 nút hiển thị trên phím nổi!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onToggleDisrupt = { checked ->
                            val ok = PreferencesManager.setBubbleDisruptVisible(context, checked)
                            if (!ok) {
                                Toast.makeText(
                                    context,
                                    "Phải giữ ít nhất 1 nút hiển thị trên phím nổi!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onShowDisruptInfo = { showDisruptInfoDialog = true },
                        onToggleAutoUnisolate = { PreferencesManager.setAutoUnisolateOnReconnect(context, it) },
                        onSelectIsolationMode = { mode ->
                            PreferencesManager.setIsolationMode(context, mode)
                            if (mode == PreferencesManager.ISOLATION_MODE_FOREGROUND &&
                                !ForegroundAppDetector.hasUsageStatsPermission(context)
                            ) {
                                activeDialog = InfoDialogType.USAGE_ACCESS_PROMPT
                            }
                        },
                        onOpenExcludedAppsDialog = { showExcludedAppsDialog = true },
                        onRequestOverlayPermission = onRequestOverlayPermission,
                        onRequestUsageStatsPermission = {
                            activeDialog = InfoDialogType.USAGE_ACCESS_PROMPT
                        }
                    )
                }

                val appsContent: @Composable (Modifier) -> Unit = { paneModifier ->
                    Column(modifier = paneModifier) {
                        SelectedAppsHeaderBar(
                            selectedCount = selectedApps.size,
                            activeCount = activeSelectedCount,
                            onOpenAppPicker = { showAppPickerDialog = true }
                        )

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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        controlsContent()

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

            if (showExcludedAppsDialog) {
                ExcludedAppsDialog(
                    allApps = allApps,
                    excludedPackages = excludedOverlayPackages,
                    onToggleExcludedApp = { pkg ->
                        PreferencesManager.toggleExcludedOverlayPackage(context, pkg)
                        excludedOverlayPackages = PreferencesManager.getExcludedOverlayPackages(context)
                    },
                    onDismiss = { showExcludedAppsDialog = false }
                )
            }

            if (showDisruptInfoDialog) {
                DisruptInfoDialog(onDismiss = { showDisruptInfoDialog = false })
            }

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
                        onDismiss = {
                            availableUpdate?.let {
                                PreferencesManager.setDismissedUpdateVersion(context, it.version)
                            }
                            activeDialog = InfoDialogType.NONE
                        },
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
                InfoDialogType.DONATE -> {
                    DonateDialog(
                        onDismiss = { activeDialog = InfoDialogType.NONE },
                        onOpenSponsors = {
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, MainActivity.GITHUB_SPONSORS_URL.toUri())
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
