package com.skn.countdown

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fires once, exactly at a single widget's target instant. */
class ExpiryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val widgetId = intent.getIntExtra(AlarmScheduler.EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        if (!WidgetPrefs.isConfigured(context, widgetId)) return

        if (!WidgetPrefs.isExpired(context, widgetId)) {
            WidgetPrefs.markExpired(context, widgetId)
            NotificationHelper.postExpiredAlert(context, widgetId, WidgetPrefs.getLabel(context, widgetId))
        }

        CountdownWidgetProvider.updateWidget(context, AppWidgetManager.getInstance(context), widgetId)
    }
}
