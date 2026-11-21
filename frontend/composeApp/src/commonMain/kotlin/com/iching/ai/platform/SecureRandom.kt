package com.iching.ai.platform

import kotlin.random.Random

/** A cryptographically strong [Random] for Siam Si draws, so no stick can be predicted or favoured. */
expect fun secureRandom(): Random
