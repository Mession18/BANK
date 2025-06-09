package com.example.incomeplanner.util

import java.util.Calendar
import java.util.TimeZone

object DateUtil {

    private fun getCalendarInstance(timestamp: Long): Calendar {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = timestamp
        }
    }

    fun getStartOfDay(timestamp: Long): Long {
        return getCalendarInstance(timestamp).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun getEndOfDay(timestamp: Long): Long {
        return getCalendarInstance(timestamp).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    fun getStartOfWeek(timestamp: Long): Long { // Assuming week starts on Monday
        return getCalendarInstance(timestamp).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun getEndOfWeek(timestamp: Long): Long { // Assuming week starts on Monday
        return getCalendarInstance(timestamp).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
            // If Sunday is the first day of the week in this Calendar setting, add 7 days.
            if (get(Calendar.DAY_OF_WEEK) < Calendar.MONDAY) {
                 add(Calendar.DAY_OF_YEAR, 7)
            }
        }.timeInMillis
    }


    fun getStartOfMonth(timestamp: Long): Long {
        return getCalendarInstance(timestamp).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun getEndOfMonth(timestamp: Long): Long {
        return getCalendarInstance(timestamp).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
}
