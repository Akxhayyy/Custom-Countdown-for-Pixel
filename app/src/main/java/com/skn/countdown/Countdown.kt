package com.skn.countdown

import java.util.concurrent.TimeUnit

/** Pure time math, shared by the widget renderer and the notification text. */
object Countdown {

    data class Remaining(val days: Long, val hours: Long, val minutes: Long)

    fun isExpired(targetMillis: Long, nowMillis: Long = System.currentTimeMillis()): Boolean =
        nowMillis >= targetMillis

    fun remaining(targetMillis: Long, nowMillis: Long = System.currentTimeMillis()): Remaining {
        val diff = (targetMillis - nowMillis).coerceAtLeast(0L)
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
        return Remaining(days, hours, minutes)
    }

    fun format(r: Remaining): String =
        "${r.days}:${"%02d".format(r.hours)}:${"%02d".format(r.minutes)}"
}
