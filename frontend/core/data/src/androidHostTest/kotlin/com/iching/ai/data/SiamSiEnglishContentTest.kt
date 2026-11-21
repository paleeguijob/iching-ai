package com.iching.ai.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Validates the bundled English Siam Si files (files/siamsi/{id}.en.json) against the Thai set files. */
class SiamSiEnglishContentTest {

    private val dir = "src/commonMain/composeResources/files/siamsi"
    private val topicKeys = listOf("work", "money", "love", "health", "travel", "lost", "dealings")
    private val thai = '\u0E00'..'\u0E7F'

    private fun load(name: String): JsonObject = Json.parseToJsonElement(File("$dir/$name").readText()).jsonObject

    private fun JsonObject.sticks() = getValue("sticks").jsonArray.map { it.jsonObject }

    private fun JsonObject.strings(key: String) = getValue(key).jsonArray.map { it.jsonPrimitive.content }

    private fun forEachSet(block: (id: String, th: JsonObject, en: JsonObject) -> Unit) {
        listOf("guanyin100", "jiazi60").forEach { id -> block(id, load("$id.json"), load("$id.en.json")) }
    }

    @Test
    fun headerMatchesThaiSet() = forEachSet { id, th, en ->
        assertEquals(id, en.getValue("id").jsonPrimitive.content)
        assertEquals(1, en.getValue("version").jsonPrimitive.int)
        assertTrue(en.getValue("name").jsonPrimitive.content.isNotBlank(), "$id name")
        assertTrue(en.getValue("source").jsonObject.getValue("note").jsonPrimitive.content.isNotBlank(), "$id note")
        assertFalse(en.containsKey("partial"), "$id partial")
        assertEquals(th.getValue("count").jsonPrimitive.int, en.sticks().size, "$id stick count")
    }

    @Test
    fun setNamesDoNotRepeatTheCountTheAppAddsItself() = forEachSet { id, _, en ->
        val name = en.getValue("name").jsonPrimitive.content
        assertFalse(name.any { it.isDigit() }, "$id name has digits: $name")
        assertFalse(name.contains("Slips", ignoreCase = true), "$id name says Slips: $name")
    }

    @Test
    fun everyStickIsCompleteAndMatchesThai() = forEachSet { id, th, en ->
        val thSticks = th.sticks()
        val enSticks = en.sticks()
        assertEquals(thSticks.map { it.getValue("number").jsonPrimitive.int }, enSticks.map { it.getValue("number").jsonPrimitive.int }, "$id numbers")
        thSticks.zip(enSticks).forEach { (t, e) ->
            val n = e.getValue("number").jsonPrimitive.int
            assertEquals(t.strings("poem_orig").size, e.strings("poem_en").size, "$id $n poem lines")
            assertTrue(e.strings("poem_en").all { it.isNotBlank() }, "$id $n blank poem line")
            assertTrue(e.getValue("summary").jsonPrimitive.content.isNotBlank(), "$id $n summary")
            val topics = e.getValue("topics").jsonObject
            assertEquals(topicKeys.toSet(), topics.keys, "$id $n topic keys")
            assertTrue(topics.values.all { it.jsonPrimitive.content.isNotBlank() }, "$id $n blank topic")
            assertTrue(e.strings("keywords").isNotEmpty(), "$id $n keywords")
            assertEquals(t.getValue("story").jsonPrimitive.content.isEmpty(), e.getValue("story").jsonPrimitive.content.isEmpty(), "$id $n story")
        }
    }

    @Test
    fun noThaiScript() = forEachSet { id, _, en ->
        assertTrue(en.toString().none { it in thai }, "$id has Thai characters")
    }

    @Test
    fun styleIsUniform() = forEachSet { id, _, en ->
        en.sticks().forEach { e ->
            val n = e.getValue("number").jsonPrimitive.int
            e.strings("poem_en").forEach { assertTrue(it.first().isUpperCase(), "$id $n poem line '$it'") }
            e.getValue("topics").jsonObject.values.forEach { assertTrue(it.jsonPrimitive.content.last() in ".?!", "$id $n topic end") }
            e.strings("keywords").forEach { assertTrue(it.first().isUpperCase(), "$id $n keyword '$it'") }
        }
    }
}
