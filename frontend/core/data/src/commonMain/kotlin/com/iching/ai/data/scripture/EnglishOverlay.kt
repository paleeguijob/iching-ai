package com.iching.ai.data.scripture

import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.YearMessage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The bundled English hexagram file; Hosting may serve a newer one at `/content/hexagrams.en.json`. */
internal const val BUNDLED_ENGLISH_HEXAGRAMS = "files/hexagrams.en.json"

@Serializable
internal data class EnglishYearDto(
    val overview: String = "",
    val work: String = "",
    val love: String = "",
    val money: String = "",
    val blessing: String = "",
) {
    fun isComplete() = overview.isNotBlank() && work.isNotBlank() && love.isNotBlank() && money.isNotBlank() && blessing.isNotBlank()
    fun toDomain() = YearMessage(overview, work, love, money, blessing)
}

@Serializable
internal data class EnglishHexagramDto(
    val id: Int,
    val nameEn: String = "",
    val meaningEn: String = "",
    val keywordsEn: List<String>? = null,
    val judgmentEn: String = "",
    val imageEn: String = "",
    val descriptionEn: String = "",
    val linesEn: List<String>? = null,
    val allLinesEn: String? = null,
    val dailyMessageEn: String = "",
    val yearMessageEn: EnglishYearDto? = null,
)

@Serializable
internal data class EnglishTrigramDto(val natureEn: String = "")

@Serializable
internal data class EnglishFileDto(
    val version: Int = 1,
    val trigrams: Map<String, EnglishTrigramDto> = emptyMap(),
    val hexagrams: List<EnglishHexagramDto>,
)

/** The parsed `hexagrams.en.json`: [trigrams] maps a three-character pattern ("111") to its English nature. */
class EnglishScripture internal constructor(
    val version: Int,
    val trigrams: Map<String, String>,
    internal val hexagrams: Map<Int, EnglishHexagramDto>,
)

private val englishJson = Json { ignoreUnknownKeys = true }

/** Throws on malformed JSON. Completeness is checked against the Thai scripture by [applyEnglish]. */
fun parseEnglish(raw: String): EnglishScripture {
    val dto = englishJson.decodeFromString<EnglishFileDto>(raw)
    return EnglishScripture(dto.version, dto.trigrams.mapValues { it.value.natureEn }, dto.hexagrams.associateBy { it.id })
}

/** The version recorded in a `hexagrams.en.json`, or null when it cannot be read. */
fun englishVersion(raw: String): Int? = runCatching { englishJson.decodeFromString<EnglishFileDto>(raw).version }.getOrNull()

/**
 * [th] with every text field replaced by its English counterpart; the structure (id, pattern, Chinese name,
 * pinyin) is kept. [Hexagram.nameTh] becomes "Name — meaning" so the short name and meaning accessors work.
 * Throws [IllegalStateException] when any hexagram of [th] has no complete English entry: the caller then
 * serves Thai for all, never a mix.
 */
fun applyEnglish(th: List<Hexagram>, en: EnglishScripture): List<Hexagram> = th.map { h ->
    val e = en.hexagrams[h.id]
    val keywords = e?.keywordsEn
    val lines = e?.linesEn
    check(
        e != null && e.nameEn.isNotBlank() && e.meaningEn.isNotBlank() && e.judgmentEn.isNotBlank() &&
            e.imageEn.isNotBlank() && e.descriptionEn.isNotBlank() && e.dailyMessageEn.isNotBlank() &&
            keywords != null && lines != null && lines.size == 6 && lines.none { it.isBlank() } &&
            (h.allLines == null || !e.allLinesEn.isNullOrBlank()) &&
            (h.yearMessage == null || e.yearMessageEn?.isComplete() == true),
    ) { "English text for hexagram ${h.id} is missing or incomplete" }
    h.copy(
        nameTh = "${e.nameEn} — ${e.meaningEn}",
        nameEn = e.nameEn,
        keywords = keywords!!,
        judgment = e.judgmentEn,
        image = e.imageEn,
        description = e.descriptionEn,
        lines = lines!!,
        allLines = e.allLinesEn,
        dailyMessage = e.dailyMessageEn,
        yearMessage = e.yearMessageEn?.toDomain(),
    )
}
