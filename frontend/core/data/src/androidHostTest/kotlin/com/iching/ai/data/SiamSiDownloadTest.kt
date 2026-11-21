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
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SiamSiDownloadTest {
    private val allTopics = """{"work":"w","money":"m","love":"l","health":"h","travel":"t","lost":"x","dealings":"d"}"""
    private fun thai28(name: String, version: Int? = null) = """{"id":"thai28","name":"$name",${version?.let { "\"version\":$it," } ?: ""}"count":1,"source":{"title":"ตำราไทย","note":"n"},"sticks":[
        {"number":1,"grade_orig":null,"level":"good","label_orig":null,"poem_orig":["ก","ข","ค","ง"],"poem_th":["ก","ข","ค","ง"],
        "story":"s","summary":"sum","topics":$allTopics,"keywords":[]}]}"""

    private val dir = Files.createTempDirectory("siamsi").toOkioPath()
    private val requests = mutableListOf<String>()
    /** What Hosting answers next: a set file, or null for a broken one. */
    private var serve: String? = thai28("เซียมซีไทย")

    /** Downloads of these ids never answer. */
    private val hanging = mutableSetOf<String>()

    private fun repository(bundledThai28: String? = null, fetchTimeoutMillis: Long = 15_000): SiamSiSetRepository {
        val engine = MockEngine { request ->
            requests += request.url.toString()
            if (hanging.any { request.url.encodedPath.endsWith("/$it.json") }) awaitCancellation()
            serve?.let { respond(it, HttpStatusCode.OK) } ?: respondError(HttpStatusCode.InternalServerError)
        }
        return SiamSiSetRepository(
            bundled = if (bundledThai28 == null) {
                BundledSiamSiRepository(bundled = emptyList(), loadRaw = { error("no bundled sets") })
            } else {
                BundledSiamSiRepository(bundled = listOf("thai28"), loadRaw = { bundledThai28 })
            },
            client = HttpClient(engine),
            baseUrl = "https://example.web.app",
            cache = FileSiamSiCache(dir, FileSystem.SYSTEM),
            fetchTimeoutMillis = fetchTimeoutMillis,
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

    @Test
    fun aBundledSetIsUsedWithoutAFetchWhileTheServerHasNoNewerVersion() {
        assertEquals(listOf("ฉบับในแอป"), names(repository(bundledThai28 = thai28("ฉบับในแอป")), 1))
        assertEquals(emptyList(), requests)
    }

    @Test
    fun aNewerServerVersionReplacesTheBundledSet() = runBlocking {
        val repo = repository(bundledThai28 = thai28("ฉบับในแอป"))
        serve = thai28("ฉบับแก้แล้ว", version = 2)
        assertEquals(listOf("ฉบับแก้แล้ว"), names(repo, 2))
        assertEquals("ฉบับแก้แล้ว", repo.set("thai28")?.name)
        // Offline later, a fresh start still draws from the corrected copy.
        serve = null
        assertEquals("ฉบับแก้แล้ว", repository(bundledThai28 = thai28("ฉบับในแอป")).set("thai28")?.name)
    }

    @Test
    fun aHangingDownloadGivesUpAndFallsBackToTheBundledSet() {
        hanging += "thai28"
        assertEquals(listOf("ฉบับในแอป"), names(repository(bundledThai28 = thai28("ฉบับในแอป"), fetchTimeoutMillis = 50), 2))
    }

    @Test
    fun aSlowDownloadDoesNotHoldUpTheJournal() = runBlocking {
        names(repository(), 1)
        val repo = repository()
        hanging += "thai28"
        val slow = launch { repo.available(listOf(SiamSiSetRef("thai28", 2))) }
        yield()
        assertEquals("เซียมซีไทย", withTimeout(2_000) { repo.set("thai28") }?.name)
        slow.cancel()
    }

    @Test
    fun localSetsNeedNoNetwork() = runBlocking {
        names(repository(), 1)
        requests.clear()
        val repo = repository(bundledThai28 = null)
        assertEquals(listOf("เซียมซีไทย"), repo.local(listOf(SiamSiSetRef("thai28", 2))).map { it.name })
        assertEquals(emptyList(), requests)
    }
}
