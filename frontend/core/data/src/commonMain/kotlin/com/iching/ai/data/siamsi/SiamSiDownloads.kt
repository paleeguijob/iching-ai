package com.iching.ai.data.siamsi

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
) : SiamSiRepository {
    private val mutex = Mutex()
    private val parsed = mutableMapOf<String, Pair<Int, SiamSiSet>>()

    override suspend fun available(refs: List<SiamSiSetRef>): List<SiamSiSet> = coroutineScope {
        refs.map { ref -> async { resolve(ref, fetch = true) } }.awaitAll().filterNotNull()
    }

    override suspend fun local(refs: List<SiamSiSetRef>): List<SiamSiSet> = refs.mapNotNull { resolve(it, fetch = false) }

    /** For the journal and the draw: the newest copy on the device, bundled or downloaded, whatever its version. */
    override suspend fun set(id: String): SiamSiSet? = newest(bundled.set(id), onDevice(id))

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
