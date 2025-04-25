package com.example.expencetracker.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.expencetracker.R
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.util.Log

object NotificationHelper {

    private const val BUDGET_CHANNEL_ID = "budget_alert"
    private const val REMINDER_CHANNEL_ID = "daily_reminder"
    private const val BUDGET_CHANNEL_NAME = "Budget alerts"
    private const val REMINDER_CHANNEL_NAME = "Daily Reminders"
    private const val TAG = "NotificationHelper"

    fun showBudgetAlert(
        ctx: Context,
        title: String,
        msg: String,
        isExceeded: Boolean         // true → red icon, false → amber icon
    ) {
        createChannel(ctx)

        val iconRes = if (isExceeded)
            android.R.drawable.stat_notify_error
        else
            android.R.drawable.stat_sys_warning
        val notif = NotificationCompat.Builder(ctx, BUDGET_CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)   // ← no extra markup here
            .setContentText(msg)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        // Android 13+ requires POST_NOTIFICATIONS runtime permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // permission not granted → skip notification
            return
        }

        NotificationManagerCompat.from(ctx).notify(1001, notif)
    }

    fun showDailyReminder(context: Context) {
        Log.d(TAG, "Showing daily reminder notification")
        
        createReminderChannel(context)

        val notif = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Record Daily Expenses")
            .setContentText("Don't forget to record today's expenses!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .build()

        // Check notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission not granted")
                return
            }
        }

        try {
            NotificationManagerCompat.from(context).notify(1002, notif)
            Log.d(TAG, "Notification sent successfully")
        } catch (e: SecurityException) {
            Log.e(TAG, "Error showing notification", e)
        }
    }

    private fun createChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                BUDGET_CHANNEL_ID, BUDGET_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            )
            NotificationManagerCompat.from(ctx).createNotificationChannel(channel)
        }
    }

    private fun createReminderChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                REMINDER_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily reminders to record expenses"
                enableLights(true)
                enableVibration(true)
            }
            try {
                NotificationManagerCompat.from(context).createNotificationChannel(channel)
                Log.d(TAG, "Notification channel created")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating notification channel", e)
            }
        }
    }
}