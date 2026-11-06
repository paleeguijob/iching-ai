package com.iching.ai.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/** Opens the system share sheet with an image and a line of text. */
interface ImageSharer {
    /** True when the share sheet opened; it doesn't say whether the person went on to share. */
    suspend fun share(png: ByteArray, text: String): Boolean
}

@Composable
expect fun rememberImageSharer(): ImageSharer

expect fun ImageBitmap.encodePng(): ByteArray
