package com.example.doctracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Document reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
        // Checks once a day (the exact time is chosen by Android) whether a reminder is due.
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "reminders", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS).build()
        )
    }

    companion object { const val CHANNEL_ID = "reminders" }
}
