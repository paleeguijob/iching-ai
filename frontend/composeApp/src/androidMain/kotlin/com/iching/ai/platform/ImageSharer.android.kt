package com.iching.ai.platform

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

private class AndroidImageSharer(private val context: Context) : ImageSharer {
    override suspend fun share(png: ByteArray, text: String): Boolean {
        // One file, overwritten each time, so sharing never grows the cache. See res/xml/share_paths.xml.
        val file = withContext(Dispatchers.IO) {
            File(context.cacheDir, "share").apply { mkdirs() }.resolve("card.png").apply { writeBytes(png) }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.share", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TEXT, text)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(null, uri)
        context.startActivity(Intent.createChooser(send, null))
        return true
    }
}

@Composable
actual fun rememberImageSharer(): ImageSharer {
    val context = LocalContext.current
    return remember(context) { AndroidImageSharer(context) }
}

actual fun ImageBitmap.encodePng(): ByteArray {
    // The captured layer may be a hardware bitmap, which has to be copied before it can be read.
    val bitmap = asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
    return ByteArrayOutputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        out.toByteArray()
    }
}
