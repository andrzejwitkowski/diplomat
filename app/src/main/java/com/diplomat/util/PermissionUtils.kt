package com.diplomat.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.getSystemService

/**
 * Helpers for the system-level permissions Diplomat depends on: notification
 * access (BIND_NOTIFICATION_LISTENER_SERVICE, granted in Settings) and battery
 * optimization exemption.
 */
object PermissionUtils {

    /** True when the user has granted notification access to this app. */
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        return enabled.split(':').any { it.substringBefore('/') == context.packageName }
    }

    /** Opens the system screen where the user grants notification access. */
    fun openNotificationListenerSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** True when the app is already exempt from battery optimization. */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val power = context.getSystemService<PowerManager>() ?: return false
        return power.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Prompts the user to disable battery optimization for this app. */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
