package com.iching.ai.data.siamsi

import com.iching.ai.domain.siamsi.SiamSiSet
import com.iching.ai.domain.siamsi.SiamSiTopic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class SiamSiEnglishSourceDto(val note: String = "")

@Serializable
internal data class SiamSiEnglishStickDto(
    val number: Int,
    @SerialName("poem_en") val poemEn: List<String>? = null,
    // Nullable on purpose: every Thai slip has an empty story, so "" is complete and a missing key is not.
    val story: String? = null,
    val summary: String = "",
    val topics: Map<String, String>? = null,
    val keywords: List<String>? = null,
)

@Serializable
internal data class SiamSiEnglishDto(
    val id: String = "",
    val version: Int = 1,
    val name: String = "",
    val source: SiamSiEnglishSourceDto? = null,
    val sticks: List<SiamSiEnglishStickDto> = emptyList(),
)

/** A parsed `{id}.en.json`. Completeness is judged against the Thai set by [applyEnglish]. */
class SiamSiEnglish internal constructor(val id: String, val version: Int, internal val dto: SiamSiEnglishDto)

private val englishJson = Json { ignoreUnknownKeys = true }

/** Throws on malformed JSON. */
fun parseSiamSiEnglish(raw: String): SiamSiEnglish {
    val dto = englishJson.decodeFromString<SiamSiEnglishDto>(raw)
    return SiamSiEnglish(dto.id, dto.version, dto)
}

/**
 * [th] with the English text laid over it: the set's name and note, and every slip's poem (in
 * [com.iching.ai.domain.siamsi.SiamSiStick.poemTh]), story, summary, topics and keywords. The Chinese poem,
 * grade, level and label stay. Null when [en] is for another set, or does not cover every slip of [th] with
 * a four-line poem, a story (an empty one is fine, a missing one is not), a summary, all seven topics and a
 * keyword list: the caller then hides the set, never shows a mix.
 */
fun applyEnglish(th: SiamSiSet, en: SiamSiEnglish): SiamSiSet? {
    val dto = en.dto
    if (dto.id != th.id || dto.name.isBlank()) return null
    val byNumber = dto.sticks.associateBy { it.number }
    val sticks = th.sticks.map { stick ->
        val e = byNumber[stick.number] ?: return null
        val poem = e.poemEn
        val story = e.story
        val topics = e.topics
        val keywords = e.keywords
        if (poem == null || poem.size != 4 || poem.any { it.isBlank() } || story == null || e.summary.isBlank() ||
            topics == null || keywords == null
        ) {
            return null
        }
        val englishTopics = SiamSiTopic.entries.associateWith { t -> topics[t.wire]?.takeIf { it.isNotBlank() } ?: return null }
        stick.copy(poemTh = poem, story = story, summary = e.summary, topics = englishTopics, keywords = keywords)
    }
    return th.copy(
        name = dto.name,
        sourceNote = dto.source?.note?.takeIf { it.isNotBlank() } ?: th.sourceNote,
        sticks = sticks,
    )
}
