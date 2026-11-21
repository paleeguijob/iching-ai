package com.iching.ai.feature.poster

import coil3.PlatformContext
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun posterImagesModule(): Module = module {
    single<PosterImages> { CoilPosterImages(PlatformContext.INSTANCE) }
}
