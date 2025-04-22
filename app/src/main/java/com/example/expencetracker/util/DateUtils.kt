package com.example.expencetracker.util

import java.util.Calendar

object DateUtils {
    /** Returns [date] floored to 00:00:00.000 of its first day of month. */
    fun startOfMonth(dateMillis: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }

    /** Returns 23:59:59.999 of the last day of month containing [date]. */
    fun endOfMonth(dateMillis: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return c.timeInMillis
    }

    /** Returns 00:00:00.000 of the Monday (or Calendar’s first day) of the week of [date]. */
    fun startOfWeek(dateMillis: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }

    /** Returns 23:59:59.999 of the Sunday of the same week as [date]. */
    fun endOfWeek(dateMillis: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek + 6)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return c.timeInMillis
    }
}