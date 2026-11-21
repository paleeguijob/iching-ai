package com.iching.ai.feature.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalMaterial3Api::class)
class FeedbackSheetTest {
    @Test
    fun aSendInFlightCannotBeSwipedAway() {
        // Hiding mid-send would leave the sheet invisible yet open, and the send's error would land where nobody sees it.
        assertFalse(feedbackSheetMayMove(sending = true, target = SheetValue.Hidden))
        assertTrue(feedbackSheetMayMove(sending = true, target = SheetValue.Expanded))
        assertTrue(feedbackSheetMayMove(sending = false, target = SheetValue.Hidden))
    }
}
