package com.iching.ai.ui.strings

import com.iching.ai.domain.model.AppLanguage

/** Home, its quota panel, the pre-cast sheet, the streak line and the year campaign line; also the tab names. */
interface HomeStrings {
    val tabAsk: String
    val tabLibrary: String
    val tabJournal: String

    val appName: String
    val modeIching: String
    val modeSiamsi: String
    val settings: String
    val askPlaceholderSiamsi: String
    val askPlaceholder: String
    val yearPlaceholder: String
    val askYearTitle: String

    val dailyHexagram: String
    val dailyHexagramPending: String
    val ritualPrompt: String
    val ritualCaption: String
    fun setButton(label: String): String

    val siamsiWatchAd: String
    val siamsiShake: String
    val siamsiPaywall: String
    val artHintWatchAd: String
    val artHintPaywall: String

    val startButton: String
    val startButtonDeciding: String

    val premiumPrefix: String
    val quotaPaused: String
    val quotaToday: String
    val quotaCoins: String
    val quotaAds: String
    val coinsNote: String
    val quotaAdsNote: String
    val premiumOutNote: String
    val freeOutNote: String
    val quotaUnit: String
    fun quotaPlusCoins(coins: Int): String
    fun quotaSpoken(label: String, count: Int): String

    val preCastBlankQuestion: String
    val preCastAdTitle: String
    val preCastAdBody: String
    val preCastWatchAd: String
    val preCastWithoutWords: String
    val preCastReady: String
    val siamsiPreCastWatchAd: String
    val siamsiPreCastWithoutWords: String
    val siamsiPreCastReady: String
    fun adsLeftSuffix(n: Int): String
    val preCastUpsellTitle: String
    val preCastUpsellBody: String
    val preCastReadyTitle: String
    val productsFailed: String
    val adUnavailableOrScripture: String

    val streakStart: String
    fun streakDayLabel(n: Int): String
    fun streakRun(days: Int): String
    fun streakKeep(run: String): String
    fun streakBest(run: String, best: Int): String
    fun streakNextTheme(run: String, daysLeft: Int, themeLabel: String): String

    fun campaignNew(year: Int): String
    fun campaignCast(hexagramName: String): String
    val campaignActionCast: String
    val campaignActionView: String
}

object ThHomeStrings : HomeStrings {
    private val lang = AppLanguage.TH

    override val tabAsk = "ถามเต๋า"
    override val tabLibrary = "คัมภีร์"
    override val tabJournal = "บันทึกชะตา"

    override val appName = "อี้จิง"
    override val modeIching = "อี้จิง"
    override val modeSiamsi = "เซียมซี"
    override val settings = "ตั้งค่า"
    override val askPlaceholderSiamsi = "เรื่องที่อยากถาม (ไม่บังคับ)…"
    override val askPlaceholder = "พิมพ์คำถาม หรือเว้นว่างไว้อธิษฐานในใจ"
    override val yearPlaceholder = "พิมพ์สิ่งที่ค้างคาใจ หรือเว้นว่างไว้เพื่ออธิษฐานในใจ..."
    override val askYearTitle = "ถามนักพรตเรื่องปีนี้"

    override val dailyHexagram = "กวาประจำวัน"
    override val dailyHexagramPending = "กวาประจำวัน · ยังไม่ได้เปิดดู"
    override val ritualPrompt = "มีเรื่องใดค้างใจ"
    override val ritualCaption = "ตั้งจิตถึงเรื่องนั้น แล้วแตะเหรียญเพื่อโยน ๖ ครั้ง"
    override fun setButton(label: String) = "ชุด: $label"

    override val siamsiWatchAd = "▶ ดูโฆษณาแล้วเขย่าเซียมซี"
    override val siamsiShake = "เขย่าเซียมซี"
    override val siamsiPaywall = "สมัคร Premium เสี่ยงได้ ๓ ครั้ง/วัน ไม่มีโฆษณา"
    override val artHintWatchAd = "กดปุ่มด้านล่างเพื่อดูโฆษณาแล้วเขย่า"
    override val artHintPaywall = "วันนี้เสี่ยงครบแล้ว กลับมาใหม่พรุ่งนี้"

    override val startButton = "เริ่มเสี่ยงทาย"
    override val startButtonDeciding = "กำลังตั้งจิต..."

    override val premiumPrefix = "Premium · "
    override val quotaPaused = "นักพรตปลีกวิเวกชั่วคราว — ยังเสี่ยงทายและอ่านตัวบทคัมภีร์ได้"
    override val quotaToday = "วันนี้เหลือคำชี้แนะจากนักพรต"
    override val quotaCoins = "เหรียญอี้จิงใช้ทำนายได้อีก"
    override val quotaAds = "ชมโฆษณา รับคำชี้แนะเพิ่มได้อีก"
    override val coinsNote = "ใช้ได้ทุกวัน ไม่มีวันหมดอายุ"
    override val quotaAdsNote = "กดเริ่มเสี่ยงทาย แล้วชมโฆษณาสั้นก่อนโยนเหรียญ"
    override val premiumOutNote = "ยังเสี่ยงทายและอ่านตัวบทได้"
    override val freeOutNote = "สมัคร Premium หรือใช้เหรียญอี้จิงเพื่อรับคำชี้แนะต่อ · ยังเสี่ยงทายและอ่านตัวบทได้"
    override val quotaUnit = "ครั้ง"
    override fun quotaPlusCoins(coins: Int) = "และมีเหรียญอี้จิงอีก ${formatNumber(coins, lang)} เหรียญ ใช้ได้ทุกวัน ไม่มีวันหมดอายุ"
    override fun quotaSpoken(label: String, count: Int) = "$label ${formatNumber(count, lang)} ครั้ง"

    override val preCastBlankQuestion = "อธิษฐานในใจ"
    override val preCastAdTitle = "รับคำชี้แนะจากนักพรต"
    override val preCastAdBody = "ชมโฆษณาสั้น ๆ เพื่อรับคำชี้แนะจากนักพรตสำหรับการเสี่ยงทายครั้งนี้"
    override val preCastWatchAd = "ชมโฆษณาสั้น แล้วเริ่มเสี่ยงทาย"
    override val preCastWithoutWords = "เสี่ยงทาย อ่านตัวบทคัมภีร์อย่างเดียว"
    override val preCastReady = "เริ่มเสี่ยงทาย"
    override val siamsiPreCastWatchAd = "ชมโฆษณาสั้น แล้วแปลความหมาย"
    override val siamsiPreCastWithoutWords = "อ่านใบเซียมซีอย่างเดียว"
    override val siamsiPreCastReady = "แปลความหมาย"
    override fun adsLeftSuffix(n: Int) = "(เหลือ ${formatNumber(n, lang)})"
    override val preCastUpsellTitle = "คำชี้แนะจากโฆษณาของวันนี้ครบแล้ว"
    override val preCastUpsellBody = "ท่านยังรับคำชี้แนะจากนักพรตสำหรับการเสี่ยงทายครั้งนี้ได้"
    override val preCastReadyTitle = "พร้อมรับคำชี้แนะแล้ว"
    override val productsFailed = "ยังโหลดแพ็กเกจจากร้านค้าไม่ได้ โปรดตรวจสอบอินเทอร์เน็ตแล้วลองใหม่"
    override val adUnavailableOrScripture = "ยังไม่มีโฆษณาพร้อมแสดง โปรดลองใหม่อีกครู่ หรือเสี่ยงทายอ่านตัวบทไปก่อน"

    override val streakStart = "เปิดอ่านกวาวันนี้ เพื่อเริ่มนับวันแรก"
    override fun streakDayLabel(n: Int) = formatNumber(n, lang)
    override fun streakRun(days: Int) = "ต่อเนื่อง ${formatNumber(days, lang)} วัน"
    override fun streakKeep(run: String) = "$run · เปิดอ่านกวาวันนี้เพื่อไม่ให้ขาด"
    override fun streakBest(run: String, best: Int) = "$run · ดีที่สุด ${formatNumber(best, lang)} วัน"
    override fun streakNextTheme(run: String, daysLeft: Int, themeLabel: String) =
        "$run · อีก ${formatNumber(daysLeft, lang)} วันปลดล็อกธีม$themeLabel"

    override fun campaignNew(year: Int) = "กวาประจำปี${zodiacYearLabel(year, lang)} · ยังไม่ได้เสี่ยง"
    override fun campaignCast(hexagramName: String) = "กวาประจำปีของท่าน: $hexagramName"
    override val campaignActionCast = "เสี่ยง"
    override val campaignActionView = "ดู"
}

object EnHomeStrings : HomeStrings {
    private val lang = AppLanguage.EN

    override val tabAsk = "Ask"
    override val tabLibrary = "Library"
    override val tabJournal = "Journal"

    override val appName = "I Ching"
    override val modeIching = "I Ching"
    override val modeSiamsi = "Siam Si"
    override val settings = "Settings"
    override val askPlaceholderSiamsi = "Your question (optional)…"
    override val askPlaceholder = "Type a question, or leave it blank and ask silently"
    override val yearPlaceholder = "Type what is on your mind, or leave it blank and ask silently..."
    override val askYearTitle = "Ask the hermit about this year"

    override val dailyHexagram = "Daily hexagram"
    override val dailyHexagramPending = "Daily hexagram · not opened yet"
    override val ritualPrompt = "What is on your mind?"
    override val ritualCaption = "Hold the question in mind, then tap the coins to toss six times"
    override fun setButton(label: String) = "Set: $label"

    override val siamsiWatchAd = "▶ Watch an ad, then shake"
    override val siamsiShake = "Shake the sticks"
    override val siamsiPaywall = "Go Premium: 3 draws a day, no ads"
    override val artHintWatchAd = "Use the button below to watch an ad and shake"
    override val artHintPaywall = "You have drawn enough for today. Come back tomorrow."

    override val startButton = "Begin casting"
    override val startButtonDeciding = "Settling your mind..."

    override val premiumPrefix = "Premium · "
    override val quotaPaused = "The hermit is away for now — you can still cast and read the scripture"
    override val quotaToday = "Hermit's guidance today"
    override val quotaCoins = "I Ching coins for readings"
    override val quotaAds = "Watch ads for extra guidance"
    override val coinsNote = "Usable any day, never expire"
    override val quotaAdsNote = "Tap Begin casting, then watch a short ad before the toss"
    override val premiumOutNote = "You can still cast and read the scripture"
    override val freeOutNote = "Go Premium or use I Ching coins for more guidance · you can still cast and read the scripture"
    override val quotaUnit = "left"
    override fun quotaPlusCoins(coins: Int) =
        "Plus $coins I Ching ${if (coins == 1) "coin" else "coins"}, usable any day, never expire"
    override fun quotaSpoken(label: String, count: Int) = "$label: $count left"

    override val preCastBlankQuestion = "Silent prayer"
    override val preCastAdTitle = "Get the hermit's guidance"
    override val preCastAdBody = "Watch a short ad to receive the hermit's guidance for this cast."
    override val preCastWatchAd = "Watch a short ad, then cast"
    override val preCastWithoutWords = "Cast and read the scripture only"
    override val preCastReady = "Begin casting"
    override val siamsiPreCastWatchAd = "Watch a short ad, then interpret"
    override val siamsiPreCastWithoutWords = "Read the slip only"
    override val siamsiPreCastReady = "Interpret"
    override fun adsLeftSuffix(n: Int) = "($n left)"
    override val preCastUpsellTitle = "Today's ad-supported guidance is used up"
    override val preCastUpsellBody = "You can still receive the hermit's guidance for this cast."
    override val preCastReadyTitle = "Guidance is ready"
    override val productsFailed = "Plans could not be loaded from the store. Check your connection and try again."
    override val adUnavailableOrScripture = "No ad is ready right now. Try again shortly, or cast and read the scripture first."

    override val streakStart = "Open today's hexagram to start your streak"
    override fun streakDayLabel(n: Int) = formatNumber(n, lang)
    override fun streakRun(days: Int) = "$days-day streak"
    override fun streakKeep(run: String) = "$run · open today's hexagram to keep it going"
    override fun streakBest(run: String, best: Int) = "$run · best $best days"
    override fun streakNextTheme(run: String, daysLeft: Int, themeLabel: String) =
        "$run · $daysLeft more ${if (daysLeft == 1) "day" else "days"} to unlock the $themeLabel theme"

    override fun campaignNew(year: Int) = "Hexagram for the ${zodiacYearLabel(year, lang)} · not cast yet"
    override fun campaignCast(hexagramName: String) = "Your year hexagram: $hexagramName"
    override val campaignActionCast = "Cast"
    override val campaignActionView = "View"
}
