package com.smartchat

import android.app.Application
import androidx.room.Room
import com.smartchat.core.database.SmartChatDatabase
import com.smartchat.core.database.MIGRATION_1_2
import com.smartchat.core.datastore.SettingsRepository
import com.smartchat.core.network.NetworkClient
import com.smartchat.data.DefaultChatRepository
import com.smartchat.repository.AuthRepositoryImpl
import com.smartchat.repository.ProfileRepositoryImpl
import com.smartchat.worker.WorkScheduler

class SmartChatApplication : Application() {
    val database: SmartChatDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            SmartChatDatabase::class.java,
            "smartchat.db"
        ).addMigrations(MIGRATION_1_2).build()
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(applicationContext)
    }

    private val api by lazy {
        NetworkClient.create(settingsRepository)
    }

    val authRepository by lazy {
        AuthRepositoryImpl(api, settingsRepository)
    }

    val profileRepository by lazy {
        ProfileRepositoryImpl(api)
    }

    val chatRepository by lazy {
        DefaultChatRepository(
            database = database,
            api = api,
            contentResolver = contentResolver,
            onSyncNeeded = { WorkScheduler.enqueuePendingSync(applicationContext) }
        )
    }

    override fun onCreate() {
        super.onCreate()
        WorkScheduler.schedulePeriodicSync(this)
    }
}
