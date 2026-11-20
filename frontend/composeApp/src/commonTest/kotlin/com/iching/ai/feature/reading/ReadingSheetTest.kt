package com.iching.ai.feature.reading

import com.iching.ai.ui.strings.TH_STRINGS
import com.iching.ai.domain.model.InterpretationSource
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingSheetTest {

    @Test
    fun scriptureSheetHasNoAiBadge() =
        assertEquals(SheetHeader("ตัวบทจากคัมภีร์", showAiBadge = false), sheetHeader(InterpretationSource.SCRIPTURE, TH_STRINGS.reading))

    @Test
    fun aiSheetShowsBadge() =
        (InterpretationSource.entries.filter { it != InterpretationSource.SCRIPTURE } + null).forEach {
            assertEquals(SheetHeader("คำชี้แนะจากนักพรต", showAiBadge = true), sheetHeader(it, TH_STRINGS.reading), "source $it")
        }
}
