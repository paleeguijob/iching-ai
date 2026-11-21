package com.iching.ai.feature.siamsi

import com.iching.ai.domain.siamsi.PuaProgress
import com.iching.ai.domain.siamsi.PuaResult
import com.iching.ai.domain.siamsi.PuaRound
import com.iching.ai.domain.siamsi.SiamSiLevel
import com.iching.ai.domain.siamsi.SiamSiStick
import com.iching.ai.domain.siamsi.SiamSiTopic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SiamSiLeaveWarningTest {
    private val stick = SiamSiStick(
        number = 7, gradeOrig = "中吉", level = SiamSiLevel.FAIR, labelOrig = null,
        poemOrig = List(4) { "句$it" }, poemTh = List(4) { "วรรค$it" }, story = "", summary = "sum",
        topics = SiamSiTopic.entries.associateWith { it.labelTh }, keywords = emptyList(),
    )
    private val failedToss = PuaProgress(listOf(PuaRound(7, PuaResult.LAUGH)))
    private val slip = SiamSiStage.Slip(stick, autoConfirmed = false)

    @Test
    fun nothingDrawnYetLeavesWithoutAsking() {
        assertNull(siamsiLeaveWarning(SiamSiStage.Loading, InterpretUi()))
        assertNull(siamsiLeaveWarning(SiamSiStage.Shaking, InterpretUi()))
        assertNull(siamsiLeaveWarning(SiamSiStage.Unpaid, InterpretUi()))
        assertNull(siamsiLeaveWarning(SiamSiStage.Safety("โทร 1323"), InterpretUi()))
    }

    @Test
    fun aStickInHandAsksBeforeLeaving() {
        assertEquals(SiamSiLeaveWarning.DRAW, siamsiLeaveWarning(SiamSiStage.Fallen(7), InterpretUi()))
        assertEquals(SiamSiLeaveWarning.DRAW, siamsiLeaveWarning(SiamSiStage.Tossing(7, emptyList()), InterpretUi()))
        assertEquals(SiamSiLeaveWarning.DRAW, siamsiLeaveWarning(SiamSiStage.Pua(failedToss), InterpretUi()))
    }

    @Test
    fun aSavedSlipLeavesWithoutAsking() {
        val confirmed = PuaProgress(listOf(PuaRound(7, PuaResult.SAK)))
        assertNull(siamsiLeaveWarning(SiamSiStage.Confirmed(stick, false, confirmed), InterpretUi()))
        assertNull(siamsiLeaveWarning(slip, InterpretUi()))
        assertNull(siamsiLeaveWarning(slip, InterpretUi(text = "ใบนี้ดี")))
    }

    @Test
    fun anInterpretationUnderWayAsksBeforeLeaving() {
        assertEquals(SiamSiLeaveWarning.INTERPRETING, siamsiLeaveWarning(slip, InterpretUi(streaming = true)))
        assertEquals(
            SiamSiLeaveWarning.INTERPRETING_RESUMABLE,
            siamsiLeaveWarning(slip, InterpretUi(text = "ใบ", streaming = true, onServer = true)),
        )
    }
}
