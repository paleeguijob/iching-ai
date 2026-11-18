package com.iching.ai.feature.home

import com.iching.ai.domain.model.Hexagram
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CampaignLineTest {
    private val kun = Hexagram(
        id = 47, nameCn = "困", pinyin = "kùn", nameTh = "ขุน — ความตีบตัน, ถูกล้อม", pattern = "010110",
        keywords = emptyList(), judgment = "", image = "", description = "", lines = emptyList(),
    )

    @Test
    fun hiddenHasNoLine() = assertNull(campaignLine(CampaignCard.Hidden))

    @Test
    fun newAsksToCast() = assertEquals(
        CampaignLine("กวาประจำปีมะแม ๒๕๗๐ · ยังไม่ได้เสี่ยง", action = "เสี่ยง", pending = true),
        campaignLine(CampaignCard.New(2027)),
    )

    @Test
    fun castNamesTheYearHexagram() = assertEquals(
        CampaignLine("กวาประจำปีของท่าน: ขุน", action = "ดู", pending = false),
        campaignLine(CampaignCard.Cast(2027, kun)),
    )
}
