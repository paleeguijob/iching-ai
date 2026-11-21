package com.iching.ai.data.siamsi

import com.iching.ai.data.resources.Res
import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.domain.repository.SiamSiRepository
import com.iching.ai.domain.siamsi.SiamSiSet
import com.iching.ai.domain.siamsi.SiamSiSetRef
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path

/** A set file kept on the device, with the version it was downloaded as. */
data class CachedSiamSiSet(val version: Int, val raw: String)

interface SiamSiCache {
    suspend fun read(id: String): CachedSiamSiSet?
    suspend fun write(id: String, version: Int, raw: String)
}

/** One `{id}.json` and one `{id}.version` per set under [dir]. */
class FileSiamSiCache(private val dir: Path, private val fs: FileSystem) : SiamSiCache {
    override suspend fun read(id: String): CachedSiamSiSet? = withContext(Dispatchers.IO) {
        runCatching {
            val version = fs.read(dir / "$id.version") { readUtf8() }.trim().toInt()
            CachedSiamSiSet(version, fs.read(dir / "$id.json") { readUtf8() })
        }.getOrNull()
    }

    override suspend fun write(id: String, version: Int, raw: String): Unit = withContext(Dispatchers.IO) {
        fs.createDirectories(dir)
        // The set first, then its version: a crash in between leaves an old version that is fetched again.
        fs.write(dir / "$id.json") { writeUtf8(raw) }
        fs.write(dir / "$id.version") { writeUtf8(version.toString()) }
        Unit
    }
}

private val SET_ID = Regex("[a-z0-9_]{1,40}")

/** Cache key of a set's English file: `{id}.en.json` + `{id}.en.version`, next to the Thai copy. */
private fun englishCacheId(id: String) = "$id.en"

/**
 * The bundled sets, plus sets the server switches on later (or newer versions of bundled ones), downloaded
 * from Hosting at `{baseUrl}/content/siamsi/{id}.json?v={version}` and kept on the device. A version already
 * on the device is never fetched again; a failed, broken or slow download falls back to the copy on the
 * device, if any. The lock guards only the in-memory map, never a network call, so one slow download holds
 * up neither the other sets nor the journal.
 */
class SiamSiSetRepository(
    private val bundled: BundledSiamSiRepository,
    private val client: HttpClient,
    private val baseUrl: String,
    private val cache: SiamSiCache,
    private val fetchTimeoutMillis: Long = 15_000,
    private val content: SiamSiContentState = SiamSiContentState(),
    private val loadBundledEnglish: suspend (String) -> String? = { Res.readBytes("files/siamsi/$it.en.json").decodeToString() },
) : SiamSiRepository {
    private val mutex = Mutex()
    private val parsed = mutableMapOf<String, Pair<Int, SiamSiSet>>()

    /** An English set already applied: the Thai set's version and the English version it was built from. */
    private class EnglishCopy(val thaiVersion: Int, val enVersion: Int, val set: SiamSiSet)
    private val english = mutableMapOf<String, EnglishCopy>()

    /**
     * In English a set shows only when its English file is complete, so the seeker never meets a mix of the two
     * languages; in Thai the English files are never read.
     */
    override suspend fun available(refs: List<SiamSiSetRef>): List<SiamSiSet> = coroutineScope {
        val lang = content.language
        refs.map { ref -> async { resolve(ref, fetch = true).inLanguage(lang, ref.enVersion, fetch = true) } }.awaitAll().filterNotNull()
    }

    override suspend fun local(refs: List<SiamSiSetRef>): List<SiamSiSet> {
        val lang = content.language
        return refs.mapNotNull { resolve(it, fetch = false).inLanguage(lang, it.enVersion, fetch = false) }
    }

    /**
     * For the journal and the draw: the newest copy on the device, bundled or downloaded, whatever its version.
     * In English, the English text when the device has a complete copy; otherwise the Thai set, so a reading
     * drawn earlier still opens.
     */
    override suspend fun set(id: String): SiamSiSet? {
        val th = newest(bundled.set(id), onDevice(id))
        return if (content.language == AppLanguage.EN) th?.let { englishFor(it, content.enVersion(id), fetch = false) ?: it } else th
    }

    private suspend fun SiamSiSet?.inLanguage(lang: AppLanguage, enVersion: Int, fetch: Boolean): SiamSiSet? =
        if (this == null || lang != AppLanguage.EN) this else englishFor(this, enVersion, fetch)

    /**
     * The English copy of [th]: a complete downloaded or cached file first, then a complete bundled one, else
     * null. A newer [wanted] version than the one on the device is downloaded once. A file is checked against
     * [th] before it is cached, and a failure is never remembered, so the next call tries again.
     */
    private suspend fun englishFor(th: SiamSiSet, wanted: Int, fetch: Boolean): SiamSiSet? {
        if (!SET_ID.matches(th.id)) return null
        mutex.withLock { english[th.id] }?.takeIf { it.thaiVersion == th.version && it.enVersion >= wanted }?.let { return it.set }
        val inApp = applied(th, runCatchingNotCancel { loadBundledEnglish(th.id) })
        val device = runCatchingNotCancel { cache.read(englishCacheId(th.id)) }?.let { cached ->
            applied(th, cached.raw)?.let { cached.version to it.second }
        }
        val best = when {
            inApp != null && inApp.first >= wanted -> inApp
            device != null && device.first >= wanted -> device
            else -> (if (fetch) downloadEnglish(th, wanted) else null)
                ?: if (device != null && device.first > (inApp?.first ?: 0)) device else inApp
        } ?: return null
        mutex.withLock { english[th.id] = EnglishCopy(th.version, best.first, best.second) }
        return best.second
    }

    /** The version recorded in [raw] and [th] with it applied, or null when [raw] is not a complete English file for [th]. */
    private suspend fun applied(th: SiamSiSet, raw: String?): Pair<Int, SiamSiSet>? {
        if (raw == null) return null
        return withContext(Dispatchers.Default) {
            runCatching { parseSiamSiEnglish(raw).let { en -> applyEnglish(th, en)?.let { en.version to it } } }.getOrNull()
        }
    }

    private suspend fun downloadEnglish(th: SiamSiSet, version: Int): Pair<Int, SiamSiSet>? {
        val raw = try {
            withTimeoutOrNull(fetchTimeoutMillis) {
                val response = client.get("${baseUrl.trimEnd('/')}/content/siamsi/${th.id}.en.json?v=$version")
                if (response.status.isSuccess()) response.bodyAsText() else null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        } ?: return null
        val set = applied(th, raw) ?: return null
        // Kept under the version the file says, never the one asked for (see HexagramEnDownloads).
        if (set.first < version) return null
        runCatching { cache.write(englishCacheId(th.id), set.first, raw) }
        return set
    }

    private suspend inline fun <T> runCatchingNotCancel(block: () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private suspend fun resolve(ref: SiamSiSetRef, fetch: Boolean): SiamSiSet? {
        val inApp = bundled.set(ref.id)
        if (inApp != null && inApp.version >= ref.version) return inApp
        val device = onDevice(ref.id)
        if (device != null && device.version >= ref.version) return device
        val fresh = if (fetch) download(ref) else null
        return fresh ?: newest(inApp, device)
    }

    private fun newest(a: SiamSiSet?, b: SiamSiSet?): SiamSiSet? = when {
        a == null -> b
        b == null -> a
        b.version > a.version -> b
        else -> a
    }

    /** A downloaded copy already parsed, or read from the device cache. */
    private suspend fun onDevice(id: String): SiamSiSet? {
        if (!SET_ID.matches(id)) return null
        mutex.withLock { parsed[id] }?.let { return it.second }
        val cached = cache.read(id) ?: return null
        return remember(id, cached)
    }

    private suspend fun download(ref: SiamSiSetRef): SiamSiSet? {
        if (!SET_ID.matches(ref.id)) return null
        val raw = try {
            withTimeoutOrNull(fetchTimeoutMillis) {
                val response = client.get("${baseUrl.trimEnd('/')}/content/siamsi/${ref.id}.json?v=${ref.version}")
                if (response.status.isSuccess()) response.bodyAsText() else null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        } ?: return null
        val set = parse(raw)?.takeIf { it.id == ref.id } ?: return null
        runCatching { cache.write(ref.id, ref.version, raw) }
        mutex.withLock { parsed[ref.id] = ref.version to set }
        return set
    }

    private suspend fun remember(id: String, cached: CachedSiamSiSet): SiamSiSet? {
        val set = parse(cached.raw)?.takeIf { it.id == id } ?: return null
        mutex.withLock { parsed[id] = cached.version to set }
        return set
    }

    private suspend fun parse(raw: String): SiamSiSet? = withContext(Dispatchers.Default) {
        runCatching { SiamSiParser.parse(raw, strict = true) }.getOrNull()
    }
}
