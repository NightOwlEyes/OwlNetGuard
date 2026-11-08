package com.nightowleyes.owlnetguard.utils

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.core.net.toUri

object ForegroundAppDetector {

    @Volatile
    private var lastKnownForegroundPackage: String? = null

    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                ?: return false
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    fun openUsageAccessSettings(context: Context) {
        try {
            val intent = Intent(
                Settings.ACTION_USAGE_ACCESS_SETTINGS,
                "package:${context.packageName}".toUri()
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {
                Toast.makeText(
                    context,
                    "Không thể mở cài đặt Quyền truy cập dữ liệu sử dụng",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun getForegroundPackage(context: Context): String? {
        if (!hasUsageStatsPermission(context)) {
            return null
        }
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return lastKnownForegroundPackage
            val now = System.currentTimeMillis()
            // First check recent 2 minutes
            var events = usm.queryEvents(now - 120_000L, now)
            val event = UsageEvents.Event()
            var latestPkg: String? = null
            var latestTime = 0L

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    val pkg = event.packageName
                    if (!pkg.isNullOrBlank() &&
                        pkg != "com.android.systemui" &&
                        event.timeStamp >= latestTime
                    ) {
                        latestTime = event.timeStamp
                        latestPkg = pkg
                    }
                }
            }

            if (latestPkg != null) {
                lastKnownForegroundPackage = latestPkg
                return latestPkg
            }

            // Fallback: wider range 10 minutes
            events = usm.queryEvents(now - 600_000L, now)
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    val pkg = event.packageName
                    if (!pkg.isNullOrBlank() &&
                        pkg != "com.android.systemui" &&
                        event.timeStamp >= latestTime
                    ) {
                        latestTime = event.timeStamp
                        latestPkg = pkg
                    }
                }
            }

            if (latestPkg != null) {
                lastKnownForegroundPackage = latestPkg
                return latestPkg
            }

            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, now - 600_000L, now)
            val mostRecent = stats
                ?.filter {
                    it.packageName != "com.android.systemui" &&
                        it.lastTimeUsed > 0L
                }
                ?.maxByOrNull { it.lastTimeUsed }

            if (mostRecent != null) {
                lastKnownForegroundPackage = mostRecent.packageName
            }
            lastKnownForegroundPackage
        } catch (_: Exception) {
            lastKnownForegroundPackage
        }
    }

    /**
     * Determines whether a package belongs to Android system, settings, launcher, or OwlNetGuard itself.
     */
    fun isSystemOrSettingsOrLauncher(context: Context, packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return true
        if (packageName == "com.android.systemui" || packageName == context.packageName) return true
        if (packageName.contains("settings", ignoreCase = true)) return true
        if (packageName.contains("launcher", ignoreCase = true) || packageName.contains("quickstep", ignoreCase = true)) return true

        try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolveInfo = context.packageManager.resolveActivity(intent, 0)
            if (resolveInfo?.activityInfo?.packageName == packageName) {
                return true
            }
        } catch (_: Exception) {}
        return false
    }

    /**
     * Retrieves the most recently resumed user app (ignoring settings, system UI, and launchers)
     * optionally filtered by a set of target candidates (e.g. active isolated packages).
     */
    fun getLastUserForegroundPackage(context: Context, targetSet: Set<String>? = null): String? {
        if (!hasUsageStatsPermission(context)) return null
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
            val now = System.currentTimeMillis()
            val events = usm.queryEvents(now - 180_000L, now)
            val event = UsageEvents.Event()
            var latestTargetPkg: String? = null
            var latestTargetTime = 0L

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    val pkg = event.packageName
                    if (!pkg.isNullOrBlank() && !isSystemOrSettingsOrLauncher(context, pkg)) {
                        if (targetSet == null || targetSet.contains(pkg)) {
                            if (event.timeStamp >= latestTargetTime) {
                                latestTargetTime = event.timeStamp
                                latestTargetPkg = pkg
                            }
                        }
                    }
                }
            }
            latestTargetPkg
        } catch (_: Exception) {
            null
        }
    }
}
