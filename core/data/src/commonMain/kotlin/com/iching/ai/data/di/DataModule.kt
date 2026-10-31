package com.iching.ai.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.iching.ai.data.BuildConfig
import com.iching.ai.data.api.HttpOracleApi
import com.iching.ai.data.journal.JournalDatabase
import com.iching.ai.data.journal.MIGRATION_1_2
import com.iching.ai.data.journal.RoomJournalRepository
import com.iching.ai.data.scripture.ScriptureHexagramRepository
import com.iching.ai.data.settings.DataStoreSettingsRepository
import com.iching.ai.domain.repository.HexagramRepository
import com.iching.ai.domain.repository.JournalRepository
import com.iching.ai.domain.repository.OracleApi
import com.iching.ai.domain.repository.SettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module

internal const val DATABASE_FILE = "iching_journal.db"
internal const val SETTINGS_FILE = "iching_settings.preferences_pb"

/** Platform pieces: HTTP engine, database builder and a writable directory. */
internal expect fun platformDataModule(): Module

internal fun createDataStore(path: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { path.toPath() })

internal fun HttpClient.configured(): HttpClient = config {
    install(HttpTimeout) {
        requestTimeoutMillis = 120_000
        connectTimeoutMillis = 20_000
        socketTimeoutMillis = 60_000
    }
}

internal fun buildDatabase(builder: RoomDatabase.Builder<JournalDatabase>): JournalDatabase =
    builder
        .addMigrations(MIGRATION_1_2)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

/** Requires an [com.iching.ai.data.api.ApiSession] binding, provided by the app module. */
val dataModule: Module = module {
    includes(platformDataModule())
    single<HexagramRepository> { ScriptureHexagramRepository() }
    single<OracleApi> { HttpOracleApi(get(), get(), BuildConfig.API_BASE_URL) }
    single<JournalRepository> { RoomJournalRepository(get<JournalDatabase>().readingDao()) }
    single<SettingsRepository> { DataStoreSettingsRepository(get()) }
}
