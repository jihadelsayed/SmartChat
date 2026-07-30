package com.smartchat.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val PERIODIC_SYNC_NAME = "smartchat-periodic-message-sync"
    private const val PENDING_SYNC_NAME = "smartchat-pending-message-sync"

    private val connectedConstraint = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedulePeriodicSync(context: Context) {
        val request = PeriodicWorkRequestBuilder<ConversationSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(connectedConstraint)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_SYNC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun enqueuePendingSync(context: Context) {
        val request = OneTimeWorkRequestBuilder<ConversationSyncWorker>()
            .setConstraints(connectedConstraint)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            PENDING_SYNC_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
