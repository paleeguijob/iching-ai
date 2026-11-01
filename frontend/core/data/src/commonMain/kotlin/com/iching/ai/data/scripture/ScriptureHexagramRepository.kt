package com.iching.ai.data.scripture

import com.iching.ai.data.resources.Res
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.repository.HexagramRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
) {
    fun toDomain() = Hexagram(
        id = id, nameCn = nameCn, pinyin = pinyin, nameTh = nameTh, pattern = pattern,
        keywords = keywords, judgment = judgment, image = image, description = description,
        lines = lines, allLines = allLines,
    )
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

/** The static scripture database (64 hexagrams / 384 lines) bundled with the app. */
class ScriptureHexagramRepository(
    private val loadRaw: suspend () -> String = { Res.readBytes("files/hexagrams.json").decodeToString() },
) : HexagramRepository {

    private val mutex = Mutex()
    private var cache: List<Hexagram>? = null
    private var byPattern: Map<String, Hexagram> = emptyMap()

    override suspend fun all(): List<Hexagram> = mutex.withLock {
        cache ?: ScriptureParser.parse(loadRaw()).also { loaded ->
            cache = loaded
            byPattern = loaded.associateBy { it.pattern }
        }
    }

    override suspend fun byId(id: Int): Hexagram =
        all().getOrNull(id - 1)?.takeIf { it.id == id }
            ?: throw NoSuchElementException("No hexagram #$id")

    override suspend fun byPattern(pattern: String): Hexagram {
        all()
        return byPattern[pattern] ?: throw NoSuchElementException("No hexagram with pattern $pattern")
    }
}
