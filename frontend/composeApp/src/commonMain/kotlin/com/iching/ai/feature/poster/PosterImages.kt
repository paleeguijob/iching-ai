package com.iching.ai.feature.poster

import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult

/** Fetches a poster's artwork ahead of showing it; true once it is in the image cache. */
interface PosterImages {
    suspend fun preload(url: String): Boolean
}

/** Uses the app's shared Coil loader, so the popup's AsyncImage draws straight from the same cache. */
class CoilPosterImages(private val context: PlatformContext) : PosterImages {
    override suspend fun preload(url: String): Boolean =
        SingletonImageLoader.get(context).execute(ImageRequest.Builder(context).data(url).build()) is SuccessResult
}
