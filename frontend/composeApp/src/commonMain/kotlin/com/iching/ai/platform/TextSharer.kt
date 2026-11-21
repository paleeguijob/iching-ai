package com.iching.ai.platform

import androidx.compose.runtime.Composable

/** Opens the system share sheet with plain text, such as a link to the app. */
fun interface TextSharer {
    fun share(text: String)
}

@Composable
expect fun rememberTextSharer(): TextSharer
