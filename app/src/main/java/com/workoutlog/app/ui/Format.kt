package com.workoutlog.app.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

fun formatNumber(value: Double): String =
    if (abs(value - value.roundToLong()) < 0.01) value.roundToLong().toString()
    else String.format(Locale.US, "%.1f", value)

fun formatDate(millis: Long): String = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(millis))

fun formatShortDate(millis: Long): String = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))

fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%d:%02d", m, s)
}

fun relativeDay(millis: Long): String {
    fun dayIndex(t: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = t }
        return c.get(Calendar.YEAR) * 400L + c.get(Calendar.DAY_OF_YEAR)
    }
    val today = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = millis }
    val days = ((today.timeInMillis - then.timeInMillis) / 86_400_000L).toInt()
    return when {
        dayIndex(millis) == dayIndex(today.timeInMillis) -> "today"
        days <= 1 -> "yesterday"
        days < 7 -> "$days days ago"
        else -> formatDate(millis)
    }
}
