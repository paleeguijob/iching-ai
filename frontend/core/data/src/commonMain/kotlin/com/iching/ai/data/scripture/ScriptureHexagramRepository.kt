package com.iching.ai.data.scripture

import com.iching.ai.data.resources.Res
import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.YearMessage
import com.iching.ai.domain.repository.HexagramRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class HexagramDto(
    val id: Int,
    val nameCn: String,
    val pinyin: String,
    val nameTh: String,
    val pattern: String,
    val keywords: List<String> = emptyList(),
    val judgment: String,
    val image: String,
    val description: String,
    val lines: List<String>,
    val allLines: String? = null,
    val dailyMessage: String = "",
    val yearMessage: YearMessageDto? = null,
) {
    fun toDomain() = Hexagram(
        id = id, nameCn = nameCn, pinyin = pinyin, nameTh = nameTh, pattern = pattern,
        keywords = keywords, judgment = judgment, image = image, description = description,
        lines = lines, allLines = allLines, dailyMessage = dailyMessage,
        yearMessage = yearMessage?.toDomain(),
    )
}

@Serializable
internal data class YearMessageDto(
    val overview: String,
    val work: String,
    val love: String,
    val money: String,
    val blessing: String,
) {
    fun toDomain() = YearMessage(overview, work, love, money, blessing)
}

internal object ScriptureParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(raw: String): List<Hexagram> {
        val hexagrams = json.decodeFromString<List<HexagramDto>>(raw).map { it.toDomain() }
        check(hexagrams.size == 64) { "Scripture must contain 64 hexagrams, found ${hexagrams.size}" }
        check(hexagrams.map { it.pattern }.toSet().size == 64) { "Hexagram patterns must be unique" }
        check(hexagrams.all { it.lines.size == 6 && it.pattern.length == 6 }) { "Each hexagram needs 6 lines" }
        return hexagrams.sortedBy { it.id }
    }
}

/**
 * The static scripture database (64 hexagrams / 384 lines) bundled with the app, in the language [language]
 * returns at the time of each call. English is the Thai scripture with the English overlay applied; an
 * overlay that cannot be loaded, is not valid or does not cover all 64 hexagrams leaves the whole set in
 * Thai (never a mix). Thai never reads the English file.
 */
class ScriptureHexagramRepository(
    private val loadEnglish: suspend () -> String? = { Res.readBytes(BUNDLED_ENGLISH_HEXAGRAMS).decodeToString() },
    private val language: () -> AppLanguage = { AppLanguage.TH },
    // Last, so the existing `ScriptureHexagramRepository { raw }` call keeps binding its lambda to the Thai loader.
    private val loadRaw: suspend () -> String = { Res.readBytes("files/hexagrams.json").decodeToString() },
) : HexagramRepository {

    private class Loaded(val list: List<Hexagram>) {
        val byPattern: Map<String, Hexagram> = list.associateBy { it.pattern }
    }

    private val mutex = Mutex()
    private var thai: Loaded? = null
    private var english: Loaded? = null

    private suspend fun loaded(): Loaded = mutex.withLock {
        val th = thai ?: Loaded(
            // Parsing nearly 300 KB of JSON takes long enough on a phone to drop frames on the main thread.
            loadRaw().let { raw -> withContext(Dispatchers.Default) { ScriptureParser.parse(raw) } },
        ).also { thai = it }
        if (language() != AppLanguage.EN) return@withLock th
        english ?: (loadEnglishOverlay(th) ?: th).also { english = it }
    }

    private suspend fun loadEnglishOverlay(th: Loaded): Loaded? = try {
        val raw = loadEnglish()
        if (raw == null) {
            println("Scripture: English hexagrams unavailable; staying in Thai")
            null
        } else {
            withContext(Dispatchers.Default) { Loaded(applyEnglish(th.list, parseEnglish(raw))) }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        println("Scripture: English hexagrams unusable (${e.message}); staying in Thai")
        null
    }

    override suspend fun all(): List<Hexagram> = loaded().list

    override suspend fun byId(id: Int): Hexagram =
        all().getOrNull(id - 1)?.takeIf { it.id == id }
            ?: throw NoSuchElementException("No hexagram #$id")

    override suspend fun byPattern(pattern: String): Hexagram =
        loaded().byPattern[pattern] ?: throw NoSuchElementException("No hexagram with pattern $pattern")
}
