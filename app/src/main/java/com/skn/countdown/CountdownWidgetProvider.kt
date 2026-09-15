package com.skn.countdown

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews

class CountdownWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        NotificationHelper.ensureChannels(context)
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
        AlarmScheduler.scheduleNextTick(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            AlarmScheduler.cancelExpiry(context, id)
            NotificationHelper.cancelOngoing(context, id)
            WidgetPrefs.remove(context, id)
        }
    }

    // Fires while dragging the widget's resize handles, so the card is
    // redrawn to the new pixel size live instead of staying
    // stretched/letterboxed until the next minute's tick.
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    companion object {
        /** Shared by the provider itself, the per-minute tick, and the expiry alarm. */
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val (widthPx, heightPx) = WidgetSizing.currentSizePx(context, appWidgetManager, widgetId)
            val views = RemoteViews(context.packageName, R.layout.widget_countdown)

            if (!WidgetPrefs.isConfigured(context, widgetId)) {
                views.setImageViewBitmap(
                    R.id.widget_image,
                    WidgetRenderer.render(
                        context, "Countdown", "Tap to set up", expired = false, widthPx, heightPx,
                        WidgetPrefs.DEFAULT_FONT_KEY, WidgetPrefs.DEFAULT_BACKGROUND_KEY,
                        WidgetPrefs.DEFAULT_OPACITY_PERCENT, WidgetPrefs.DEFAULT_TEXT_COLOR_KEY
                    )
                )
                views.setOnClickPendingIntent(R.id.widget_image, PendingIntents.editWidget(context, widgetId))
                appWidgetManager.updateAppWidget(widgetId, views)
                return
            }

            val label = WidgetPrefs.getLabel(context, widgetId)
            val target = WidgetPrefs.getTarget(context, widgetId)
            val expiredNow = Countdown.isExpired(target)
            val fontKey = WidgetPrefs.getFontKey(context, widgetId)
            val backgroundKey = WidgetPrefs.getBackgroundKey(context, widgetId)
            val opacityPercent = WidgetPrefs.getOpacityPercent(context, widgetId)
            val textColorKey = WidgetPrefs.getTextColorKey(context, widgetId)
            val labelScalePercent = WidgetPrefs.getLabelScalePercent(context, widgetId)
            val valueScalePercent = WidgetPrefs.getValueScalePercent(context, widgetId)

            // Safety net: if the exact expiry alarm was ever missed or delayed
            // (e.g. extreme Doze), the next tick still catches the crossover.
            if (expiredNow && !WidgetPrefs.isExpired(context, widgetId)) {
                WidgetPrefs.markExpired(context, widgetId)
                NotificationHelper.postExpiredAlert(context, widgetId, label)
            }

            val countdownText = if (expiredNow) "0:00:00" else Countdown.format(Countdown.remaining(target))

            views.setImageViewBitmap(
                R.id.widget_image,
                WidgetRenderer.render(
                    context, label, countdownText, expiredNow, widthPx, heightPx,
                    fontKey, backgroundKey, opacityPercent, textColorKey,
                    labelScalePercent, valueScalePercent, showUnitCaptions = true
                )
            )
            views.setOnClickPendingIntent(R.id.widget_image, PendingIntents.editWidget(context, widgetId))
            appWidgetManager.updateAppWidget(widgetId, views)

            NotificationHelper.updateOngoing(context, widgetId, label, countdownText, expiredNow)
        }
    }
}
