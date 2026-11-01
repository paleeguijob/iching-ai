package com.iching.ai.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.popoverPresentationController

private class IosImageSharer : ImageSharer {
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun share(png: ByteArray, text: String): Boolean {
        val data = png.usePinned { NSData.create(bytes = it.addressOf(0), length = png.size.toULong()) }
        val image = UIImage.imageWithData(data) ?: return false
        var top = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return false
        while (true) top = top.presentedViewController ?: break
        val sheet = UIActivityViewController(listOf(image, text), null)
        sheet.popoverPresentationController?.sourceView = top.view
        top.presentViewController(sheet, animated = true, completion = null)
        return true
    }
}

@Composable
actual fun rememberImageSharer(): ImageSharer = remember { IosImageSharer() }

actual fun ImageBitmap.encodePng(): ByteArray =
    Image.makeFromBitmap(asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)?.bytes
        ?: error("PNG encoding failed")
