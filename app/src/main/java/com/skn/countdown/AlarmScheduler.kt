package com.skn.countdown

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Two kinds of alarms:
 *  - one shared "tick" alarm, re-armed for the next minute boundary each time
 *    it fires, that repaints every configured widget's display text
 *  - one "expiry" alarm per widget, set for the exact target instant, that
 *    fires the "timer expired" notification independently of the tick grid
 *    so the alert isn't a minute late.
 */
object AlarmScheduler {
    private const val REQUEST_CODE_TICK = 1000
    private const val EXPIRY_REQUEST_BASE = 5_000_000

    @SuppressLint("ShortAlarm")
    fun scheduleNextTick(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val nextMinuteBoundary = (System.currentTimeMillis() / 60_000L + 1) * 60_000L

        val intent = Intent(context, TickReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE_TICK, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, nextMinuteBoundary, pendingIntent
        )
    }

    fun scheduleExpiry(context: Context, widgetId: Int, targetMillis: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = expiryPendingIntent(context, widgetId)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent
        )
    }

    fun cancelExpiry(context: Context, widgetId: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(expiryPendingIntent(context, widgetId))
    }

    private fun expiryPendingIntent(context: Context, widgetId: Int): PendingIntent {
        val intent = Intent(context, ExpiryReceiver::class.java)
            .putExtra(EXTRA_WIDGET_ID, widgetId)
        return PendingIntent.getBroadcast(
            context, EXPIRY_REQUEST_BASE + widgetId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    const val EXTRA_WIDGET_ID = "widget_id"
}
