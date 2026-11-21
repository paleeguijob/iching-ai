package com.iching.ai.data

import com.iching.ai.data.scripture.HexagramEnDownloads
import com.iching.ai.data.scripture.ScriptureHexagramRepository
import com.iching.ai.data.siamsi.FileSiamSiCache
import com.iching.ai.domain.model.AppLanguage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class HexagramEnDownloadTest {
    private val dir = Files.createTempDirectory("hexen").toOkioPath()
    private val requests = mutableListOf<String>()
    private var serve: String? = null
    private var serverVersion = 1

    private val root = File("src/commonMain/composeResources/files")
    private val real = File(root, "hexagrams.en.json").readText()

    /** The real, complete file stamped with [version]. */
    private fun file(version: Int) = real.replaceFirst("\"version\": 1", "\"version\": $version")

    /** Parses, but is missing hexagram 64. */
    private fun incomplete(version: Int) = file(version).replace("\"id\": 64,", "\"id\": 640,")

    private fun downloads(bundled: String?): HexagramEnDownloads {
        val engine = MockEngine { request ->
            requests += request.url.toString()
            serve?.let { respond(it, HttpStatusCode.OK) } ?: respondError(HttpStatusCode.InternalServerError)
        }
        return HexagramEnDownloads(
            bundled = { bundled },
            client = HttpClient(engine),
            baseUrl = "https://example.web.app/",
            cache = FileSiamSiCache(dir, FileSystem.SYSTEM),
            serverVersion = { serverVersion },
        )
    }

    @Test
    fun bundledIsUsedUnlessTheServerIsNewer() = runBlocking {
        serve = file(5)
        assertEquals(file(1), downloads(file(1)).load())
        assertEquals(emptyList(), requests)
    }

    @Test
    fun newerServerVersionDownloadsOnceThenCaches() = runBlocking {
        serverVersion = 2
        serve = file(2)
        assertEquals(file(2), downloads(file(1)).load())
        assertEquals(listOf("https://example.web.app/content/hexagrams.en.json?v=2"), requests)
        serve = null
        assertEquals(file(2), downloads(file(1)).load())
        assertEquals(1, requests.size)
    }

    @Test
    fun aStaleFileServedForANewerRequestIsNotCachedAsTheNewerVersion() = runBlocking {
        serverVersion = 2
        serve = file(1)
        assertEquals(file(1), downloads(file(1)).load())
        assertEquals(null, FileSiamSiCache(dir, FileSystem.SYSTEM).read("hexagrams_en"))
        // Hosting catches up: the next launch downloads again instead of trusting a v2 label on v1 text.
        serve = file(2)
        assertEquals(file(2), downloads(file(1)).load())
        assertEquals(2, requests.size)
        assertEquals(2, FileSiamSiCache(dir, FileSystem.SYSTEM).read("hexagrams_en")!!.version)
    }

    @Test
    fun failedOrBrokenDownloadFallsBackToBundled() = runBlocking {
        serverVersion = 2
        serve = null
        assertEquals(file(1), downloads(file(1)).load())
        serve = "{not json"
        assertEquals(file(1), downloads(file(1)).load())
    }

    @Test
    fun incompleteDownloadIsNotCachedAndEnglishComesFromTheBundle() = runBlocking {
        serverVersion = 2
        serve = incomplete(2)
        val thaiRaw = File(root, "hexagrams.json").readText()
        val dl = downloads(file(1))
        assertEquals(file(1), dl.load())
        // Nothing was cached: a later launch with a good download is served by Hosting, not a stale bad copy.
        serve = file(2)
        assertEquals(file(2), downloads(file(1)).load())
        assertEquals(2, requests.size)
        val repo = ScriptureHexagramRepository(loadRaw = { thaiRaw }, loadEnglish = downloads(file(1))::load, language = { AppLanguage.EN })
        assertNotNull(repo.byId(1).nameEn.takeIf { it.isNotBlank() })
        Unit
    }

    @Test
    fun incompleteCachedCopyIsIgnored() = runBlocking {
        serverVersion = 2
        FileSiamSiCache(dir, FileSystem.SYSTEM).write("hexagrams_en", 2, incomplete(2))
        serve = null
        assertEquals(file(1), downloads(file(1)).load())
    }

    @Test
    fun hostingVersionNewerThanBundleIsWhatTheRepositoryServes() = runBlocking {
        val thaiRaw = File(root, "hexagrams.json").readText()
        val bundled = real
        serverVersion = 2
        serve = file(2).replace("\"nameEn\": \"Heaven\"", "\"nameEn\": \"Sky\"")
        val dl = downloads(bundled)
        val repo = ScriptureHexagramRepository(loadRaw = { thaiRaw }, loadEnglish = dl::load, language = { AppLanguage.EN })
        assertEquals("Sky", repo.byId(1).nameEn)
    }

    @Test
    fun theBundledEnglishFileCoversAllSixtyFour() = runBlocking {
        val repo = ScriptureHexagramRepository(
            loadRaw = { File(root, "hexagrams.json").readText() },
            loadEnglish = { File(root, "hexagrams.en.json").readText() },
            language = { AppLanguage.EN },
        )
        val all = repo.all()
        assertEquals(64, all.size)
        assertEquals("乾", all.first().nameCn)
        assertNotNull(all.first().nameEn.takeIf { it.isNotBlank() })
        assert(all.none { h -> (h.nameTh + h.judgment + h.description + h.dailyMessage + h.lines.joinToString()).any { it in '฀'..'๿' } })
    }
}
