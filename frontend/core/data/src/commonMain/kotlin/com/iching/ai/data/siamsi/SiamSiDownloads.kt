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
 * The bundled sets, plus sets the server switches on later, downloaded from Hosting at
 * `{baseUrl}/content/siamsi/{id}.json?v={version}` and kept on the device. A version already on the device
 * is never fetched again; a failed or broken download falls back to the copy on the device, if any.
 */
class SiamSiSetRepository(
    private val bundled: BundledSiamSiRepository,
    private val client: HttpClient,
    private val baseUrl: String,
    private val cache: SiamSiCache,
) : SiamSiRepository {
    private val mutex = Mutex()
    private val parsed = mutableMapOf<String, Pair<Int, SiamSiSet>>()

    override suspend fun available(refs: List<SiamSiSetRef>): List<SiamSiSet> =
        refs.mapNotNull { ref -> bundled.set(ref.id) ?: downloaded(ref) }

    /** For the journal: a bundled set, or whatever version of a downloaded one is on the device. */
    override suspend fun set(id: String): SiamSiSet? = bundled.set(id) ?: mutex.withLock {
        if (!SET_ID.matches(id)) return@withLock null
        parsed[id]?.second ?: cache.read(id)?.let { remember(id, it) }
    }

    private suspend fun downloaded(ref: SiamSiSetRef): SiamSiSet? = mutex.withLock {
        if (!SET_ID.matches(ref.id)) return@withLock null
        parsed[ref.id]?.let { (version, set) -> if (version == ref.version) return@withLock set }
        val cached = cache.read(ref.id)
        if (cached?.version == ref.version) remember(ref.id, cached)?.let { return@withLock it }
        val fresh = try {
            val response = client.get("${baseUrl.trimEnd('/')}/content/siamsi/${ref.id}.json?v=${ref.version}")
            if (!response.status.isSuccess()) null else response.bodyAsText().takeIf { parse(it)?.id == ref.id }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        if (fresh != null) {
            runCatching { cache.write(ref.id, ref.version, fresh) }
            return@withLock remember(ref.id, CachedSiamSiSet(ref.version, fresh))
        }
        cached?.let { remember(ref.id, it) }
    }

    private suspend fun remember(id: String, cached: CachedSiamSiSet): SiamSiSet? =
        parse(cached.raw)?.takeIf { it.id == id }?.also { parsed[id] = cached.version to it }

    private suspend fun parse(raw: String): SiamSiSet? = withContext(Dispatchers.Default) {
        runCatching { SiamSiParser.parse(raw, strict = true) }.getOrNull()
    }
}
