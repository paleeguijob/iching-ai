package com.iching.ai.data

import com.iching.ai.data.scripture.ScriptureHexagramRepository
import com.iching.ai.data.scripture.applyEnglish
import com.iching.ai.data.scripture.parseEnglish
import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.domain.model.Hexagram
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ScriptureEnglishTest {
    private fun patternOf(i: Int) = (0 until 6).map { if ((i shr it) and 1 == 1) '1' else '0' }.joinToString("")

    private fun thai(id: Int) = Hexagram(
        id = id, nameCn = "乾$id", pinyin = "Qián$id", nameTh = "เฉียน — ฟ้า$id", pattern = patternOf(id),
        keywords = listOf("ก"), judgment = "ตัดสิน", image = "ภาพ", description = "คำอธิบาย",
        lines = List(6) { "เส้น$it" }, allLines = null, dailyMessage = "วันนี้",
    )

    private fun enEntry(id: Int, withMeaning: Boolean = true) = """{"id":$id,"nameEn":"Heaven","meaningEn":"${if (withMeaning) "Strong and unceasing" else ""}",
        "keywordsEn":["strength"],"judgmentEn":"Great beginnings.","imageEn":"Heaven moves.","descriptionEn":"Pure yang.",
        "linesEn":["l1","l2","l3","l4","l5","l6"],"allLinesEn":null,"dailyMessageEn":"Keep going.","yearMessageEn":null}"""

    private fun enFile(count: Int, bad: Int? = null) =
        """{"version":1,"trigrams":{"111":{"natureEn":"Heaven"}},"hexagrams":[${(1..count).joinToString(",") { enEntry(it, withMeaning = it != bad) }}]}"""

    private val thaiAll = (1..64).map(::thai)

    private fun thaiJson() = "[" + thaiAll.joinToString(",") { h ->
        """{"id":${h.id},"nameCn":"${h.nameCn}","pinyin":"${h.pinyin}","nameTh":"${h.nameTh}","pattern":"${h.pattern}",
        "keywords":["ก"],"judgment":"${h.judgment}","image":"${h.image}","description":"${h.description}",
        "lines":[${h.lines.joinToString(",") { "\"$it\"" }}],"dailyMessage":"${h.dailyMessage}"}"""
    } + "]"

    private fun repository(language: AppLanguage, english: String?, reads: MutableList<String> = mutableListOf()) =
        ScriptureHexagramRepository(
            loadRaw = { reads += "th"; thaiJson() },
            loadEnglish = { reads += "en"; english },
            language = { language },
        )

    @Test
    fun applyEnglishReplacesTextAndKeepsStructure() {
        val en = parseEnglish(enFile(1))
        val merged = applyEnglish(listOf(thai(1)), en).single()
        assertEquals("Heaven — Strong and unceasing", merged.nameTh)
        assertEquals("Heaven", merged.nameEn)
        assertEquals(listOf("l1", "l2", "l3", "l4", "l5", "l6"), merged.lines)
        assertEquals("Keep going.", merged.dailyMessage)
        assertEquals(thai(1).pattern, merged.pattern)
        assertEquals("乾1", merged.nameCn)
        assertEquals("Qián1", merged.pinyin)
        assertEquals("Heaven", merged.shortNameTh)
        assertEquals("Strong and unceasing", merged.meaningTh)
    }

    @Test
    fun applyEnglishRejectsAMissingHexagram() {
        val en = parseEnglish(enFile(1))
        assertFailsWith<IllegalStateException> { applyEnglish(listOf(thai(1), thai(2)), en) }
    }

    @Test
    fun trigramsAreReadFromTheFile() {
        assertEquals("Heaven", parseEnglish(enFile(1)).trigrams["111"])
    }

    @Test
    fun englishRepositoryServesEnglishWhenComplete() = runTest {
        val all = repository(AppLanguage.EN, enFile(64)).all()
        assertEquals(64, all.size)
        assertEquals("Heaven — Strong and unceasing", all.first().nameTh)
    }

    @Test
    fun incompleteOverlayFallsBackToThaiWholly() = runTest {
        val all = repository(AppLanguage.EN, enFile(63)).all()
        assertEquals(64, all.size)
        assertEquals(thaiAll, all)
        // A hexagram that is present but has a blank field also sends the whole set back to Thai.
        assertEquals(thaiAll, repository(AppLanguage.EN, enFile(64, bad = 2)).all())
    }

    @Test
    fun missingOrBrokenEnglishFallsBackToThai() = runTest {
        assertEquals(thaiAll, repository(AppLanguage.EN, null).all())
        assertEquals(thaiAll, repository(AppLanguage.EN, "{not json").all())
    }

    @Test
    fun thaiRepositoryNeverReadsEnglish() = runTest {
        val reads = mutableListOf<String>()
        assertEquals(thaiAll, repository(AppLanguage.TH, enFile(64), reads).all())
        assertEquals(listOf("th"), reads)
    }

    @Test
    fun cachesPerLanguage() = runTest {
        var language = AppLanguage.TH
        val reads = mutableListOf<String>()
        val repo = ScriptureHexagramRepository(
            loadRaw = { reads += "th"; thaiJson() },
            loadEnglish = { reads += "en"; enFile(64) },
            language = { language },
        )
        assertEquals("เฉียน — ฟ้า1", repo.byId(1).nameTh)
        language = AppLanguage.EN
        assertEquals("Heaven — Strong and unceasing", repo.byId(1).nameTh)
        language = AppLanguage.TH
        assertEquals("เฉียน — ฟ้า1", repo.byId(1).nameTh)
        assertEquals(listOf("th", "en"), reads)
    }

    @Test
    fun aFailedEnglishLoadIsRetriedNotRemembered() = runTest {
        var english: String? = null
        val repo = ScriptureHexagramRepository(loadEnglish = { english }, language = { AppLanguage.EN }, loadRaw = { thaiJson() })
        assertEquals("เฉียน — ฟ้า1", repo.byId(1).nameTh)
        english = enFile(64)
        assertEquals("Heaven — Strong and unceasing", repo.byId(1).nameTh)
    }

    @Test
    fun aNewEnglishVersionInvalidatesTheEnglishCache() = runTest {
        var version = 1
        var name = "Heaven"
        val repo = ScriptureHexagramRepository(
            loadEnglish = { enFile(64).replace("\"Heaven\"", "\"$name\"") },
            language = { AppLanguage.EN }, englishVersion = { version }, loadRaw = { thaiJson() },
        )
        assertEquals("Heaven", repo.byId(1).nameEn)
        name = "Sky"
        assertEquals("Heaven", repo.byId(1).nameEn)
        version = 2
        assertEquals("Sky", repo.byId(1).nameEn)
    }

    @Test
    fun switchingTheHolderToEnglishGivesEnglish() = runTest {
        val state = com.iching.ai.data.scripture.HexagramContentState()
        var current = AppLanguage.TH
        state.languageSource = { current }
        val repo = ScriptureHexagramRepository(
            loadEnglish = { enFile(64) }, language = { state.language },
            englishVersion = { state.hexagramsEnVersion }, loadRaw = { thaiJson() },
        )
        assertEquals("เฉียน — ฟ้า1", repo.byId(1).nameTh)
        current = AppLanguage.EN
        assertEquals("Heaven — Strong and unceasing", repo.byId(1).nameTh)
    }
}
