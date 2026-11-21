package com.iching.ai.data

import com.iching.ai.data.siamsi.BundledSiamSiRepository
import com.iching.ai.data.siamsi.FileSiamSiCache
import com.iching.ai.data.siamsi.SiamSiContentState
import com.iching.ai.data.siamsi.SiamSiParser
import com.iching.ai.data.siamsi.SiamSiSetRepository
import com.iching.ai.data.siamsi.applyEnglish
import com.iching.ai.data.siamsi.parseSiamSiEnglish
import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.domain.siamsi.SiamSiLevel
import com.iching.ai.domain.siamsi.SiamSiSetRef
import com.iching.ai.domain.siamsi.SiamSiTopic
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
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SiamSiEnglishTest {
    private val topicsTh = """{"work":"w","money":"m","love":"l","health":"h","travel":"t","lost":"x","dealings":"d"}"""
    private val topicsEn = """{"work":"Work ok","money":"Money ok","love":"Love ok","health":"Health ok","travel":"Travel ok","lost":"Lost ok","dealings":"Dealings ok"}"""

    private fun thaiStick(n: Int) = """{"number":$n,"grade_orig":"中吉","level":"fair","label_orig":null,"poem_orig":["一一一一一一一","二二二二二二二","三三三三三三三","四四四四四四四"],
        "poem_th":["ก","ข","ค","ง"],"story":"","summary":"สรุป","topics":$topicsTh,"keywords":["ก"]}"""

    private fun thai(id: String = "guanyin100", count: Int = 2, version: Int = 1) =
        """{"id":"$id","name":"เซียมซีกวนอิม","version":$version,"count":$count,"source":{"title":"觀音靈籤","note":"ตำราโบราณ"},
        "sticks":[${(1..count).joinToString(",") { thaiStick(it) }}]}"""

    private fun enStick(n: Int, story: String? = "", poem: Int = 4, summary: String = "A good sign") =
        """{"number":$n,"poem_en":[${(1..poem).joinToString(",") { "\"line $it\"" }}],${story?.let { "\"story\":\"$it\"," } ?: ""}"summary":"$summary","topics":$topicsEn,"keywords":["good"]}"""

    private fun english(id: String = "guanyin100", sticks: List<String> = listOf(enStick(1), enStick(2)), version: Int = 1) =
        """{"id":"$id","version":$version,"name":"Guanyin Oracle","source":{"note":"Ancient text"},"sticks":[${sticks.joinToString(",")}]}"""

    private val thSet = SiamSiParser.parse(thai())

    @Test
    fun overlayReplacesTextAndKeepsTheChineseStructure() {
        val merged = applyEnglish(thSet, parseSiamSiEnglish(english()))!!
        val stick = merged.stick(1)
        assertEquals(listOf("line 1", "line 2", "line 3", "line 4"), stick.poemTh)
        assertEquals("A good sign", stick.summary)
        assertEquals("Work ok", stick.topics[SiamSiTopic.WORK])
        assertEquals(listOf("good"), stick.keywords)
        assertEquals("Guanyin Oracle", merged.name)
        assertEquals("Ancient text", merged.sourceNote)
        // Taken from the Thai set.
        assertEquals("中吉", stick.gradeOrig)
        assertEquals(SiamSiLevel.FAIR, stick.level)
        assertEquals(thSet.stick(1).poemOrig, stick.poemOrig)
        assertEquals("觀音靈籤", merged.sourceTitle)
        // The slip screen shows poemOrig and then poemTh, and hides the story when it is blank; "Thai only" (one
        // block) applies only when the two are equal, so English keeps the two-block layout as is.
        assertNotEquals(stick.poemOrig, stick.poemTh)
        assertTrue(stick.story.isBlank())
    }

    @Test
    fun anEmptyStoryIsCompleteButAMissingOneIsNot() {
        assertNotNull(applyEnglish(thSet, parseSiamSiEnglish(english(sticks = listOf(enStick(1, story = ""), enStick(2, story = ""))))))
        assertNull(applyEnglish(thSet, parseSiamSiEnglish(english(sticks = listOf(enStick(1), enStick(2, story = null))))))
    }

    @Test
    fun anOverlayMissingASlipOrAFieldIsRejected() {
        assertNull(applyEnglish(thSet, parseSiamSiEnglish(english(sticks = listOf(enStick(1))))))
        assertNull(applyEnglish(thSet, parseSiamSiEnglish(english(sticks = listOf(enStick(1), enStick(2, poem = 3))))))
        assertNull(applyEnglish(thSet, parseSiamSiEnglish(english(sticks = listOf(enStick(1), enStick(2, summary = ""))))))
        assertNull(applyEnglish(thSet, parseSiamSiEnglish(english(id = "jiazi60"))))
    }

    // --- the repository ---

    private val dir = Files.createTempDirectory("siamsi-en").toOkioPath()
    private val requests = mutableListOf<String>()
    private val bundledReads = mutableListOf<String>()
    private var appLanguage = AppLanguage.EN
    private var serverEnVersion = 1
    private var serveEnglish: String? = null

    private fun repository(bundledEnglish: String?, thaiRaw: String = thai()): SiamSiSetRepository {
        val engine = MockEngine { request ->
            requests += request.url.toString()
            serveEnglish?.let { respond(it, HttpStatusCode.OK) } ?: respondError(HttpStatusCode.InternalServerError)
        }
        val content = SiamSiContentState().apply {
            languageSource = { appLanguage }
            enVersionSource = { _ -> serverEnVersion }
        }
        return SiamSiSetRepository(
            bundled = BundledSiamSiRepository(bundled = listOf("guanyin100"), loadRaw = { thaiRaw }),
            client = HttpClient(engine),
            baseUrl = "https://example.web.app",
            cache = FileSiamSiCache(dir, FileSystem.SYSTEM),
            content = content,
            loadBundledEnglish = { bundledReads += it; bundledEnglish },
        )
    }

    private fun names(repo: SiamSiSetRepository, enVer: Int = 1) =
        runBlocking { repo.available(listOf(SiamSiSetRef("guanyin100", 1, enVer))).map { it.name } }

    @Test
    fun englishModeShowsTheEnglishSetAndThaiModeNeverReadsTheOverlay() {
        val repo = repository(english())
        appLanguage = AppLanguage.TH
        assertEquals(listOf("เซียมซีกวนอิม"), names(repo))
        assertEquals(emptyList(), bundledReads)
        // Switching to English in the same session yields English sets.
        appLanguage = AppLanguage.EN
        assertEquals(listOf("Guanyin Oracle"), names(repo))
        assertEquals("line 1", runBlocking { repo.set("guanyin100") }!!.stick(1).poemTh.first())
        appLanguage = AppLanguage.TH
        assertEquals(listOf("เซียมซีกวนอิม"), names(repo))
    }

    @Test
    fun aSetWithoutACompleteOverlayIsHiddenInEnglishButKeepsOpeningOldReadings() {
        for (bad in listOf(null, english(sticks = listOf(enStick(1))), "{nope")) {
            val repo = repository(bad)
            assertEquals(emptyList(), names(repo), "available in EN for $bad")
            assertEquals(emptyList(), runBlocking { repo.local(listOf(SiamSiSetRef("guanyin100", 1))) }.map { it.name })
            assertEquals("เซียมซีกวนอิม", runBlocking { repo.set("guanyin100") }!!.name)
        }
    }

    @Test
    fun aNewerEnVersionDownloadsOnceAndIsCachedUnderTheEnKey() {
        serveEnglish = english(version = 2).replace("Guanyin Oracle", "Guanyin Oracle v2")
        assertEquals(listOf("Guanyin Oracle v2"), names(repository(english()), enVer = 2))
        assertEquals(listOf("https://example.web.app/content/siamsi/guanyin100.en.json?v=2"), requests)
        assertTrue(File(dir.toFile(), "guanyin100.en.json").exists() && File(dir.toFile(), "guanyin100.en.version").readText() == "2")
        // A fresh start reads the cache, not Hosting.
        assertEquals(listOf("Guanyin Oracle v2"), names(repository(english()), enVer = 2))
        assertEquals(1, requests.size)
    }

    @Test
    fun theBundledOverlayIsEnoughWhenTheVersionIsNotNewer() {
        assertEquals(listOf("Guanyin Oracle"), names(repository(english()), enVer = 1))
        assertEquals(emptyList(), requests)
    }

    @Test
    fun aBrokenDownloadIsNeverCachedAndTheBundledOverlayStays() {
        serveEnglish = english(sticks = listOf(enStick(1)), version = 2)
        assertEquals(listOf("Guanyin Oracle"), names(repository(english()), enVer = 2))
        assertFalse(File(dir.toFile(), "guanyin100.en.json").exists())
    }

    @Test
    fun aStaleFileServedForANewerRequestIsNotCachedAsTheNewerVersion() {
        // The server asks for v2 but Hosting still serves the v1 file.
        serveEnglish = english(version = 1).replace("Guanyin Oracle", "Guanyin Oracle stale")
        assertEquals(listOf("Guanyin Oracle"), names(repository(english()), enVer = 2))
        assertFalse(File(dir.toFile(), "guanyin100.en.json").exists())
        assertFalse(File(dir.toFile(), "guanyin100.en.version").exists())
    }

    @Test
    fun aFailureIsNotRememberedAndANewerEnVersionTakesEffectInTheSameSession() {
        val repo = repository(null)
        assertEquals(emptyList(), names(repo))
        // Hosting recovers: the same repository now shows the set.
        serveEnglish = english(version = 2)
        assertEquals(listOf("Guanyin Oracle"), names(repo, enVer = 2))
        // Then the server publishes version 3 while the app runs.
        serveEnglish = english(version = 3).replace("Guanyin Oracle", "Guanyin Oracle v3")
        assertEquals(listOf("Guanyin Oracle v3"), names(repo, enVer = 3))
    }

    // --- the shipped files ---

    private val root = File("src/commonMain/composeResources/files/siamsi")

    @Test
    fun theShippedEnglishFilesAreCompleteAndHaveNoThai() {
        for ((id, count) in listOf("guanyin100" to 100, "jiazi60" to 60)) {
            val th = SiamSiParser.parse(File(root, "$id.json").readText())
            val en = parseSiamSiEnglish(File(root, "$id.en.json").readText())
            val merged = applyEnglish(th, en)
            assertNotNull(merged, "$id overlay must cover every slip")
            assertEquals(count, merged.sticks.size)
            val thai = Regex("[฀-๿]")
            for (s in merged.sticks) {
                assertEquals("", s.story, "$id #${s.number}: every Thai slip has an empty story")
                val text = (s.poemTh + s.summary + s.topics.values + s.keywords).joinToString() + merged.name + merged.sourceNote
                assertFalse(thai.containsMatchIn(text), "$id #${s.number} has Thai text in English mode")
            }
        }
    }
}
