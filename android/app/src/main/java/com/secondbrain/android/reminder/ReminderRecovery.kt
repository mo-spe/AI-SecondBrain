package com.secondbrain.android.reminder

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.secondbrain.android.data.remote.SecondBrainApi
import com.secondbrain.android.data.remote.requireData
import com.secondbrain.android.data.session.SessionStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Rebuilds local alerts from the service because device work may be cleared after a reboot,
 * an app update, or a user restoring the application.
 */
object ReminderRecovery {
    private const val UNIQUE_WORK_NAME = "review-reminder-recovery"

    fun enqueue(context: Context) {
        val request = OneTimeWorkRequestBuilder<ReminderRecoveryWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}

/** Runs independently from a UI lifecycle so recovery also works after a device restart. */
class ReminderRecoveryWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val dependencies = EntryPointAccessors.fromApplication(
            applicationContext,
            ReminderRecoveryDependencies::class.java
        )
        if (dependencies.sessionStore().token().isNullOrBlank()) {
            return Result.success()
        }
        return try {
            dependencies.api().reminders().requireData()
                .filterNot { it.status.equals("sent", ignoreCase = true) || it.status.equals("cancelled", ignoreCase = true) }
                .forEach { reminder ->
                    ReminderScheduler.schedule(
                        applicationContext,
                        reminder.nodeId,
                        "知识点 #${reminder.nodeId} 的复习时间到了",
                        reminder.scheduledAt
                    )
                }
            Result.success()
        } catch (_: Exception) {
            // A 401 interceptor clears the token, so retrying in that case would only waste background work.
            if (dependencies.sessionStore().token().isNullOrBlank()) Result.success() else Result.retry()
        }
    }
}

/** Makes singleton network dependencies available to a WorkManager-created worker. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderRecoveryDependencies {
    fun api(): SecondBrainApi
    fun sessionStore(): SessionStore
}
