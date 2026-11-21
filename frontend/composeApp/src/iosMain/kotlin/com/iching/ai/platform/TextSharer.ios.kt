package com.iching.ai.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.popoverPresentationController

@Composable
actual fun rememberTextSharer(): TextSharer = remember {
    TextSharer { text ->
        var top = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return@TextSharer
        while (true) top = top.presentedViewController ?: break
        val sheet = UIActivityViewController(listOf(text), null)
        sheet.popoverPresentationController?.sourceView = top.view
        top.presentViewController(sheet, animated = true, completion = null)
    }
}
