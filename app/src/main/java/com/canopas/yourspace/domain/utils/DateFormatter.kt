package com.canopas.yourspace.domain.utils

import android.content.Context
import com.canopas.yourspace.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

fun Long.formattedTimeAgoString(context: Context): String {
    val now = System.currentTimeMillis()
    val elapsedTime = now - this
    return when {
        elapsedTime < TimeUnit.MINUTES.toMillis(1) -> context.getString(R.string.map_user_item_location_updated_now)
        elapsedTime < TimeUnit.HOURS.toMillis(1) -> context.getString(
            R.string.map_user_item_location_updated_minutes_ago,
            "${TimeUnit.MILLISECONDS.toMinutes(elapsedTime)}"
        )

        elapsedTime < TimeUnit.DAYS.toMillis(1) ->
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(this))

        else -> SimpleDateFormat("h:mm a • d MMM", Locale.getDefault()).format(Date(this))
    }
}

fun Long.formattedMessageTimeString(context: Context): String {
    val now = System.currentTimeMillis()
    val elapsedTime = now - this
    return when {
        elapsedTime < TimeUnit.MINUTES.toMillis(1) -> context.getString(R.string.map_user_item_location_updated_now)
        elapsedTime < TimeUnit.HOURS.toMillis(1) -> context.getString(
            R.string.messages_time_format_minutes,
            "${TimeUnit.MILLISECONDS.toMinutes(elapsedTime)}"
        )

        elapsedTime < TimeUnit.DAYS.toMillis(1) ->
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(this))

        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(this))
    }
}

fun Long.formattedMessageDateHeader(context: Context): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = this

    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val sdf = SimpleDateFormat("dd MMMM", Locale.getDefault())
    return when {
        calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) && calendar.get(Calendar.DAY_OF_YEAR) == today.get(
            Calendar.DAY_OF_YEAR
        ) -> context.getString(R.string.common_today)

        else -> sdf.format(calendar.time)
    }
}

fun Long.isToday(): Boolean {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = this

    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) && calendar.get(Calendar.DAY_OF_YEAR) == today.get(
        Calendar.DAY_OF_YEAR
    )
}

fun timeAgo(timestamp: Long?): String {
    if (timestamp == null) return ""
    val now = System.currentTimeMillis()
    val seconds = (now - timestamp) / 1000

    val minutes = TimeUnit.SECONDS.toMinutes(seconds)
    val hours = TimeUnit.SECONDS.toHours(seconds)
    val days = TimeUnit.SECONDS.toDays(seconds)
    val weeks = days / 7
    val months = days / 30
    val years = days / 365

    return when {
        seconds < 60 -> "just now"
        minutes < 60 -> "$minutes minutes ago"
        hours < 24 -> "$hours hours ago"
        days < 7 -> "$days days ago"
        weeks < 4 -> "$weeks weeks ago"
        months < 12 -> "$months months ago"
        else -> "$years years ago"
    }
}
