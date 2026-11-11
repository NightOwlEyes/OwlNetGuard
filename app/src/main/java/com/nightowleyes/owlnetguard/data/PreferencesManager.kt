package com.nightowleyes.owlnetguard.data

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.net.VpnService
import android.service.quicksettings.TileService
import androidx.core.content.edit
import com.nightowleyes.owlnetguard.service.BubbleService
import com.nightowleyes.owlnetguard.service.LocalVpnService
import com.nightowleyes.owlnetguard.service.QuickTileService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PreferencesManager {
    private const val PREFS_NAME = "owl_netguard_prefs"
    private const val KEY_DEFAULTS_INIT = "defaults_initialized_v8"
    private const val KEY_VPN_ENABLED = "vpn_background_enabled"
    private const val KEY_ISOLATION_ACTIVE = "app_isolation_active"
    private const val KEY_SHOW_BUBBLE = "show_floating_bubble"
    private const val KEY_SHOW_BUBBLE_ISOLATE = "show_bubble_isolate"
    private const val KEY_SHOW_BUBBLE_WIFI = "show_bubble_wifi"
    private const val KEY_SHOW_BUBBLE_DATA = "show_bubble_data"
    private const val KEY_SHOW_BUBBLE_AIRPLANE = "show_bubble_airplane"
    private const val KEY_AUTO_UNISOLATE_ON_RECONNECT = "auto_unisolate_on_reconnect"
    private const val KEY_BUBBLE_PACKAGES = "bubble_packages"
    private const val KEY_ACTIVE_PACKAGES = "active_bubble_packages"
    private const val KEY_ISOLATION_MODE = "isolation_mode"
    private const val KEY_EXCLUDED_OVERLAY_PACKAGES = "excluded_overlay_packages"

    const val ISOLATION_MODE_ALL = 0
    const val ISOLATION_MODE_FOREGROUND = 1

    private val _stateVersion = MutableStateFlow(0L)
    val stateVersion: StateFlow<Long> = _stateVersion.asStateFlow()

    private fun prefs(context: Context): SharedPreferences {
        val appContext = context.applicationContext
        val sp = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ensureDefaults(sp)
        return sp
    }

    @Synchronized
    private fun ensureDefaults(sp: SharedPreferences) {
        if (!sp.getBoolean(KEY_DEFAULTS_INIT, false)) {
            val existingBubble = LinkedHashSet(sp.getStringSet(KEY_BUBBLE_PACKAGES, emptySet()) ?: emptySet())
            @Suppress("SpellCheckingInspection")
            existingBubble.remove("com.supercell.hayday")
            val existingActive = sp.getStringSet(KEY_ACTIVE_PACKAGES, null)?.let { LinkedHashSet(it) }
                ?: LinkedHashSet(existingBubble)
            @Suppress("SpellCheckingInspection")
            existingActive.remove("com.supercell.hayday")

            sp.edit {
                putStringSet(KEY_BUBBLE_PACKAGES, existingBubble)
                putStringSet(KEY_ACTIVE_PACKAGES, existingActive)
                putBoolean(KEY_VPN_ENABLED, sp.getBoolean(KEY_VPN_ENABLED, false))
                putBoolean(KEY_ISOLATION_ACTIVE, sp.getBoolean(KEY_ISOLATION_ACTIVE, true))
                putBoolean(KEY_SHOW_BUBBLE, sp.getBoolean(KEY_SHOW_BUBBLE, false))
                putBoolean(KEY_SHOW_BUBBLE_ISOLATE, sp.getBoolean(KEY_SHOW_BUBBLE_ISOLATE, true))
                putBoolean(KEY_SHOW_BUBBLE_WIFI, sp.getBoolean(KEY_SHOW_BUBBLE_WIFI, true))
                putBoolean(KEY_SHOW_BUBBLE_DATA, sp.getBoolean(KEY_SHOW_BUBBLE_DATA, true))
                putBoolean(KEY_SHOW_BUBBLE_AIRPLANE, sp.getBoolean(KEY_SHOW_BUBBLE_AIRPLANE, false))
                putInt(KEY_ISOLATION_MODE, sp.getInt(KEY_ISOLATION_MODE, ISOLATION_MODE_ALL))
                putBoolean(KEY_AUTO_UNISOLATE_ON_RECONNECT, sp.getBoolean(KEY_AUTO_UNISOLATE_ON_RECONNECT, true))
                if (!sp.contains(KEY_EXCLUDED_OVERLAY_PACKAGES)) {
                    putStringSet(KEY_EXCLUDED_OVERLAY_PACKAGES, setOf("com.facebook.orca", "com.zing.zalo"))
                }
                putBoolean(KEY_DEFAULTS_INIT, true)
            }
        }
    }

    fun notifyStateChanged() {
        _stateVersion.value = System.currentTimeMillis()
    }

    // --- 1. Master Isolation Switch ("Cách ly ứng dụng") ---

    fun isVpnEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_VPN_ENABLED, false)
    }

    fun setVpnEnabled(context: Context, enabled: Boolean) {
        if (!enabled) {
            foregroundTunnelPackage = null
        }
        prefs(context).edit {
            putBoolean(KEY_VPN_ENABLED, enabled)
            if (enabled) {
                putBoolean(KEY_ISOLATION_ACTIVE, true)
            }
        }
        syncIsolation(context)
    }



    // --- 2. Isolation Sub-State (Controlled by Floating Bubble without stopping background service) ---

    fun isAppIsolationActive(context: Context): Boolean {
        return isVpnEnabled(context) && prefs(context).getBoolean(KEY_ISOLATION_ACTIVE, true)
    }

    fun setAppIsolationActive(context: Context, active: Boolean) {
        if (!isVpnEnabled(context)) return
        val currentActive = getActivePackages(context)
        val bubble = getBubblePackages(context)
        prefs(context).edit {
            putBoolean(KEY_ISOLATION_ACTIVE, active)
            if (active && currentActive.isEmpty() && bubble.isNotEmpty()) {
                putStringSet(KEY_ACTIVE_PACKAGES, LinkedHashSet(bubble))
            }
        }
        syncIsolation(context)
    }

    fun toggleAppIsolationActive(context: Context): Boolean {
        val newActive = !isAppIsolationActive(context)
        setAppIsolationActive(context, newActive)
        return newActive
    }

    // Compatibility aliases for Bubble & Tile toggles
    fun isBubbleIsolated(context: Context): Boolean = isAppIsolationActive(context)

    fun toggleBubbleIsolation(context: Context): Boolean = toggleAppIsolationActive(context)

    // --- 3. Floating Bubble HUD Overlay ("Bong bóng nổi thông minh") ---

    fun isBubbleOverlayEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_SHOW_BUBBLE, false)
    }

    fun setBubbleOverlayEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit {
            putBoolean(KEY_SHOW_BUBBLE, enabled)
            if (!enabled && getIsolationMode(context) == ISOLATION_MODE_FOREGROUND) {
                putInt(KEY_ISOLATION_MODE, ISOLATION_MODE_ALL)
                val bubble = getBubblePackages(context)
                putStringSet(KEY_ACTIVE_PACKAGES, LinkedHashSet(bubble))
            }
        }
        if (!enabled) {
            foregroundTunnelPackage = null
        }
        BubbleService.sync(context)
        notifyStateChanged()
    }

    fun isBubbleIsolateVisible(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_BUBBLE_ISOLATE, true)

    fun setBubbleIsolateVisible(context: Context, visible: Boolean) {
        prefs(context).edit { putBoolean(KEY_SHOW_BUBBLE_ISOLATE, visible) }
        BubbleService.sync(context)
        notifyStateChanged()
    }

    fun isBubbleWifiVisible(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_BUBBLE_WIFI, true)

    fun setBubbleWifiVisible(context: Context, visible: Boolean) {
        prefs(context).edit { putBoolean(KEY_SHOW_BUBBLE_WIFI, visible) }
        BubbleService.sync(context)
        notifyStateChanged()
    }

    fun isBubbleDataVisible(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_BUBBLE_DATA, true)

    fun setBubbleDataVisible(context: Context, visible: Boolean) {
        prefs(context).edit { putBoolean(KEY_SHOW_BUBBLE_DATA, visible) }
        BubbleService.sync(context)
        notifyStateChanged()
    }

    fun isBubbleAirplaneVisible(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_BUBBLE_AIRPLANE, false)

    fun setBubbleAirplaneVisible(context: Context, visible: Boolean) {
        prefs(context).edit { putBoolean(KEY_SHOW_BUBBLE_AIRPLANE, visible) }
        BubbleService.sync(context)
        notifyStateChanged()
    }

    fun isAutoUnisolateOnReconnect(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_UNISOLATE_ON_RECONNECT, true)

    fun setAutoUnisolateOnReconnect(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_AUTO_UNISOLATE_ON_RECONNECT, enabled) }
        notifyStateChanged()
    }

    // --- 4. Isolation Mode (ALL vs FOREGROUND) ---

    @Volatile
    private var foregroundTunnelPackage: String? = null

    fun getForegroundTunnelPackage(): String? = foregroundTunnelPackage

    fun setForegroundTunnelPackage(context: Context, packageName: String?) {
        if (foregroundTunnelPackage == packageName) return
        foregroundTunnelPackage = packageName
        val sp = prefs(context)
        if (packageName != null) {
            sp.edit {
                putStringSet(KEY_ACTIVE_PACKAGES, setOf(packageName))
                putBoolean(KEY_ISOLATION_ACTIVE, true)
            }
        } else {
            sp.edit {
                putStringSet(KEY_ACTIVE_PACKAGES, emptySet())
            }
        }
        syncIsolation(context)
    }

    fun getIsolationMode(context: Context): Int =
        prefs(context).getInt(KEY_ISOLATION_MODE, ISOLATION_MODE_ALL)

    fun setIsolationMode(context: Context, mode: Int) {
        prefs(context).edit {
            putInt(KEY_ISOLATION_MODE, mode)
            if (mode == ISOLATION_MODE_FOREGROUND) {
                // Chế độ 'Chỉ app đang mở': tất cả các app chuyển về trạng thái 'Đang tạm dừng'
                // Khi chuyển qua chỉ app đang mở thì không có app nào được đưa vào đường hầm mạng cả
                putStringSet(KEY_ACTIVE_PACKAGES, emptySet())
                putBoolean(KEY_ISOLATION_ACTIVE, true)
            } else {
                // Chế độ 'Tất cả app đã chọn': tất cả app trong danh sách chuyển sang 'Đang cách ly'
                val bubble = getBubblePackages(context)
                putStringSet(KEY_ACTIVE_PACKAGES, LinkedHashSet(bubble))
                putBoolean(KEY_ISOLATION_ACTIVE, true)
            }
        }
        foregroundTunnelPackage = null
        syncIsolation(context)
    }

    // --- 4b. Excluded Overlay Packages (Bong bóng chat, ứng dụng nổi) ---

    fun getExcludedOverlayPackages(context: Context): Set<String> {
        val set = prefs(context).getStringSet(KEY_EXCLUDED_OVERLAY_PACKAGES, null)
        return if (set != null) LinkedHashSet(set) else setOf("com.facebook.orca", "com.zing.zalo")
    }

    fun setExcludedOverlayPackages(context: Context, packages: Set<String>) {
        prefs(context).edit {
            putStringSet(KEY_EXCLUDED_OVERLAY_PACKAGES, LinkedHashSet(packages))
        }
        notifyStateChanged()
    }

    fun toggleExcludedOverlayPackage(context: Context, packageName: String): Boolean {
        val current = LinkedHashSet(getExcludedOverlayPackages(context))
        val added = if (current.contains(packageName)) {
            current.remove(packageName)
            false
        } else {
            current.add(packageName)
            true
        }
        setExcludedOverlayPackages(context, current)
        return added
    }

    fun clearActivePackages(context: Context) {
        foregroundTunnelPackage = null
        prefs(context).edit {
            putStringSet(KEY_ACTIVE_PACKAGES, emptySet())
        }
        syncIsolation(context)
    }

    fun setActiveForegroundApp(context: Context, packageName: String) {
        foregroundTunnelPackage = packageName
        prefs(context).edit {
            putStringSet(KEY_ACTIVE_PACKAGES, setOf(packageName))
            putBoolean(KEY_ISOLATION_ACTIVE, true)
        }
        syncIsolation(context)
    }

    // --- 5. Selected & Active Packages ---

    fun getBubblePackages(context: Context): Set<String> {
        val set = prefs(context).getStringSet(KEY_BUBBLE_PACKAGES, emptySet())
        return if (set != null) LinkedHashSet(set) else emptySet()
    }

    fun getActivePackages(context: Context): Set<String> {
        val bubble = getBubblePackages(context)
        val active = prefs(context).getStringSet(KEY_ACTIVE_PACKAGES, emptySet()) ?: emptySet()
        return active.filterTo(LinkedHashSet()) { bubble.contains(it) }
    }

    fun pruneUninstalledPackages(context: Context, installedPackages: Set<String>) {
        val currentBubble = LinkedHashSet(getBubblePackages(context))
        val currentActive = LinkedHashSet(getActivePackages(context))
        val retainedBubble = currentBubble.filterTo(LinkedHashSet()) { installedPackages.contains(it) }
        val retainedActive = currentActive.filterTo(LinkedHashSet()) { retainedBubble.contains(it) }

        if (retainedBubble.size != currentBubble.size || retainedActive.size != currentActive.size) {
            prefs(context).edit {
                putStringSet(KEY_BUBBLE_PACKAGES, retainedBubble)
                putStringSet(KEY_ACTIVE_PACKAGES, retainedActive)
            }
            syncIsolation(context)
        }
    }

    /**
     * Returns the packages that should currently have their traffic routed into the sinkhole.
     * All activated apps in `activePackages` are actively isolated ("ĐANG CÁCH LY").
     * Apps marked "ĐANG TẠM DỪNG" retain normal internet access.
     */
    fun getEffectiveBlockedPackages(context: Context): Set<String> {
        if (!isAppIsolationActive(context)) return emptySet()
        val mode = getIsolationMode(context)
        return if (mode == ISOLATION_MODE_FOREGROUND) {
            val fg = foregroundTunnelPackage
            if (fg != null && getActivePackages(context).contains(fg)) {
                setOf(fg)
            } else {
                emptySet()
            }
        } else {
            getActivePackages(context)
        }
    }

    fun togglePackageInBubble(context: Context, packageName: String): Boolean {
        val sp = prefs(context)
        val currentBubble = LinkedHashSet(getBubblePackages(context))
        val currentActive = LinkedHashSet(getActivePackages(context))
        val added: Boolean
        if (currentBubble.contains(packageName)) {
            currentBubble.remove(packageName)
            currentActive.remove(packageName)
            added = false
        } else {
            currentBubble.add(packageName)
            currentActive.add(packageName)
            added = true
        }

        sp.edit {
            putStringSet(KEY_BUBBLE_PACKAGES, currentBubble)
            putStringSet(KEY_ACTIVE_PACKAGES, currentActive)
        }

        syncIsolation(context)
        return added
    }

    fun removePackageFromBubble(context: Context, packageName: String) {
        val sp = prefs(context)
        val currentBubble = LinkedHashSet(getBubblePackages(context))
        val currentActive = LinkedHashSet(getActivePackages(context))
        if (currentBubble.remove(packageName)) {
            currentActive.remove(packageName)
            sp.edit {
                putStringSet(KEY_BUBBLE_PACKAGES, currentBubble)
                putStringSet(KEY_ACTIVE_PACKAGES, currentActive)
            }
            syncIsolation(context)
        }
    }

    fun togglePackageActive(context: Context, packageName: String): Boolean {
        val sp = prefs(context)
        val currentBubble = LinkedHashSet(getBubblePackages(context))
        if (!currentBubble.contains(packageName)) return false

        val currentActive = LinkedHashSet(getActivePackages(context))
        val nowActive: Boolean
        if (currentActive.contains(packageName)) {
            currentActive.remove(packageName)
            nowActive = false
        } else {
            currentActive.add(packageName)
            nowActive = true
        }

        sp.edit {
            putStringSet(KEY_ACTIVE_PACKAGES, currentActive)
            if (nowActive || currentActive.isNotEmpty()) {
                putBoolean(KEY_ISOLATION_ACTIVE, true)
            }
        }

        syncIsolation(context)
        return nowActive
    }

    fun setPackageActive(context: Context, packageName: String, active: Boolean) {
        val sp = prefs(context)
        val currentBubble = LinkedHashSet(getBubblePackages(context))
        if (!currentBubble.contains(packageName)) return

        val currentActive = LinkedHashSet(getActivePackages(context))
        val changed = if (active) currentActive.add(packageName) else currentActive.remove(packageName)
        if (changed) {
            sp.edit {
                putStringSet(KEY_ACTIVE_PACKAGES, currentActive)
                if (active || currentActive.isNotEmpty()) {
                    putBoolean(KEY_ISOLATION_ACTIVE, true)
                }
            }
            syncIsolation(context)
        }
    }

    /**
     * Unified isolation synchronizer.
     * Manages the Local VPN Sinkhole Service based on Master Isolation state.
     */
    fun syncIsolation(context: Context) {
        val enabled = isVpnEnabled(context)
        if (enabled) {
            LocalVpnService.applyOrStop(context)
        } else {
            LocalVpnService.stop(context)
        }

        BubbleService.sync(context)
        requestTileUpdate(context)
        notifyStateChanged()
    }

    fun syncAllServices(context: Context) {
        syncIsolation(context)
    }

    fun isVpnPermissionGranted(context: Context): Boolean {
        return VpnService.prepare(context) == null
    }

    fun requestTileUpdate(context: Context) {
        try {
            TileService.requestListeningState(
                context.applicationContext,
                ComponentName(context.applicationContext, QuickTileService::class.java)
            )
        } catch (_: Exception) {
        }
    }
}
