package com.skn.countdown

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** Fires once a minute (self re-arming) and repaints every placed widget. */
class TickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, CountdownWidgetProvider::class.java))

        for (id in ids) {
            CountdownWidgetProvider.updateWidget(context, appWidgetManager, id)
        }

        // Stop re-arming once nothing is placed; a fresh widget add restarts
        // the chain via CountdownWidgetProvider.onUpdate.
        if (ids.isNotEmpty()) {
            AlarmScheduler.scheduleNextTick(context)
        }
    }
}
