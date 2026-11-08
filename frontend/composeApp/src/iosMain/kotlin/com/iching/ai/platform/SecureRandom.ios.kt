package com.iching.ai.platform

import platform.posix.arc4random
import kotlin.random.Random

private object Arc4Random : Random() {
    override fun nextBits(bitCount: Int): Int =
        if (bitCount == 0) 0 else arc4random().toInt() ushr (32 - bitCount)
}

actual fun secureRandom(): Random = Arc4Random
