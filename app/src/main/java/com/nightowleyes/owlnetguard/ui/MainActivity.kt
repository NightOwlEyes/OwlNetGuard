package com.nightowleyes.owlnetguard.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.nightowleyes.owlnetguard.BuildConfig
import com.nightowleyes.owlnetguard.data.PreferencesManager
import com.nightowleyes.owlnetguard.ui.theme.OwlNetGuardTheme
import com.nightowleyes.owlnetguard.utils.NetworkSettingsHelper

class MainActivity : ComponentActivity() {

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            PreferencesManager.setVpnEnabled(this, true)
            PreferencesManager.syncAllServices(this)
            Toast.makeText(this, "Đã cấp quyền VPN và khởi động dịch vụ!", Toast.LENGTH_SHORT).show()
        } else {
            PreferencesManager.setVpnEnabled(this, false)
            PreferencesManager.syncAllServices(this)
            Toast.makeText(this, "Chưa cấp quyền VPN!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NetworkSettingsHelper.init(this)
        requestNotificationPermissionIfNeeded()
        handleIncomingIntent(intent)

        setContent {
            OwlNetGuardTheme {
                MainScreen(
                    onRequestVpnPermission = { requestVpnPermissionAndSync() },
                    onRequestOverlayPermission = { openOverlayPermissionSettings() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_REQUEST_VPN_PERMISSION, false) == true) {
            requestVpnPermissionAndSync()
        } else if (VpnService.prepare(this) == null) {
            PreferencesManager.syncAllServices(this)
        }
    }

    private fun requestVpnPermissionAndSync() {
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            vpnPermissionLauncher.launch(prepareIntent)
        } else {
            PreferencesManager.setVpnEnabled(this, true)
            PreferencesManager.syncAllServices(this)
        }
    }

    private fun openOverlayPermissionSettings() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            )
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "Không thể mở cài đặt quyền Hiển thị trên ứng dụng khác",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
    }

    companion object {
        const val EXTRA_REQUEST_VPN_PERMISSION = "REQUEST_VPN_PERMISSION"
        const val GITHUB_REPO_URL = "https://github.com/NightOwlEyes/OwlNetGuard"
        const val GITHUB_RELEASES_URL = "https://github.com/NightOwlEyes/OwlNetGuard/releases"
        const val GITHUB_SPONSORS_URL = "https://github.com/sponsors/NightOwlEyes"
        const val GITHUB_LATEST_API = "https://api.github.com/repos/NightOwlEyes/OwlNetGuard/releases/latest"
        val CURRENT_APP_VERSION: String = BuildConfig.VERSION_NAME
    }
}
