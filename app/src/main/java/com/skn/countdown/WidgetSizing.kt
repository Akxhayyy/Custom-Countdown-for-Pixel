package com.skn.countdown

import android.appwidget.AppWidgetManager
import android.content.Context

/** Shared by the provider (actual render) and the config screen (live preview). */
object WidgetSizing {
    private const val FALLBACK_WIDTH_DP = 180
    private const val FALLBACK_HEIGHT_DP = 140
    private const val MIN_SIZE_PX = 80
    private const val MAX_SIZE_PX = 1600

    /** The widget's actual on-screen pixel size, so the bitmap fills it exactly with no letterboxing. */
    fun currentSizePx(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int): Pair<Int, Int> {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, FALLBACK_WIDTH_DP)
        val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, FALLBACK_HEIGHT_DP)

        val density = context.resources.displayMetrics.density
        val widthPx = (widthDp * density).toInt().coerceIn(MIN_SIZE_PX, MAX_SIZE_PX)
        val heightPx = (heightDp * density).toInt().coerceIn(MIN_SIZE_PX, MAX_SIZE_PX)
        return widthPx to heightPx
    }
}
