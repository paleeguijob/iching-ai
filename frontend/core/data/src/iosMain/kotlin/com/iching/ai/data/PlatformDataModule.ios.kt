package com.iching.ai.data.di

import com.iching.ai.data.siamsi.FileSiamSiCache
import com.iching.ai.data.siamsi.SiamSiCache
import okio.FileSystem
import okio.Path.Companion.toPath
import androidx.room.Room
import com.iching.ai.data.journal.JournalDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val url = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return requireNotNull(url?.path) { "Documents directory unavailable" }
}

internal actual fun platformDataModule(): Module = module {
    single { HttpClient(Darwin).configured() }
    single { buildDatabase(Room.databaseBuilder<JournalDatabase>(name = "${documentDirectory()}/$DATABASE_FILE")) }
    single { createDataStore("${documentDirectory()}/$SETTINGS_FILE") }
    single<SiamSiCache> { FileSiamSiCache("${documentDirectory()}/$SIAMSI_DIR".toPath(), FileSystem.SYSTEM) }
}
