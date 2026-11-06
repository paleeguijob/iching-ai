package com.iching.ai.feature.settings

import com.iching.ai.domain.repository.ApiException
import com.iching.ai.domain.repository.FailureReason
import kotlin.test.Test
import kotlin.test.assertEquals

class JournalSyncMessageTest {
    @Test
    fun saysHowManyReadingsCameBack() {
        assertEquals("ดึงบันทึกชะตาจากระบบมา 3 รายการ", journalSyncMessage(Result.success(3)))
    }

    @Test
    fun nothingNewReadsAsUpToDate() {
        assertEquals("บันทึกชะตาในเครื่องเป็นปัจจุบันแล้ว", journalSyncMessage(Result.success(0)))
    }

    @Test
    fun aFailedFetchDoesNotReadAsUpToDate() {
        val failed = Result.failure<Int>(ApiException(FailureReason.NETWORK, "offline"))
        assertEquals("ดึงบันทึกชะตาไม่สำเร็จ โปรดตรวจสอบอินเทอร์เน็ตแล้วลองใหม่", journalSyncMessage(failed))
    }

    @Test
    fun aFetchRefusedForLapsedPremiumSaysSo() {
        val refused = Result.failure<Int>(ApiException(FailureReason.PREMIUM_REQUIRED, "HTTP 403"))
        assertEquals("การดึงบันทึกชะตาจากระบบเป็นสิทธิ์ของสมาชิก Premium", journalSyncMessage(refused))
    }
}
