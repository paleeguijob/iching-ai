package com.iching.ai.feature.poster

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun posterImagesModule(): Module = module {
    single<PosterImages> { CoilPosterImages(androidContext()) }
}
