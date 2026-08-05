package com.diplomat.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.diplomat.R

/**
 * Centralized notification channel ids and registration.
 */
object NotificationChannels {

    const val FOREGROUND = "diplomat_foreground"
    const val DECISIONS = "diplomat_decisions"

    fun registerAll(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return

        val foreground = NotificationChannel(
            FOREGROUND,
            context.getString(R.string.fg_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.fg_channel_description)
        }

        val decisions = NotificationChannel(
            DECISIONS,
            context.getString(R.string.decision_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.decision_channel_description)
        }

        manager.createNotificationChannels(listOf(foreground, decisions))
    }
}
