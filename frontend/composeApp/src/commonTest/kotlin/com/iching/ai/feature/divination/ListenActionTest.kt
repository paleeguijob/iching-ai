package com.iching.ai.feature.divination

import kotlin.test.Test
import kotlin.test.assertEquals

class ListenActionTest {
    @Test
    fun freeGoesToPaywall() = assertEquals(ListenAction.OPEN_PAYWALL, listenAction(premium = false, speaking = false))

    @Test
    fun premiumSpeaks() = assertEquals(ListenAction.SPEAK, listenAction(premium = true, speaking = false))

    @Test
    fun tappingWhileSpeakingStops() = assertEquals(ListenAction.STOP, listenAction(premium = true, speaking = true))

    @Test
    fun aLapsedSubscriberCanStillStopTheVoice() = assertEquals(ListenAction.STOP, listenAction(premium = false, speaking = true))
}
