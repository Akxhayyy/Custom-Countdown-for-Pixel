package com.skn.countdown

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent

object PendingIntents {
    /** Opens ConfigureActivity pre-loaded with this widget's saved values, for edits. */
    fun editWidget(context: Context, widgetId: Int): PendingIntent {
        val intent = Intent(context, ConfigureActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .setData(android.net.Uri.parse("countdown://widget/$widgetId"))
        return PendingIntent.getActivity(
            context, widgetId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
