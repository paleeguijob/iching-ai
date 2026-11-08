package com.iching.ai.data

import com.iching.ai.data.siamsi.BundledSiamSiRepository
import com.iching.ai.data.siamsi.SiamSiParser
import com.iching.ai.data.siamsi.resolveSelectedSet
import com.iching.ai.domain.siamsi.SiamSiLevel
import com.iching.ai.domain.siamsi.SiamSiSetRef
import com.iching.ai.domain.siamsi.SiamSiTopic
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SiamSiDataTest {

    private val allTopics = """{"work":"w","money":"m","love":"l","health":"h","travel":"t","lost":"x","dealings":"d"}"""

    private fun stick(
        n: Int,
        grade: String? = "中吉",
        level: String = "fair",
        poem: String = """["一","二","三","四"]""",
        topics: String = allTopics,
    ) = """{"number":$n,"grade_orig":${grade?.let { "\"$it\"" } ?: "null"},"level":"$level","label_orig":null,
        "poem_orig":$poem,"poem_th":["ก","ข","ค","ง"],"story":"s","summary":"sum","topics":$topics,"keywords":["k"]}"""

    private fun set(id: String = "guanyin100", count: Int = 2, sticks: List<String> = listOf(stick(1), stick(2))) =
        """{"id":"$id","name":"เซียมซีเจ้าแม่กวนอิม","count":$count,
        "source":{"title":"觀音靈籤","note":"n"},"sticks":[${sticks.joinToString(",")}]}"""

    @Test
    fun parseReadsAValidSet() {
        val s = SiamSiParser.parse(set())
        assertEquals("guanyin100", s.id)
        assertEquals(2, s.sticks.size)
        assertEquals(SiamSiLevel.FAIR, s.stick(2).level)
        assertEquals("中吉", s.stick(1).gradeOrig)
        assertEquals("w", s.stick(1).topics[SiamSiTopic.WORK])
        assertEquals("觀音靈籤", s.sourceTitle)
    }

    @Test
    fun parseRejectsWrongCount() {
        assertFailsWith<IllegalStateException> { SiamSiParser.parse(set(count = 3)) }
        assertEquals(1, SiamSiParser.parse(set(count = 100, sticks = listOf(stick(1))), strict = false).sticks.size)
    }

    @Test
    fun parseRejectsDuplicateNumber() {
        assertFailsWith<IllegalStateException> { SiamSiParser.parse(set(sticks = listOf(stick(1), stick(1)))) }
    }

    @Test
    fun parseRejectsNumberOutsideCount() {
        assertFailsWith<IllegalStateException> { SiamSiParser.parse(set(sticks = listOf(stick(1), stick(3)))) }
    }

    @Test
    fun parseRejectsThreeLinePoem() {
        val e = assertFailsWith<IllegalStateException> {
            SiamSiParser.parse(set(sticks = listOf(stick(1), stick(2, poem = """["一","二","三"]"""))))
        }
        assertTrue(e.message!!.contains("2"))
    }

    @Test
    fun parseRejectsMissingTopic() {
        assertFailsWith<IllegalStateException> {
            SiamSiParser.parse(set(sticks = listOf(stick(1), stick(2, topics = """{"work":"w"}"""))))
        }
    }

    @Test
    fun parseRejectsLevelThatContradictsGrade() {
        assertFailsWith<IllegalStateException> {
            SiamSiParser.parse(set(sticks = listOf(stick(1), stick(2, grade = "上上", level = "caution"))))
        }
        // Sets without a grade (Thai sets) take the level as written.
        assertEquals(SiamSiLevel.GOOD, SiamSiParser.parse(set(sticks = listOf(stick(1), stick(2, grade = null, level = "good")))).stick(2).level)
    }

    @Test
    fun availableSkipsBrokenSetsAndKeepsOrder() = runBlocking {
        val files = mapOf("jiazi60" to set("jiazi60"), "guanyin100" to set("guanyin100"), "broken" to "{nope")
        val repo = BundledSiamSiRepository(bundled = files.keys, loadRaw = { files.getValue(it) }, strict = true)
        val refs = listOf("jiazi60", "broken", "notBundled", "guanyin100").map { SiamSiSetRef(it, 1) }
        assertEquals(listOf("jiazi60", "guanyin100"), repo.available(refs).map { it.id })
        assertEquals("guanyin100", repo.set("guanyin100")?.id)
        assertNull(repo.set("broken"))
    }

    @Test
    fun bundledSetsParse() {
        for (id in com.iching.ai.data.siamsi.BUNDLED_SIAMSI_SETS) {
            val raw = java.io.File("src/commonMain/composeResources/files/siamsi/$id.json").readText()
            assertEquals(id, SiamSiParser.parse(raw, strict = false).id)
        }
    }

    @Test
    fun resolveFallsBackToFirstWhenSelectedRemoved() {
        val sets = listOf(SiamSiParser.parse(set("guanyin100")), SiamSiParser.parse(set("jiazi60")))
        assertEquals("jiazi60", resolveSelectedSet("jiazi60", sets)?.id)
        assertEquals("guanyin100", resolveSelectedSet("thai28", sets)?.id)
        assertEquals("guanyin100", resolveSelectedSet(null, sets)?.id)
        assertNull(resolveSelectedSet("x", emptyList()))
    }
}
