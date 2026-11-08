package com.iching.ai.data

import com.iching.ai.data.siamsi.BundledSiamSiRepository
import com.iching.ai.data.siamsi.FileSiamSiCache
import com.iching.ai.data.siamsi.SiamSiSetRepository
import com.iching.ai.domain.siamsi.SiamSiSetRef
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SiamSiDownloadTest {
    private val allTopics = """{"work":"w","money":"m","love":"l","health":"h","travel":"t","lost":"x","dealings":"d"}"""
    private fun thai28(name: String) = """{"id":"thai28","name":"$name","count":1,"source":{"title":"ตำราไทย","note":"n"},"sticks":[
        {"number":1,"grade_orig":null,"level":"good","label_orig":null,"poem_orig":["ก","ข","ค","ง"],"poem_th":["ก","ข","ค","ง"],
        "story":"s","summary":"sum","topics":$allTopics,"keywords":[]}]}"""

    private val dir = Files.createTempDirectory("siamsi").toOkioPath()
    private val requests = mutableListOf<String>()
    /** What Hosting answers next: a set file, or null for a broken one. */
    private var serve: String? = thai28("เซียมซีไทย")

    private fun repository(): SiamSiSetRepository {
        val engine = MockEngine { request ->
            requests += request.url.toString()
            serve?.let { respond(it, HttpStatusCode.OK) } ?: respondError(HttpStatusCode.InternalServerError)
        }
        return SiamSiSetRepository(
            bundled = BundledSiamSiRepository(bundled = emptyList(), loadRaw = { error("no bundled sets") }),
            client = HttpClient(engine),
            baseUrl = "https://example.web.app",
            cache = FileSiamSiCache(dir, FileSystem.SYSTEM),
        )
    }

    private fun names(repo: SiamSiSetRepository, version: Int) = runBlocking { repo.available(listOf(SiamSiSetRef("thai28", version))).map { it.name } }

    @Test
    fun downloadsOnceAndCaches() {
        assertEquals(listOf("เซียมซีไทย"), names(repository(), 1))
        assertEquals(listOf("https://example.web.app/content/siamsi/thai28.json?v=1"), requests)
        // A fresh app start with the same version reads the cache, not Hosting.
        assertEquals(listOf("เซียมซีไทย"), names(repository(), 1))
        assertEquals(1, requests.size)
    }

    @Test
    fun aNewVersionDownloadsAgain() {
        names(repository(), 1)
        serve = thai28("เซียมซีไทย ฉบับสอง")
        assertEquals(listOf("เซียมซีไทย ฉบับสอง"), names(repository(), 2))
        assertEquals(2, requests.size)
    }

    @Test
    fun aBrokenDownloadKeepsTheOldCache() {
        names(repository(), 1)
        serve = """{"id":"thai28","sticks":"nope"}"""
        assertEquals(listOf("เซียมซีไทย"), names(repository(), 2))
        serve = null
        assertEquals(listOf("เซียมซีไทย"), names(repository(), 3))
    }

    @Test
    fun aBrokenDownloadWithNoCacheLeavesTheSetOut() {
        serve = null
        assertEquals(emptyList(), names(repository(), 1))
        serve = "{nope"
        assertEquals(emptyList(), names(repository(), 1))
    }

    @Test
    fun theJournalCanOpenACachedSetOffline() = runBlocking {
        names(repository(), 1)
        serve = null
        assertEquals("เซียมซีไทย", repository().set("thai28")?.name)
        assertNull(repository().set("dragon28"))
    }

    @Test
    fun anIdThatIsNotAPlainNameIsNeverFetched() {
        assertEquals(emptyList(), runBlocking { repository().available(listOf(SiamSiSetRef("../x", 1))) })
        assertEquals(emptyList(), requests)
    }
}
