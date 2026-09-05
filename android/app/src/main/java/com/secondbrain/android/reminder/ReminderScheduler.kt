package com.secondbrain.android.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.core.app.NotificationManagerCompat
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Replaces a node's old device alert whenever its server reminder is changed. */
object ReminderScheduler {
    const val CHANNEL_ID = "review_reminders"

    fun schedule(context: Context, nodeId: Long, title: String, scheduledAt: String) {
        // The server notification remains authoritative when the user has disabled device alerts.
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        createChannel(context)
        val due = runCatching { OffsetDateTime.parse(scheduledAt).toInstant() }
            .getOrElse { LocalDateTime.parse(scheduledAt).atZone(java.time.ZoneId.systemDefault()).toInstant() }
        val delay = Duration.between(Instant.now(), due).coerceAtLeast(Duration.ZERO)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(Data.Builder().putString(ReminderWorker.TITLE, title).putLong(ReminderWorker.NOTIFICATION_ID, nodeId).build())
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("review-reminder-" + nodeId, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context, nodeId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork("review-reminder-" + nodeId)
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "复习提醒", NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
