package com.iching.ai.platform

import java.security.SecureRandom
import kotlin.random.Random
import kotlin.random.asKotlinRandom

actual fun secureRandom(): Random = SecureRandom().asKotlinRandom()
