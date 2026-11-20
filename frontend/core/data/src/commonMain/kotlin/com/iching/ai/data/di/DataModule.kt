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
import com.iching.ai.data.journal.MIGRATION_2_3
import com.iching.ai.data.journal.MIGRATION_3_4
import com.iching.ai.data.journal.MIGRATION_4_5
import com.iching.ai.data.journal.MIGRATION_5_6
import com.iching.ai.data.journal.RoomJournalRepository
import com.iching.ai.data.resources.Res
import com.iching.ai.data.scripture.BUNDLED_ENGLISH_HEXAGRAMS
import com.iching.ai.data.scripture.HexagramContentState
import com.iching.ai.data.scripture.HexagramEnDownloads
import com.iching.ai.data.scripture.ScriptureHexagramRepository
import com.iching.ai.data.siamsi.BundledSiamSiRepository
import com.iching.ai.data.siamsi.SiamSiSetRepository
import com.iching.ai.data.settings.DataStoreSettingsRepository
import com.iching.ai.domain.repository.HexagramRepository
import com.iching.ai.domain.repository.SiamSiRepository
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
internal const val SIAMSI_DIR = "siamsi_sets"

/** Platform pieces: HTTP engine, database builder, settings store and the Siam Si download cache. */
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
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

/** Requires an [com.iching.ai.data.api.ApiSession] binding, provided by the app module. */
val dataModule: Module = module {
    includes(platformDataModule())
    single { HexagramContentState() }
    single {
        val content: HexagramContentState = get()
        HexagramEnDownloads(
            bundled = { runCatching { Res.readBytes(BUNDLED_ENGLISH_HEXAGRAMS).decodeToString() }.getOrNull() },
            client = get(),
            baseUrl = BuildConfig.WEB_BASE_URL,
            cache = get(),
            serverVersion = { content.hexagramsEnVersion },
        )
    }
    single<HexagramRepository> {
        val content: HexagramContentState = get()
        val downloads: HexagramEnDownloads = get()
        ScriptureHexagramRepository(loadEnglish = downloads::load, language = { content.language })
    }
    single<SiamSiRepository> { SiamSiSetRepository(BundledSiamSiRepository(), get(), BuildConfig.WEB_BASE_URL, get()) }
    single<OracleApi> { HttpOracleApi(get(), get(), BuildConfig.API_BASE_URL, BuildConfig.WEB_BASE_URL) }
    single<JournalRepository> { RoomJournalRepository(get<JournalDatabase>().readingDao()) }
    single<SettingsRepository> { DataStoreSettingsRepository(get()) }
}
