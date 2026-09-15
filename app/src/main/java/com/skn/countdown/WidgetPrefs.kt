package com.skn.countdown

import android.content.Context

/**
 * Per-widget-instance storage. Each home screen widget has its own
 * AppWidgetManager-assigned id, so a single SharedPreferences file keyed by
 * that id already gives us "one timer today, many timers later" for free —
 * no separate list/database needed until we want a timer with no widget.
 */
object WidgetPrefs {
    private const val FILE = "countdown_prefs"

    const val DEFAULT_FONT_KEY = "system"
    const val DEFAULT_BACKGROUND_KEY = "butter_paper"
    const val DEFAULT_OPACITY_PERCENT = 45
    const val DEFAULT_TEXT_COLOR_KEY = "auto"
    const val DEFAULT_LABEL_SCALE_PERCENT = 100
    const val DEFAULT_VALUE_SCALE_PERCENT = 100

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun save(
        context: Context,
        widgetId: Int,
        label: String,
        targetMillis: Long,
        fontKey: String = DEFAULT_FONT_KEY,
        backgroundKey: String = DEFAULT_BACKGROUND_KEY,
        opacityPercent: Int = DEFAULT_OPACITY_PERCENT,
        textColorKey: String = DEFAULT_TEXT_COLOR_KEY,
        labelScalePercent: Int = DEFAULT_LABEL_SCALE_PERCENT,
        valueScalePercent: Int = DEFAULT_VALUE_SCALE_PERCENT
    ) {
        prefs(context).edit()
            .putString(key("label", widgetId), label)
            .putLong(key("target", widgetId), targetMillis)
            .putBoolean(key("expired", widgetId), false)
            .putString(key("font", widgetId), fontKey)
            .putString(key("background", widgetId), backgroundKey)
            .putInt(key("opacity", widgetId), opacityPercent)
            .putString(key("textcolor", widgetId), textColorKey)
            .putInt(key("labelscale", widgetId), labelScalePercent)
            .putInt(key("valuescale", widgetId), valueScalePercent)
            .apply()
    }

    fun isConfigured(context: Context, widgetId: Int): Boolean =
        prefs(context).contains(key("target", widgetId))

    fun getLabel(context: Context, widgetId: Int): String =
        prefs(context).getString(key("label", widgetId), "") ?: ""

    fun getTarget(context: Context, widgetId: Int): Long =
        prefs(context).getLong(key("target", widgetId), -1L)

    fun isExpired(context: Context, widgetId: Int): Boolean =
        prefs(context).getBoolean(key("expired", widgetId), false)

    fun markExpired(context: Context, widgetId: Int) {
        prefs(context).edit().putBoolean(key("expired", widgetId), true).apply()
    }

    fun getFontKey(context: Context, widgetId: Int): String =
        prefs(context).getString(key("font", widgetId), DEFAULT_FONT_KEY) ?: DEFAULT_FONT_KEY

    fun getBackgroundKey(context: Context, widgetId: Int): String =
        prefs(context).getString(key("background", widgetId), DEFAULT_BACKGROUND_KEY) ?: DEFAULT_BACKGROUND_KEY

    fun getOpacityPercent(context: Context, widgetId: Int): Int =
        prefs(context).getInt(key("opacity", widgetId), DEFAULT_OPACITY_PERCENT)

    fun getTextColorKey(context: Context, widgetId: Int): String =
        prefs(context).getString(key("textcolor", widgetId), DEFAULT_TEXT_COLOR_KEY) ?: DEFAULT_TEXT_COLOR_KEY

    fun getLabelScalePercent(context: Context, widgetId: Int): Int =
        prefs(context).getInt(key("labelscale", widgetId), DEFAULT_LABEL_SCALE_PERCENT)

    fun getValueScalePercent(context: Context, widgetId: Int): Int =
        prefs(context).getInt(key("valuescale", widgetId), DEFAULT_VALUE_SCALE_PERCENT)

    fun remove(context: Context, widgetId: Int) {
        prefs(context).edit()
            .remove(key("label", widgetId))
            .remove(key("target", widgetId))
            .remove(key("expired", widgetId))
            .remove(key("font", widgetId))
            .remove(key("background", widgetId))
            .remove(key("opacity", widgetId))
            .remove(key("textcolor", widgetId))
            .remove(key("labelscale", widgetId))
            .remove(key("valuescale", widgetId))
            .apply()
    }

    private fun key(prefix: String, widgetId: Int) = "${prefix}_$widgetId"
}
