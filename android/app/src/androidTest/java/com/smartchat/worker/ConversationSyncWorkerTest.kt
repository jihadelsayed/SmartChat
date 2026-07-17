package com.smartchat.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationSyncWorkerTest {
    @Test
    fun workerSucceedsWhenThereAreNoPendingMessages() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val worker = TestListenableWorkerBuilder<ConversationSyncWorker>(context).build()

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Success)
    }
}
