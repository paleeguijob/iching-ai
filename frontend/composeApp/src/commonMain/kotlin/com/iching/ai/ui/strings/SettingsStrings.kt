package com.iching.ai.ui.strings

import com.iching.ai.domain.model.AppLanguage

/** The Settings pages, their dialogs and messages, and the feedback sheet. */
interface SettingsStrings {
    val pageMain: String
    val pageAccount: String
    val pageFeedback: String
    val pageAbout: String

    val pushBlockedTitle: String
    val pushBlockedBody: String

    val planPremium: String
    val planFree: String
    fun readingsLeftToday(n: Int): String
    fun coinsNote(coins: Int): String
    fun until(untilMillis: Long): String
    val details: String
    val upgrade: String

    val sound: String
    val dailyPush: String
    val dailyPushSubtitle: String
    fun yearRow(year: Int): String
    val theme: String

    /** The Settings language row and its page title; the same bilingual text in both languages. */
    val language: String
    fun streak(current: Int, best: Int): String
    fun lockedStreak(days: Int): String
    val lockedCampaign: String
    val lockedSpecial: String
    val lockedSuffix: String

    val linkedApple: String
    val linkedGoogle: String
    val notLinked: String
    val localAccount: String
    fun accountId(id: String): String
    val linkHint: String
    val signInApple: String
    val signInGoogle: String
    val syncJournal: String
    val sectionAccount: String
    val sectionPurchases: String
    val restore: String
    val manageSubscriptions: String
    val deleteAccount: String

    val shareApp: String
    val shareAppSubtitle: String
    fun shareAppText(url: String): String
    val sendFeedback: String
    val sendFeedbackSubtitle: String

    val aboutBody: String
    val howToCast: String
    val privacy: String
    val terms: String

    /** Path of the privacy page on the web host, in this language. */
    val privacyPath: String

    /** Path of the terms page on the web host, in this language. */
    val termsPath: String
    fun version(version: String): String

    val deleteTitle: String
    val deleteBody: String
    val deleteConfirm: String

    val feedbackThanks: String
    val feedbackFailed: String
    val feedbackLimit: String
    val linkedCarriedOver: String
    val linkedPartial: String
    val linkFailed: String
    val restoreFailed: String
    val deleteFailed: String
    fun syncAdded(count: Int): String
    val syncUpToDate: String
    val syncPremiumOnly: String
    val syncFailed: String

    val feedbackTitle: String
    val feedbackSubtitle: String
    val feedbackPlaceholder: String
    fun feedbackCounter(length: Int, max: Int): String
    val feedbackSending: String
}

object ThSettingsStrings : SettingsStrings {
    private val lang = AppLanguage.TH

    override val pageMain = "ตั้งค่า"
    override val pageAccount = "บัญชีและการซื้อ"
    override val pageFeedback = "ความเห็นและแชร์แอป"
    override val pageAbout = "เกี่ยวกับแอป"

    override val pushBlockedTitle = "ยังไม่ได้รับอนุญาตให้แจ้งเตือน"
    override val pushBlockedBody = "โปรดเปิดการแจ้งเตือนของแอปนี้ในการตั้งค่าของเครื่อง"

    override val planPremium = "Premium"
    override val planFree = "ผู้ใช้ทั่วไป"
    override fun readingsLeftToday(n: Int) = "วันนี้เหลือ $n คำทำนาย"
    override fun coinsNote(coins: Int) = "เหรียญ $coins (ไม่มีวันหมดอายุ)"
    override fun until(untilMillis: Long) = "ถึง ${formatDate(untilMillis, lang)}"
    override val details = "รายละเอียด"
    override val upgrade = "อัปเกรด"

    override val sound = "เสียงเหรียญและกระดิ่งลม"
    override val dailyPush = "แจ้งเตือนกวาประจำวัน"
    override val dailyPushSubtitle = "ทุกเช้าเวลา ๗ นาฬิกา"
    override fun yearRow(year: Int) = "กวาประจำปี ${formatNumber(year + 543, lang)}"
    override val theme = "ธีม"
    override val language = LANGUAGE_TITLE
    override fun streak(current: Int, best: Int) =
        "ต่อเนื่อง ${formatNumber(current, lang)} วัน · ดีที่สุด ${formatNumber(best, lang)} วัน"
    override fun lockedStreak(days: Int) = "ปลดล็อกเมื่อเปิดดูกวาครบ ${formatNumber(days, lang)} วันต่อเนื่อง"
    override val lockedCampaign = "รับได้จากการเสี่ยงทายกวาประจำปี"
    override val lockedSpecial = "ธีมพิเศษตรุษจีน"
    override val lockedSuffix = " (ยังล็อกอยู่)"

    override val linkedApple = "เชื่อมกับ Apple แล้ว"
    override val linkedGoogle = "เชื่อมกับ Google แล้ว"
    override val notLinked = "ยังไม่เชื่อมบัญชี"
    override val localAccount = "บัญชีบนเครื่องนี้ (ยังไม่เชื่อม)"
    override fun accountId(id: String) = "รหัสบัญชี $id"
    override val linkHint = "เชื่อมบัญชีเพื่อเก็บการซื้อและบันทึกชะตาไว้ เมื่อเปลี่ยนเครื่องหรือติดตั้งแอปใหม่"
    override val signInApple = "ลงชื่อเข้าใช้ด้วย Apple"
    override val signInGoogle = "ลงชื่อเข้าใช้ด้วย Google"
    override val syncJournal = "ดึงบันทึกชะตาจากระบบ"
    override val sectionAccount = "บัญชี"
    override val sectionPurchases = "การซื้อ"
    override val restore = "กู้คืนการซื้อ"
    override val manageSubscriptions = "จัดการการสมัครสมาชิก"
    override val deleteAccount = "ลบบัญชีและข้อมูลทั้งหมด"

    override val shareApp = "แชร์แอปให้เพื่อน"
    override val shareAppSubtitle = "ส่งลิงก์ไปให้คนที่สนใจเสี่ยงทาย"
    override fun shareAppText(url: String) = "ลองเสี่ยงทายกับอี้จิง AI ดูสิ ทั้งอี้จิงและเซียมซี ได้คำทำนายเข้าใจง่ายจากตำราดั้งเดิม\n$url"
    override val sendFeedback = "ส่งความเห็น"
    override val sendFeedbackSubtitle = "บอกเราว่าชอบอะไร อยากให้ปรับตรงไหน"

    override val aboutBody =
        "อี้จิง AI ใช้การโยนเหรียญสามเหรียญหกครั้งตามตำราดั้งเดิม แล้วอ้างอิงตัวบทคัมภีร์อี้จิง ๖๔ กวา ๓๘๔ เส้น " +
            "เป็นแก่นของคำชี้แนะทุกครั้ง คำชี้แนะสร้างโดย AI (Google Gemini) มีไว้เพื่อความบันเทิงและการสะท้อนคิด " +
            "มิใช่คำแนะนำทางการแพทย์ กฎหมาย หรือการเงิน"
    override val howToCast = "วิธีเสี่ยงทาย"
    override val privacy = "นโยบายความเป็นส่วนตัว"
    override val terms = "ข้อกำหนดการใช้งาน"
    override val privacyPath = "/privacy"
    override val termsPath = "/terms"
    override fun version(version: String) = "เวอร์ชัน $version"

    override val deleteTitle = "ลบบัญชีและข้อมูลทั้งหมด?"
    override val deleteBody =
        "บันทึกชะตา บทสนทนา และเหรียญที่เหลือจะถูกลบถาวรทั้งบนเครื่องนี้และบนระบบ ไม่สามารถกู้คืนได้ " +
            "การลบบัญชีไม่ยกเลิกการสมัครสมาชิก โปรดยกเลิกในการตั้งค่าของ App Store / Google Play ด้วย"
    override val deleteConfirm = "ลบถาวร"

    override val feedbackThanks = "ขอบคุณสำหรับความเห็น"
    override val feedbackFailed = "ส่งไม่สำเร็จ โปรดตรวจสอบอินเทอร์เน็ตแล้วลองใหม่"
    override val feedbackLimit = "วันนี้ส่งความเห็นครบแล้ว ลองใหม่พรุ่งนี้นะ"
    override val linkedCarriedOver = "เชื่อมบัญชีแล้ว การซื้อและบันทึกชะตาจะติดตามท่านไปยังเครื่องใหม่"
    override val linkedPartial = "เชื่อมบัญชีแล้ว แต่ยังย้ายการซื้อหรือเหรียญจากเครื่องนี้ไม่ครบ โปรดตรวจสอบอินเทอร์เน็ตแล้วกดกู้คืนการซื้อ"
    override val linkFailed = "เชื่อมบัญชีไม่สำเร็จ โปรดลองใหม่อีกครั้ง"
    override val restoreFailed = "กู้คืนไม่สำเร็จ โปรดลองใหม่อีกครั้ง"
    override val deleteFailed = "ลบบัญชีไม่สำเร็จ โปรดตรวจสอบอินเทอร์เน็ตแล้วลองใหม่"
    override fun syncAdded(count: Int) = "ดึงบันทึกชะตาจากระบบมา $count รายการ"
    override val syncUpToDate = "บันทึกชะตาในเครื่องเป็นปัจจุบันแล้ว"
    override val syncPremiumOnly = "การดึงบันทึกชะตาจากระบบเป็นสิทธิ์ของสมาชิก Premium"
    override val syncFailed = "ดึงบันทึกชะตาไม่สำเร็จ โปรดตรวจสอบอินเทอร์เน็ตแล้วลองใหม่"

    override val feedbackTitle = "ส่งความเห็นถึงเรา"
    override val feedbackSubtitle = "ข้อความถึงทีมผู้พัฒนาโดยตรง ไม่แสดงต่อสาธารณะ"
    override val feedbackPlaceholder = "ชอบตรงไหน อยากให้ปรับอะไร เล่าให้เราฟังได้เลย"
    override fun feedbackCounter(length: Int, max: Int) = "${formatNumber(length, lang)}/${formatNumber(max, lang)}"
    override val feedbackSending = "กำลังส่ง…"
}

object EnSettingsStrings : SettingsStrings {
    private val lang = AppLanguage.EN

    override val pageMain = "Settings"
    override val pageAccount = "Account and purchases"
    override val pageFeedback = "Feedback and sharing"
    override val pageAbout = "About the app"

    override val pushBlockedTitle = "Notifications are not allowed yet"
    override val pushBlockedBody = "Please turn on notifications for this app in your device settings."

    override val planPremium = "Premium"
    override val planFree = "Free plan"
    override fun readingsLeftToday(n: Int) = "$n ${if (n == 1) "reading" else "readings"} left today"
    override fun coinsNote(coins: Int) = "Coins: $coins (never expire)"
    override fun until(untilMillis: Long) = "until ${formatDate(untilMillis, lang)}"
    override val details = "Details"
    override val upgrade = "Upgrade"

    override val sound = "Coin and wind chime sounds"
    override val dailyPush = "Daily hexagram reminder"
    override val dailyPushSubtitle = "Once a day, every morning"
    override fun yearRow(year: Int) = "Year hexagram $year"
    override val theme = "Theme"
    override val language = LANGUAGE_TITLE
    override fun streak(current: Int, best: Int) =
        "Streak: $current ${if (current == 1) "day" else "days"} · Best: $best ${if (best == 1) "day" else "days"}"
    override fun lockedStreak(days: Int) = "Unlocks after opening the daily hexagram $days ${if (days == 1) "day" else "days"} in a row"
    override val lockedCampaign = "Earned by casting the year hexagram"
    override val lockedSpecial = "Special Chinese New Year theme"
    override val lockedSuffix = " (locked)"

    override val linkedApple = "Linked with Apple"
    override val linkedGoogle = "Linked with Google"
    override val notLinked = "Account not linked"
    override val localAccount = "Account on this device (not linked)"
    override fun accountId(id: String) = "Account ID $id"
    override val linkHint = "Link your account to keep your purchases and journal when you change devices or reinstall the app."
    override val signInApple = "Sign in with Apple"
    override val signInGoogle = "Sign in with Google"
    override val syncJournal = "Fetch journal from the server"
    override val sectionAccount = "Account"
    override val sectionPurchases = "Purchases"
    override val restore = "Restore purchases"
    override val manageSubscriptions = "Manage subscription"
    override val deleteAccount = "Delete account and all data"

    override val shareApp = "Share the app with friends"
    override val shareAppSubtitle = "Send a link to someone who might enjoy casting"
    override fun shareAppText(url: String) =
        "Try I Ching AI: I Ching and Siam Si readings that are easy to follow, drawn from the traditional texts.\n$url"
    override val sendFeedback = "Send feedback"
    override val sendFeedbackSubtitle = "Tell us what you like and what to improve"

    override val aboutBody =
        "I Ching AI tosses three coins six times, as in the traditional method, and builds every reading on the text of the " +
            "I Ching's 64 hexagrams and 384 lines. The guidance is generated by AI (Google Gemini) and is for entertainment " +
            "and reflection. It is not medical, legal or financial advice."
    override val howToCast = "How to cast"
    override val privacy = "Privacy policy"
    override val terms = "Terms of use"
    override val privacyPath = "/en/privacy"
    override val termsPath = "/en/terms"
    override fun version(version: String) = "Version $version"

    override val deleteTitle = "Delete account and all data?"
    override val deleteBody =
        "Your journal, conversations and remaining coins will be permanently deleted from this device and from our system. " +
            "This cannot be undone. Deleting your account does not cancel your subscription. " +
            "Please also cancel it in the App Store / Google Play settings."
    override val deleteConfirm = "Delete permanently"

    override val feedbackThanks = "Thank you for your feedback"
    override val feedbackFailed = "Could not send. Please check your connection and try again."
    override val feedbackLimit = "You have sent all the feedback for today. Please try again tomorrow."
    override val linkedCarriedOver = "Account linked. Your purchases and journal will follow you to a new device."
    override val linkedPartial = "Account linked, but some purchases or coins from this device were not moved. Please check your connection and tap Restore purchases."
    override val linkFailed = "Could not link the account. Please try again."
    override val restoreFailed = "Could not restore. Please try again."
    override val deleteFailed = "Could not delete the account. Please check your connection and try again."
    override fun syncAdded(count: Int) = "Fetched $count ${if (count == 1) "journal entry" else "journal entries"} from the server"
    override val syncUpToDate = "The journal on this device is up to date."
    override val syncPremiumOnly = "Fetching the journal from the server is for Premium members."
    override val syncFailed = "Could not fetch the journal. Please check your connection and try again."

    override val feedbackTitle = "Send us feedback"
    override val feedbackSubtitle = "A message straight to the developers. It is not shown to anyone else."
    override val feedbackPlaceholder = "Tell us what you like and what you would change"
    override fun feedbackCounter(length: Int, max: Int) = "$length/$max"
    override val feedbackSending = "Sending…"
}
