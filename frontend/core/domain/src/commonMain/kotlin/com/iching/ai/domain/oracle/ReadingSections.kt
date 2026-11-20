package com.iching.ai.domain.oracle

/** One card under a reading: a heading the server's prompt fixes, and its sentence or two. */
data class ReadingSection(val label: String, val text: String)

/** A reading as the seeker sees it: the hermit's short message, then the cards (none for older readings). */
data class ReadingParts(val message: String, val sections: List<ReadingSection>)

/** Must match READING_SECTIONS in the backend's prompt.ts, in the same order. */
private val SECTION_MARKER = Regex("\\[(ดวงเด่น|ควรระวัง|คำแนะนำ)\\]")

/** A marker the stream has only half sent, e.g. "[ควร" at the very end. */
private val PARTIAL_MARKER = Regex("\\[[^\\]\\n]{0,12}$")

/**
 * Splits a reading into its message and cards. Text with no markers (every reading made before cards,
 * and any reply where the model ignored the format) comes back whole as the message.
 */
fun parseReading(text: String): ReadingParts {
    val full = text.replace(PARTIAL_MARKER, "")
    val markers = SECTION_MARKER.findAll(full).toList()
    if (markers.isEmpty()) return ReadingParts(full.trim(), emptyList())
    val sections = markers.mapIndexedNotNull { i, m ->
        val end = markers.getOrNull(i + 1)?.range?.first ?: full.length
        val body = stripMarkup(full.substring(m.range.last + 1, end))
        body.takeIf { it.isNotEmpty() }?.let { ReadingSection(m.groupValues[1], it) }
    }
    return ReadingParts(full.substring(0, markers.first().range.first).trim(), sections)
}

/** The reading for the device voice: the message, then each card with its heading said as a word. */
fun readingForSpeech(text: String): String {
    val parts = parseReading(text)
    return (listOf(parts.message) + parts.sections.map { "${it.label}. ${it.text}" })
        .filter { it.isNotBlank() }
        .joinToString("\n")
}
