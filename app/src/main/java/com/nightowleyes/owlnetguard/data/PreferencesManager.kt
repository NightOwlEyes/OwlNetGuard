package com.nightowleyes.owlnetguard.data

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.net.VpnService
import android.provider.Settings
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
    private const val KEY_DEFAULTS_INIT = "defaults_initialized_v10"
    private const val KEY_VPN_ENABLED = "vpn_background_enabled"
    private const val KEY_ISOLATION_ACTIVE = "app_isolation_active"
    private const val KEY_SHOW_BUBBLE = "show_floating_bubble"
    private const val KEY_SHOW_BUBBLE_ISOLATE = "show_bubble_isolate"
    private const val KEY_SHOW_BUBBLE_DISRUPT = "show_bubble_disrupt"
    private const val KEY_AUTO_UNISOLATE_ON_RECONNECT = "auto_unisolate_on_reconnect"
    private const val KEY_BUBBLE_PACKAGES = "bubble_packages"
    private const val KEY_ACTIVE_PACKAGES = "active_bubble_packages"
    private const val KEY_ISOLATION_MODE = "isolation_mode"
    private const val KEY_EXCLUDED_OVERLAY_PACKAGES = "excluded_overlay_packages"
    private const val KEY_DISMISSED_QS_TILE_SUGGESTION = "dismissed_qs_tile_suggestion"
    private const val KEY_DISMISSED_UPDATE_VERSION = "dismissed_update_version"

    const val ISOLATION_MODE_ALL = 0
    const val ISOLATION_MODE_FOREGROUND = 1

    private val _stateVersion = MutableStateFlow(0L)
    val stateVersion: StateFlow<Long> = _stateVersion.asStateFlow()

    @Volatile
    private var foregroundTunnelPackage: String? = null

    @Volatile
    private var disruptingNetwork: Boolean = false

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

            // Đồng bộ 1-1: Popup bật tương đương VPN bật
            val syncedPopupOn = sp.getBoolean(KEY_VPN_ENABLED, false) && sp.getBoolean(KEY_SHOW_BUBBLE, false)

            sp.edit {
                putStringSet(KEY_BUBBLE_PACKAGES, existingBubble)
                putStringSet(KEY_ACTIVE_PACKAGES, if (syncedPopupOn) existingActive else emptySet())
                putBoolean(KEY_VPN_ENABLED, syncedPopupOn)
                putBoolean(KEY_SHOW_BUBBLE, syncedPopupOn)
                putBoolean(KEY_ISOLATION_ACTIVE, syncedPopupOn)
                putBoolean(KEY_SHOW_BUBBLE_ISOLATE, sp.getBoolean(KEY_SHOW_BUBBLE_ISOLATE, true))
                putBoolean(KEY_SHOW_BUBBLE_DISRUPT, sp.getBoolean(KEY_SHOW_BUBBLE_DISRUPT, true))
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

    // --- 1. Unified Popup & VPN Master Switch (Popup bật = VPN bật, Popup tắt = VPN tắt) ---

    fun isPopupEnabled(context: Context): Boolean {
        val sp = prefs(context)
        return sp.getBoolean(KEY_SHOW_BUBBLE, false) && sp.getBoolean(KEY_VPN_ENABLED, false)
    }

    fun setPopupEnabled(context: Context, enabled: Boolean) {
        val sp = prefs(context)
        val currentMode = getIsolationMode(context)
        val bubble = getBubblePackages(context)

        if (!enabled) {
            foregroundTunnelPackage = null
            disruptingNetwork = false
        }

        sp.edit {
            putBoolean(KEY_VPN_ENABLED, enabled)
            putBoolean(KEY_SHOW_BUBBLE, enabled)
            if (enabled) {
                if (currentMode == ISOLATION_MODE_ALL) {
                    putStringSet(KEY_ACTIVE_PACKAGES, LinkedHashSet(bubble))
                    putBoolean(KEY_ISOLATION_ACTIVE, true)
                } else {
                    putStringSet(KEY_ACTIVE_PACKAGES, emptySet())
                    putBoolean(KEY_ISOLATION_ACTIVE, true)
                }
            } else {
                putStringSet(KEY_ACTIVE_PACKAGES, emptySet())
                putBoolean(KEY_ISOLATION_ACTIVE, false)
            }
        }

        syncIsolation(context)
    }

    // Compatibility aliases so VPN & Bubble state are always 100% locked together
    fun isVpnEnabled(context: Context): Boolean = isPopupEnabled(context)

    fun setVpnEnabled(context: Context, enabled: Boolean) = setPopupEnabled(context, enabled)

    fun isBubbleOverlayEnabled(context: Context): Boolean = isPopupEnabled(context)

    fun setBubbleOverlayEnabled(context: Context, enabled: Boolean) = setPopupEnabled(context, enabled)

    // --- 2. Isolation Sub-State (Controlled by Floating Bubble) ---

    fun isAppIsolationActive(context: Context): Boolean {
        return isPopupEnabled(context) && prefs(context).getBoolean(KEY_ISOLATION_ACTIVE, true)
    }

    fun setAppIsolationActive(context: Context, active: Boolean) {
        if (!isPopupEnabled(context)) return
        val currentMode = getIsolationMode(context)
        if (currentMode == ISOLATION_MODE_ALL) {
            setAllPackagesActive(context, active)
        } else {
            prefs(context).edit {
                putBoolean(KEY_ISOLATION_ACTIVE, active)
            }
            syncIsolation(context)
        }
    }

    fun isBubbleIsolated(context: Context): Boolean = isAppIsolationActive(context)

    // --- 3. Floating Bubble HUD Buttons & Auto-Unisolate ---

    fun isBubbleIsolateVisible(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_BUBBLE_ISOLATE, true)

    fun setBubbleIsolateVisible(context: Context, visible: Boolean): Boolean {
        if (!visible && !isBubbleDisruptVisible(context)) {
            return false
        }
        prefs(context).edit { putBoolean(KEY_SHOW_BUBBLE_ISOLATE, visible) }
        BubbleService.sync(context)
        notifyStateChanged()
        return true
    }

    fun isBubbleDisruptVisible(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_BUBBLE_DISRUPT, true)

    fun setBubbleDisruptVisible(context: Context, visible: Boolean): Boolean {
        if (!visible && !isBubbleIsolateVisible(context)) {
            return false
        }
        prefs(context).edit { putBoolean(KEY_SHOW_BUBBLE_DISRUPT, visible) }
        BubbleService.sync(context)
        notifyStateChanged()
        return true
    }

    fun isAutoUnisolateOnReconnect(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_UNISOLATE_ON_RECONNECT, true)

    fun setAutoUnisolateOnReconnect(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_AUTO_UNISOLATE_ON_RECONNECT, enabled) }
        notifyStateChanged()
    }

    fun isQsTileSuggestionDismissed(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DISMISSED_QS_TILE_SUGGESTION, false)

    fun setQsTileSuggestionDismissed(context: Context, dismissed: Boolean) {
        prefs(context).edit { putBoolean(KEY_DISMISSED_QS_TILE_SUGGESTION, dismissed) }
        notifyStateChanged()
    }

    fun getDismissedUpdateVersion(context: Context): String =
        prefs(context).getString(KEY_DISMISSED_UPDATE_VERSION, "") ?: ""

    fun setDismissedUpdateVersion(context: Context, version: String) {
        prefs(context).edit { putString(KEY_DISMISSED_UPDATE_VERSION, version) }
    }

    // --- 3b. Network Disruption (Temporary Tunnel Ejection) ---

    fun isDisruptingNetwork(): Boolean = disruptingNetwork

    fun setDisruptingNetwork(context: Context, disrupting: Boolean) {
        if (disruptingNetwork == disrupting) return
        disruptingNetwork = disrupting
        if (isPopupEnabled(context)) {
            LocalVpnService.applyOrStop(context)
        }
        notifyStateChanged()
    }

    // --- 4. Isolation Mode (ALL vs FOREGROUND) ---

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
        val currentMode = getIsolationMode(context)
        if (currentMode == mode) return

        val sp = prefs(context)
        val bubble = getBubblePackages(context)
        val isPopupOn = isPopupEnabled(context)

        sp.edit {
            putInt(KEY_ISOLATION_MODE, mode)
            if (isPopupOn) {
                if (mode == ISOLATION_MODE_FOREGROUND) {
                    putStringSet(KEY_ACTIVE_PACKAGES, emptySet())
                    putBoolean(KEY_ISOLATION_ACTIVE, true)
                } else {
                    putStringSet(KEY_ACTIVE_PACKAGES, LinkedHashSet(bubble))
                    putBoolean(KEY_ISOLATION_ACTIVE, true)
                }
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
        val isPopupOn = isPopupEnabled(context)
        val mode = getIsolationMode(context)
        val added: Boolean

        if (currentBubble.contains(packageName)) {
            currentBubble.remove(packageName)
            currentActive.remove(packageName)
            added = false
        } else {
            currentBubble.add(packageName)
            if (isPopupOn && mode == ISOLATION_MODE_ALL && isAppIsolationActive(context)) {
                currentActive.add(packageName)
            } else {
                currentActive.remove(packageName)
            }
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

    fun setAllPackagesActive(context: Context, active: Boolean) {
        val bubble = getBubblePackages(context)
        val newActive = if (active) LinkedHashSet(bubble) else LinkedHashSet<String>()
        prefs(context).edit {
            putStringSet(KEY_ACTIVE_PACKAGES, newActive)
            putBoolean(KEY_ISOLATION_ACTIVE, active)
        }
        syncIsolation(context)
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
     * Unified synchronizer:
     * Popup ON == VPN ON, Popup OFF == VPN OFF.
     */
    fun syncIsolation(context: Context) {
        val enabled = isPopupEnabled(context) &&
            isVpnPermissionGranted(context) &&
            Settings.canDrawOverlays(context)

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
