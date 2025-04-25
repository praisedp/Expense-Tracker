package com.example.expencetracker.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.expencetracker.R
import com.example.expencetracker.data.PrefsManager
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

object DailyReminderManager {
    private const val TAG = "DailyReminderManager"
    private const val REMINDER_REQUEST_CODE = 123
    private const val REMINDER_HOUR = 12
    private const val REMINDER_MINUTE = 2

    fun setDailyReminder(context: Context, enabled: Boolean) {
        Log.d(TAG, "Setting daily reminder: enabled = $enabled")
        
        PrefsManager.setDailyReminderEnabled(enabled)
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (enabled) {
            val currentTime = System.currentTimeMillis()
            val calendar = Calendar.getInstance().apply {
                timeInMillis = currentTime
                set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
                set(Calendar.MINUTE, REMINDER_MINUTE)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Debug logging for time calculations
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            Log.d(TAG, "Current time: ${dateFormat.format(currentTime)}")
            Log.d(TAG, "Initial alarm time: ${dateFormat.format(calendar.timeInMillis)}")
            
            // If the calculated time is in the past, add one day
            if (calendar.timeInMillis <= currentTime) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                Log.d(TAG, "Alarm time was in past, adjusted to: ${dateFormat.format(calendar.timeInMillis)}")
            }

            try {
                // Use setExactAndAllowWhileIdle for more precise alarm timing
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
                
                // Verify if the alarm is actually set
                val isAlarmSet = PendingIntent.getBroadcast(
                    context,
                    REMINDER_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                ) != null
                
                Log.d(TAG, "Alarm scheduled for: ${dateFormat.format(calendar.timeInMillis)}")
                Log.d(TAG, "Is alarm actually set? $isAlarmSet")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling alarm", e)
            }
        } else {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Alarm cancelled")
        }
    }

    fun testNotificationNow(context: Context) {
        NotificationHelper.showBudgetAlert(
            context,
            "Daily Expense Reminder",
            "Don't forget to record today's expenses!",
            isExceeded = false
        )
    }
}

class DailyReminderReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "DailyReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast for daily reminder")
        
        // Schedule next day's alarm immediately
        DailyReminderManager.setDailyReminder(context, true)
        
        // Show the notification
        NotificationHelper.showBudgetAlert(
            context,
            "Daily Expense Reminder",
            "Don't forget to record today's expenses!",
            isExceeded = false
        )
    }
} 