package com.iching.ai.domain.oracle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadingSectionsTest {

    @Test
    fun plainProseHasNoSections() {
        val text = "ย่อหน้าแรก\n\nย่อหน้าที่สอง\n"
        assertEquals(ReadingParts(text.trim(), emptyList()), parseReading(text))
    }

    @Test
    fun splitsMessageAndThreeSections() {
        val parts = parseReading("ข้อความ\n[ดวงเด่น] ก\n[ควรระวัง] ข\n[คำแนะนำ] ค")
        assertEquals("ข้อความ", parts.message)
        assertEquals(
            listOf(ReadingSection("ดวงเด่น", "ก"), ReadingSection("ควรระวัง", "ข"), ReadingSection("คำแนะนำ", "ค")),
            parts.sections,
        )
    }

    @Test
    fun dropsAHalfMarkerWhileStreaming() {
        val parts = parseReading("ข้อความ\n[ดวงเด่น] ก\n[ควร")
        assertEquals(listOf(ReadingSection("ดวงเด่น", "ก")), parts.sections)
        assertFalse(parts.message.contains("["))
        assertFalse(parts.sections.any { it.text.contains("[") })
    }

    @Test
    fun dropsAHalfMarkerBeforeAnySection() {
        val parts = parseReading("ข้อความ\n[ดว")
        assertEquals("ข้อความ", parts.message)
        assertTrue(parts.sections.isEmpty())
    }

    @Test
    fun skipsEmptyOrMissingSections() {
        val parts = parseReading("ข้อความ\n[ดวงเด่น]\n[คำแนะนำ] ค")
        assertEquals(listOf(ReadingSection("คำแนะนำ", "ค")), parts.sections)
    }

    @Test
    fun speechNamesHeadingsWithoutBrackets() {
        val speech = readingForSpeech("ข้อความ\n[ดวงเด่น] ก\n[ควรระวัง] ข\n[คำแนะนำ] ค")
        assertTrue(speech.contains("ควรระวัง. ข"))
        assertFalse(speech.contains("["))
    }

    @Test
    fun speechUsesTheGivenHeadingInsteadOfTheThaiMarker() {
        val speech = readingForSpeech("Hello\n[ดวงเด่น] a\n[ควรระวัง] b") { s -> if (s.label == "ดวงเด่น") "Going well" else "Watch out" }
        assertEquals("Hello\nGoing well. a\nWatch out. b", speech)
    }

    @Test
    fun toleratesSpacesInsideMarkersAndAColonAfter() {
        val parts = parseReading("ข้อความ\n[ ดวงเด่น ]: ก\n[ควรระวัง] - ข")
        assertEquals(listOf(ReadingSection("ดวงเด่น", "ก"), ReadingSection("ควรระวัง", "ข")), parts.sections)
        assertEquals("ข้อความ", parts.message)
    }

    @Test
    fun boldMarkersLeaveNoStarsBehind() {
        val parts = parseReading("ข้อความ\n**[ดวงเด่น]** ก")
        assertEquals("ข้อความ", parts.message)
        assertEquals(listOf(ReadingSection("ดวงเด่น", "ก")), parts.sections)
    }
}
