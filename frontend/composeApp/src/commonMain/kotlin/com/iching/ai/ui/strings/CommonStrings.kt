package com.iching.ai.ui.strings

/** Generic button and label text shared by many screens. */
interface CommonStrings {
    val cancel: String
    val close: String
    val gotIt: String
}

object ThCommonStrings : CommonStrings {
    override val cancel = "ยกเลิก"
    override val close = "ปิด"
    override val gotIt = "เข้าใจแล้ว"
}

object EnCommonStrings : CommonStrings {
    override val cancel = "Cancel"
    override val close = "Close"
    override val gotIt = "Got it"
}
