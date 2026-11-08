package com.iching.ai.data.siamsi

import com.iching.ai.data.resources.Res
import com.iching.ai.domain.repository.SiamSiRepository
import com.iching.ai.domain.siamsi.SiamSiLevel
import com.iching.ai.domain.siamsi.SiamSiSet
import com.iching.ai.domain.siamsi.SiamSiSetRef
import com.iching.ai.domain.siamsi.SiamSiStick
import com.iching.ai.domain.siamsi.SiamSiTopic
import com.iching.ai.domain.siamsi.gradeToLevel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Sets shipped inside the app, so they work offline from the first launch. */
val BUNDLED_SIAMSI_SETS = listOf("guanyin100", "jiazi60")

@Serializable
internal data class SiamSiSourceDto(val title: String, val note: String = "")

@Serializable
internal data class SiamSiStickDto(
    val number: Int,
    @SerialName("grade_orig") val gradeOrig: String? = null,
    val level: String,
    @SerialName("label_orig") val labelOrig: String? = null,
    @SerialName("poem_orig") val poemOrig: List<String>,
    @SerialName("poem_th") val poemTh: List<String>,
    val story: String,
    val summary: String,
    val topics: Map<String, String>,
    val keywords: List<String> = emptyList(),
)

@Serializable
internal data class SiamSiSetDto(
    val id: String,
    val name: String,
    val version: Int = 1,
    val count: Int,
    val source: SiamSiSourceDto,
    val sticks: List<SiamSiStickDto>,
)

internal object SiamSiParser {
    private val json = Json { ignoreUnknownKeys = true }

    /** [strict] also requires every stick 1..count to be present (off while a set is still being written). */
    fun parse(raw: String, strict: Boolean = true): SiamSiSet {
        val dto = json.decodeFromString<SiamSiSetDto>(raw)
        check(dto.source.title.isNotBlank()) { "${dto.id}: source title is missing" }
        val numbers = dto.sticks.map { it.number }
        check(numbers.toSet().size == numbers.size) { "${dto.id}: stick numbers repeat" }
        check(numbers.all { it in 1..dto.count }) { "${dto.id}: stick numbers must be 1..${dto.count}" }
        if (strict) check(numbers.size == dto.count) { "${dto.id}: needs ${dto.count} sticks, found ${numbers.size}" }
        val sticks = dto.sticks.map { it.toDomain(dto.id) }.sortedBy { it.number }
        return SiamSiSet(dto.id, dto.name, dto.count, dto.source.title, dto.source.note, sticks, dto.version)
    }

    private fun SiamSiStickDto.toDomain(setId: String): SiamSiStick {
        val where = "$setId stick $number"
        check(poemOrig.size == 4) { "$where: poem_orig needs 4 lines" }
        check(poemTh.size == 4) { "$where: poem_th needs 4 lines" }
        val parsedLevel = SiamSiLevel.entries.firstOrNull { it.wire == level } ?: error("$where: unknown level $level")
        gradeOrig?.let { grade ->
            val expected = gradeToLevel(grade) ?: error("$where: unknown grade $grade")
            check(expected == parsedLevel) { "$where: level $level contradicts grade $grade" }
        }
        val parsedTopics = SiamSiTopic.entries.associateWith { t ->
            topics[t.wire]?.takeIf { it.isNotBlank() } ?: error("$where: topic ${t.wire} is missing")
        }
        return SiamSiStick(number, gradeOrig, parsedLevel, labelOrig, poemOrig, poemTh, story, summary, parsedTopics, keywords)
    }
}

/** The set chosen on Home while the server still offers it, otherwise the first one it offers. */
fun resolveSelectedSet(selected: String?, available: List<SiamSiSet>): SiamSiSet? =
    available.firstOrNull { it.id == selected } ?: available.firstOrNull()

/** Siam Si sets bundled with the app. */
class BundledSiamSiRepository(
    private val bundled: Collection<String> = BUNDLED_SIAMSI_SETS,
    private val loadRaw: suspend (String) -> String = { Res.readBytes("files/siamsi/$it.json").decodeToString() },
    private val strict: Boolean = false,
) : SiamSiRepository {

    private val mutex = Mutex()
    private val cache = mutableMapOf<String, SiamSiSet?>()

    override suspend fun available(refs: List<SiamSiSetRef>): List<SiamSiSet> = refs.mapNotNull { set(it.id) }

    override suspend fun set(id: String): SiamSiSet? = mutex.withLock {
        if (id !in bundled) return@withLock null
        if (id in cache) return@withLock cache[id]
        val loaded = try {
            val raw = loadRaw(id)
            withContext(Dispatchers.Default) { SiamSiParser.parse(raw, strict) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        cache[id] = loaded
        loaded
    }
}
