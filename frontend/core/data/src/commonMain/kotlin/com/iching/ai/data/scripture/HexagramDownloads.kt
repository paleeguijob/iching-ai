package com.iching.ai.data.scripture

import com.iching.ai.data.siamsi.SiamSiCache
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/** Id under which the downloaded English file is kept in the Siam Si cache (`hexagrams_en.json` + `.version`). */
internal const val HEXAGRAMS_EN_CACHE_ID = "hexagrams_en"

/**
 * The English hexagram file: the copy bundled in the app, unless the server's `hexagramsEnVersion` is newer,
 * in which case `{baseUrl}/content/hexagrams.en.json?v={version}` is downloaded once and kept on the device.
 * A failed or broken download falls back to the newest copy already on the device; null only when even the
 * bundled copy cannot be read.
 */
class HexagramEnDownloads(
    private val bundled: suspend () -> String?,
    private val client: HttpClient,
    private val baseUrl: String,
    private val cache: SiamSiCache,
    private val serverVersion: () -> Int,
    private val fetchTimeoutMillis: Long = 15_000,
) {
    suspend fun load(): String? {
        val inApp = bundled()
        val inAppVersion = inApp?.let(::englishVersion) ?: 0
        val wanted = serverVersion()
        if (inApp != null && inAppVersion >= wanted) return inApp
        val device = runCatching { cache.read(HEXAGRAMS_EN_CACHE_ID) }.getOrNull()
            ?.takeIf { englishVersion(it.raw) != null }
        if (device != null && device.version >= wanted) return device.raw
        val fresh = download(wanted)
        if (fresh != null) return fresh
        return if (device != null && device.version > inAppVersion) device.raw else inApp
    }

    private suspend fun download(version: Int): String? {
        val raw = try {
            withTimeoutOrNull(fetchTimeoutMillis) {
                val response = client.get("${baseUrl.trimEnd('/')}/content/hexagrams.en.json?v=$version")
                if (response.status.isSuccess()) response.bodyAsText() else null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        } ?: return null
        if (englishVersion(raw) == null) return null
        runCatching { cache.write(HEXAGRAMS_EN_CACHE_ID, version, raw) }
        return raw
    }
}

/**
 * What the scripture needs from the running app: the language in force and the server's English file
 * version. The app updates it as settings and status change; until then the scripture stays Thai.
 */
class HexagramContentState {
    @kotlin.concurrent.Volatile
    var language: com.iching.ai.domain.model.AppLanguage = com.iching.ai.domain.model.AppLanguage.TH

    @kotlin.concurrent.Volatile
    var hexagramsEnVersion: Int = 1
}
