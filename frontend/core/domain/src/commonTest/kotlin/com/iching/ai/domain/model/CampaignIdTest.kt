package com.iching.ai.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CampaignIdTest {
    @Test
    fun usesTheServersCampaignId() {
        assertEquals("cny-2027", campaignIdFor(CampaignStatus("cny-2027", 2027, active = true, endsAtMillis = 0), 2030))
    }

    @Test
    fun fallsBackToTheYearWhenTheServerReportsNoCampaign() {
        assertEquals("cny-2027", campaignIdFor(null, 2027))
    }
}
