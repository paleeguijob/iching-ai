package com.iching.ai.feature.divination

import com.iching.ai.platform.SpeechState
import kotlin.test.Test
import kotlin.test.assertEquals

class ListenActionTest {
    @Test
    fun freeGoesToPaywall() = assertEquals(ListenAction.OPEN_PAYWALL, listenAction(premium = false, SpeechState.IDLE))

    @Test
    fun premiumSpeaks() = assertEquals(ListenAction.SPEAK, listenAction(premium = true, SpeechState.IDLE))

    @Test
    fun tappingWhileSpeakingStops() = assertEquals(ListenAction.STOP, listenAction(premium = true, SpeechState.SPEAKING))

    @Test
    fun tappingWhilePreparingCancels() = assertEquals(ListenAction.STOP, listenAction(premium = true, SpeechState.PREPARING))

    @Test
    fun aLapsedSubscriberCanStillStopTheVoice() = assertEquals(ListenAction.STOP, listenAction(premium = false, SpeechState.SPEAKING))

    @Test
    fun labelSaysWhatIsHappening() {
        assertEquals("ฟังคำทำนาย", listenLabel(premium = true, SpeechState.IDLE))
        assertEquals("กำลังเตรียมเสียง…", listenLabel(premium = true, SpeechState.PREPARING))
        assertEquals("หยุดอ่าน", listenLabel(premium = true, SpeechState.SPEAKING))
        assertEquals("ฟังคำทำนาย · Premium", listenLabel(premium = false, SpeechState.IDLE))
    }
}
