package com.N3looG.thingtracker.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import com.N3looG.thingtracker.R
import java.text.SimpleDateFormat
import java.util.*

object NotificationHelper {

    private const val CHANNEL_ID = "thingtracker_reminders"
    private const val CHANNEL_NAME = "ThingTracker Reminders"
    private const val PREFS_NAME = "thingtracker_notif_prefs"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notificări pentru scadențele obiectelor"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun hasBeenNotifiedToday(context: Context, loanId: Long): Boolean {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val key = "notif_${loanId}_$todayStr"
        return prefs.getBoolean(key, false)
    }

    private fun markNotifiedToday(context: Context, loanId: Long) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val key = "notif_${loanId}_$todayStr"
        prefs.edit().putBoolean(key, true).apply()
    }

    fun sendOverdueNotificationIfNeeded(context: Context, loanId: Long, itemTitle: String, personName: String, notificationId: Int) {
        // Prevent duplicate notifications on the same day
        if (hasBeenNotifiedToday(context, loanId)) {
            return
        }

        // Restrict notifications to reasonable daytime hours (8:00 AM to 9:00 PM) to avoid midnight interruptions
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (currentHour !in 8..21) {
            return
        }

        createNotificationChannel(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.stat_overdue))
            .setContentText("$itemTitle - $personName")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            notificationManager.notify(notificationId, builder.build())
            markNotifiedToday(context, loanId)
        } catch (_: Exception) {}
    }
}
