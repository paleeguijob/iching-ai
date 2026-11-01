package com.iching.ai.data.di

import android.content.Context
import androidx.room.Room
import com.iching.ai.data.journal.JournalDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun platformDataModule(): Module = module {
    single { HttpClient(OkHttp).configured() }
    single {
        val context: Context = get()
        buildDatabase(
            Room.databaseBuilder<JournalDatabase>(
                context = context,
                name = context.getDatabasePath(DATABASE_FILE).absolutePath,
            )
        )
    }
    single {
        val context: Context = get()
        createDataStore(context.filesDir.resolve(SETTINGS_FILE).absolutePath)
    }
}
