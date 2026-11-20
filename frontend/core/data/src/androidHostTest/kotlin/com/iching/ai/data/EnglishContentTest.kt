package com.iching.ai.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Validates the bundled English scripture overlay (files/hexagrams.en.json) against the content rules. */
class EnglishContentTest {

    private val root: JsonObject =
        Json.parseToJsonElement(File("src/commonMain/composeResources/files/hexagrams.en.json").readText()).jsonObject
    private val hexagrams: List<JsonObject> = root.getValue("hexagrams").jsonArray.map { it.jsonObject }

    private fun JsonObject.text(key: String): String = getValue(key).jsonPrimitive.content

    private fun JsonObject.texts(): List<String> = buildList {
        listOf("nameEn", "meaningEn", "judgmentEn", "imageEn", "descriptionEn", "dailyMessageEn").forEach { add(text(it)) }
        addAll(getValue("keywordsEn").jsonArray.map { it.jsonPrimitive.content })
        addAll(getValue("linesEn").jsonArray.map { it.jsonPrimitive.content })
        getValue("allLinesEn").takeIf { it !is JsonNull }?.let { add(it.jsonPrimitive.content) }
        getValue("yearMessageEn").takeIf { it !is JsonNull }?.jsonObject?.values?.forEach { add(it.jsonPrimitive.content) }
    }

    @Test
    fun coversAll64HexagramsInOrder() {
        assertEquals((1..64).toList(), hexagrams.map { it.getValue("id").jsonPrimitive.int })
    }

    @Test
    fun everyHexagramHasCompleteEnglishText() {
        hexagrams.forEach { h ->
            val id = h.getValue("id").jsonPrimitive.int
            listOf("nameEn", "meaningEn", "judgmentEn", "imageEn", "descriptionEn", "dailyMessageEn").forEach {
                assertTrue(h.text(it).isNotBlank(), "hexagram $id $it")
            }
            assertTrue(h.getValue("keywordsEn").jsonArray.isNotEmpty(), "hexagram $id keywordsEn")
            val lines = h.getValue("linesEn").jsonArray
            assertEquals(6, lines.size, "hexagram $id linesEn")
            assertTrue(lines.all { it.jsonPrimitive.content.isNotBlank() }, "hexagram $id linesEn blank")
        }
    }

    @Test
    fun allLinesOnlyForHexagramsOneAndTwo() {
        hexagrams.forEach { h ->
            val id = h.getValue("id").jsonPrimitive.int
            val all = h.getValue("allLinesEn")
            if (id == 1 || id == 2) assertTrue(all !is JsonNull && all.jsonPrimitive.content.isNotBlank(), "hexagram $id allLinesEn")
            else assertTrue(all is JsonNull, "hexagram $id must not have allLinesEn")
        }
    }

    @Test
    fun messagesStayWithinLimits() {
        hexagrams.forEach { h ->
            val id = h.getValue("id").jsonPrimitive.int
            assertTrue(h.text("dailyMessageEn").length <= 120, "hexagram $id dailyMessageEn")
            val y = h.getValue("yearMessageEn").jsonObject
            assertTrue(y.text("overview").length in 1..200, "hexagram $id overview")
            assertTrue(y.text("work").length in 1..80, "hexagram $id work")
            assertTrue(y.text("love").length in 1..80, "hexagram $id love")
            assertTrue(y.text("money").length in 1..80, "hexagram $id money")
            assertTrue(y.text("blessing").length in 1..60, "hexagram $id blessing")
        }
    }

    @Test
    fun containsNoThaiScript() {
        hexagrams.forEach { h ->
            val id = h.getValue("id").jsonPrimitive.int
            h.texts().forEach { t -> assertTrue(t.none { it in '฀'..'๿' }, "hexagram $id has Thai: $t") }
        }
    }

    @Test
    fun namesAreUnique() {
        val names = hexagrams.map { it.text("nameEn") }
        assertEquals(64, names.toSet().size, "duplicate names: ${names.groupBy { it }.filter { it.value.size > 1 }.keys}")
    }

    @Test
    fun hasAllEightTrigrams() {
        val trigrams = root.getValue("trigrams").jsonObject
        assertEquals(
            setOf("111", "110", "101", "100", "011", "010", "001", "000"),
            trigrams.keys,
        )
        trigrams.values.forEach { assertTrue(it.jsonObject.getValue("natureEn").jsonPrimitive.contentOrNull.orEmpty().isNotBlank()) }
    }

    @Test
    fun usesUnifiedTermsAndNoBannedRenderings() {
        val banned = listOf("far-reaching", "prosperous.", "and prosperous", "is prosperous", "good luck", "free of blame", "no blame", "wanderer", "breakthrough")
        hexagrams.forEach { h ->
            val id = h.getValue("id").jsonPrimitive.int
            val scripture = buildList {
                add(h.text("judgmentEn")); add(h.text("imageEn"))
                addAll(h.getValue("linesEn").jsonArray.map { it.jsonPrimitive.content })
            }
            scripture.forEach { t -> banned.forEach { b -> assertTrue(!t.lowercase().contains(b), "hexagram $id uses $b: $t") } }
        }
        assertEquals("Resolution", hexagrams[42].text("nameEn"))
        assertEquals("Traveler", hexagrams[55].text("nameEn"))
    }
}
