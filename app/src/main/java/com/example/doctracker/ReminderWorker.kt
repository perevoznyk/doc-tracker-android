package com.example.doctracker

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.doctracker.data.AppDatabase
import com.example.doctracker.data.Document
import com.example.doctracker.data.ReminderOption
import com.example.doctracker.data.pretty
import java.time.LocalDate

class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        AppDatabase.get(applicationContext).dao().allDocuments().forEach { doc ->
            ReminderOption.parse(doc.reminders).forEach { option ->
                if (option.triggerDate(doc.validUntil) == today) notify(doc, option)
            }
        }
        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun notify(doc: Document, option: ReminderOption) {
        val nm = NotificationManagerCompat.from(applicationContext)
        if (!nm.areNotificationsEnabled()) return
        val text = if (option == ReminderOption.ON_DAY) "Expires today."
        else "Expires on ${doc.validUntil.pretty()}."
        val pi = PendingIntent.getActivity(
            applicationContext, 0, Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(applicationContext, App.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(doc.name)
            .setContentText(text)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify((doc.id * 10 + option.ordinal).toInt(), n)
    }
}
