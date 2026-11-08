package com.nightowleyes.owlnetguard.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.nightowleyes.owlnetguard.data.AppInfo
import com.nightowleyes.owlnetguard.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppListLoader(private val context: Context) {

    suspend fun loadApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val ownPkg = context.packageName
        val activeSet = PreferencesManager.getActivePackages(context)
        val bubbleSet = PreferencesManager.getBubblePackages(context)

        // Discover all launchable packages via <queries> MAIN/LAUNCHER intent
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val launchableInfos = pm.queryIntentActivities(mainIntent, 0)
        val packageMap = LinkedHashMap<String, ApplicationInfo>()

        for (resolveInfo in launchableInfos) {
            val appInfo = resolveInfo.activityInfo?.applicationInfo ?: continue
            if (appInfo.packageName != ownPkg) {
                packageMap[appInfo.packageName] = appInfo
            }
        }

        // Also include any already-saved bubble packages if installed
        for (pkg in bubbleSet) {
            if (pkg != ownPkg && !packageMap.containsKey(pkg)) {
                try {
                    val info = pm.getApplicationInfo(pkg, 0)
                    packageMap[pkg] = info
                } catch (_: PackageManager.NameNotFoundException) {
                }
            }
        }

        val result = ArrayList<AppInfo>(packageMap.size)
        for ((pkg, appInfo) in packageMap) {
            val label = try {
                pm.getApplicationLabel(appInfo).toString().ifBlank { pkg }
            } catch (_: Exception) {
                pkg
            }
            val icon = try {
                pm.getApplicationIcon(appInfo)
            } catch (_: Exception) {
                null
            }
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0

            val inBubble = bubbleSet.contains(pkg)
            result.add(
                AppInfo(
                    name = label,
                    packageName = pkg,
                    uid = appInfo.uid,
                    icon = icon,
                    isBlocked = inBubble && activeSet.contains(pkg),
                    isInBubble = inBubble,
                    isSystemApp = isSystem
                )
            )
        }

        result.sortWith(
            compareByDescending<AppInfo> { it.isInBubble }
                .thenByDescending { it.isBlocked }
                .thenBy { it.isSystemApp }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        )
        result
    }
}
