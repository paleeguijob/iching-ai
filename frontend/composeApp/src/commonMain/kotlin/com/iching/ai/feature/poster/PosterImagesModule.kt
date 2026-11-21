package com.iching.ai.feature.poster

import org.koin.core.module.Module

/** Provides [PosterImages] with each platform's Coil context (the Android app context; nothing on iOS). */
internal expect fun posterImagesModule(): Module
