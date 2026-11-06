package com.iching.ai.domain.oracle

private val MARKUP = Regex("[*#_`]")
private val SPACES = Regex("[ \\t]+")

/** The prompt asks for plain text, but a model can still return markdown; cards and speech must not show it. */
fun stripMarkup(text: String): String = text.replace(MARKUP, "").replace(SPACES, " ").trim()

/**
 * Splits [text] into short pieces for a speech engine: each paragraph apart, then at most [maxChars]
 * each, at spaces when possible. Short pieces matter because the first one decides when the voice starts.
 */
fun splitForSpeech(text: String, maxChars: Int): List<String> =
    text.lines().flatMap { splitLine(it, maxChars) }

private fun splitLine(line: String, maxChars: Int): List<String> {
    val parts = mutableListOf<String>()
    var rest = line.trim()
    while (rest.isNotEmpty()) {
        if (rest.length <= maxChars) {
            parts += rest
            break
        }
        val cut = rest.take(maxChars + 1).indexOfLast { it.isWhitespace() }.takeIf { it > 0 } ?: maxChars
        parts += rest.take(cut).trimEnd()
        rest = rest.drop(cut).trimStart()
    }
    return parts
}
