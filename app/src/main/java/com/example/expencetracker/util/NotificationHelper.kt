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

object NotificationHelper {

    private const val CH_ID = "budget_alert"
    private const val CH_NAME = "Budget alerts"

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
        val notif = NotificationCompat.Builder(ctx, CH_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)   // ← no extra markup here
            .setContentText(msg)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        // Android 13+ requires POST_NOTIFICATIONS runtime permission
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

    private fun createChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CH_ID, CH_NAME, NotificationManager.IMPORTANCE_HIGH
            )
            NotificationManagerCompat.from(ctx).createNotificationChannel(channel)
        }
    }
}