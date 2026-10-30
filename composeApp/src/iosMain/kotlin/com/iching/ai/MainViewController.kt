package com.iching.ai

import androidx.compose.ui.window.ComposeUIViewController
import com.iching.ai.di.initKoin

fun MainViewController() = ComposeUIViewController { App() }

/** Called once from Swift at launch. */
fun doInitKoin() = initKoin()
