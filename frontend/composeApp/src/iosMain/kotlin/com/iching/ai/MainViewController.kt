package com.iching.ai

import androidx.compose.ui.window.ComposeUIViewController
import com.iching.ai.di.initKoin
import com.iching.ai.platform.NativeBridges

fun MainViewController() = ComposeUIViewController { App() }

/** Called once from Swift at launch with the Swift implementations of the native bridges. */
fun doInitKoin(bridges: NativeBridges) = initKoin(bridges)
