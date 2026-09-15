package com.skn.countdown

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** All alarms are RTC-based (not elapsed-realtime), so they don't survive a reboot on their own. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, CountdownWidgetProvider::class.java))
        if (ids.isEmpty()) return

        NotificationHelper.ensureChannels(context)
        AlarmScheduler.scheduleNextTick(context)

        for (id in ids) {
            if (!WidgetPrefs.isConfigured(context, id)) continue
            if (!WidgetPrefs.isExpired(context, id)) {
                AlarmScheduler.scheduleExpiry(context, id, WidgetPrefs.getTarget(context, id))
            }
            CountdownWidgetProvider.updateWidget(context, appWidgetManager, id)
        }
    }
}
