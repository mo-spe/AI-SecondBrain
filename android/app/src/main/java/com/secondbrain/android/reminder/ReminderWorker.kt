package com.secondbrain.android.reminder

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** Mirrors a confirmed server-side reminder as one local system notification. */
class ReminderWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
            return Result.success()
        }
        try {
            NotificationManagerCompat.from(applicationContext).notify(
                inputData.getLong(NOTIFICATION_ID, System.currentTimeMillis()).toInt(),
                NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("SecondBrain 复习提醒")
                    .setContentText(inputData.getString(TITLE) ?: "该复习了")
                    .setAutoCancel(true)
                    .build()
            )
        } catch (_: SecurityException) {
            // Permission can be revoked after scheduling; the server-side notification is still retained.
        }
        return Result.success()
    }

    companion object {
        const val TITLE = "title"
        const val NOTIFICATION_ID = "notification_id"
    }
}
