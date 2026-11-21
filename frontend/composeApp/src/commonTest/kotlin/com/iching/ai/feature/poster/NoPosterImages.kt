package com.iching.ai.feature.poster

/** For view-model tests that are not about posters: no image ever loads, so no popup ever shows. */
object NoPosterImages : PosterImages {
    override suspend fun preload(url: String) = false
}
