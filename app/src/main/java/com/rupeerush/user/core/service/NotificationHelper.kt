package com.rupeerush.user.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.rupeerush.user.MainActivity
import com.rupeerush.user.R

object NotificationHelper {

    const val CHANNEL_REMINDERS = "rupee_rush_reminders"
    const val CHANNEL_DOWNLOADS = "rupee_rush_downloads"
    const val CHANNEL_TASKS = "rupee_rush_tasks"

    // Default server notification placeholder as required by spec Section 14
    const val DEFAULT_TEXT_TBD = "[DEFAULT_TEXT_TBD]"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Check-in & Streak Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily check-in reminders and streak expiration warnings"
            }

            val downloadChannel = NotificationChannel(
                CHANNEL_DOWNLOADS,
                "Update & Download Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live download progress for in-app APK updates"
            }

            val taskChannel = NotificationChannel(
                CHANNEL_TASKS,
                "Task & Payment Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            manager.createNotificationChannels(listOf(reminderChannel, downloadChannel, taskChannel))
        }
    }

    fun showCheckinReminder(context: Context, streak: Int, isHindi: Boolean = false) {
        val title = if (isHindi) "आज का बोनस बाकी है!" else "Don't break your streak! 🔥"
        val message = if (isHindi) "अपना दैनिक इनाम क्लेम करें और स्ट्रीक $streak दिन तक बढ़ाएं।" else "Claim your daily bonus and keep your $streak-day streak alive."

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(101, notification)
    }

    fun showStreakWarning(context: Context, isHindi: Boolean = false) {
        val title = if (isHindi) "चेक-इन समाप्त होने वाला है ⚠️" else "Night Reminder: Streak at Risk! ⚠️"
        val message = if (isHindi) "आज रात 12 बजे से पहले चेक-इन करें वरना आपकी स्ट्रीक रीसेट हो जाएगी।" else "Check in before midnight or your streak resets to zero."

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            102,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(102, notification)
    }
}
