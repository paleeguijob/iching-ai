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

    private fun file(version: Int) = """{"version":$version,"hexagrams":[]}"""

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
    fun failedOrBrokenDownloadFallsBackToBundled() = runBlocking {
        serverVersion = 2
        serve = null
        assertEquals(file(1), downloads(file(1)).load())
        serve = "{not json"
        assertEquals(file(1), downloads(file(1)).load())
    }

    @Test
    fun hostingVersionNewerThanBundleIsWhatTheRepositoryServes() = runBlocking {
        val root = File("src/commonMain/composeResources/files")
        val thaiRaw = File(root, "hexagrams.json").readText()
        val bundled = File(root, "hexagrams.en.json").readText()
        serverVersion = 2
        serve = bundled.replaceFirst("\"version\": 1", "\"version\": 2").replace("\"nameEn\": \"Heaven\"", "\"nameEn\": \"Sky\"")
        val dl = downloads(bundled)
        val repo = ScriptureHexagramRepository(loadRaw = { thaiRaw }, loadEnglish = dl::load, language = { AppLanguage.EN })
        assertEquals("Sky", repo.byId(1).nameEn)
    }

    @Test
    fun theBundledEnglishFileCoversAllSixtyFour() = runBlocking {
        val root = File("src/commonMain/composeResources/files")
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
