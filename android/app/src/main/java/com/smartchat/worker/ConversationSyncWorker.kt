package com.smartchat.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartchat.SmartChatApplication
import kotlinx.coroutines.flow.first

class ConversationSyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val application = applicationContext as? SmartChatApplication
            ?: return Result.failure()
        if (application.settingsRepository.accessToken.first().isNullOrBlank()) {
            return Result.success()
        }
        return if (application.chatRepository.retryAllPendingMessages()) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
