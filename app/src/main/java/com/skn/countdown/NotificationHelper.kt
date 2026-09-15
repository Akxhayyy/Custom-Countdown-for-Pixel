package com.skn.countdown

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Two notifications per widget:
 *  - an ongoing, silent, non-swipeable one that mirrors the widget's current
 *    text — this is the actual "lock screen" surface, since a real lock
 *    screen widget isn't available on this device (Pixel's lock screen
 *    panel is a fixed, Google-curated set: weather / Gemini / stocks, no
 *    third-party slot yet).
 *  - a one-shot, dismissible alert fired exactly at zero.
 */
object NotificationHelper {
    private const val CHANNEL_ONGOING = "countdown_ongoing"
    private const val CHANNEL_ALERTS = "countdown_alerts"

    private const val ONGOING_ID_BASE = 9_000_000
    private const val ALERT_ID_BASE = 8_000_000

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ONGOING, "Countdown (ongoing)", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Always-visible countdown, mirrors the widget"
                setShowBadge(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERTS, "Countdown expired", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Fires once when a countdown reaches zero"
            }
        )
    }

    fun updateOngoing(context: Context, widgetId: Int, label: String, countdownText: String, expired: Boolean) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val title = if (label.isNotBlank()) label else "Countdown"
        val body = if (expired) "Expired" else countdownText

        val notification = NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle(title)
            .setContentText(body)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(PendingIntents.editWidget(context, widgetId))
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(ONGOING_ID_BASE + widgetId, notification)
        }
    }

    fun cancelOngoing(context: Context, widgetId: Int) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(ONGOING_ID_BASE + widgetId)
        }
    }

    fun postExpiredAlert(context: Context, widgetId: Int, label: String) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val title = if (label.isNotBlank()) label else "Countdown"
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle(title)
            .setContentText("Timer expired")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(PendingIntents.editWidget(context, widgetId))
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(ALERT_ID_BASE + widgetId, notification)
        }
    }
}
