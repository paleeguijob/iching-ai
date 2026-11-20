package com.iching.ai.ui.strings

import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.domain.model.ProductKind
import com.iching.ai.domain.model.StoreProduct

/** The paywall, its purchase and restore messages (also shown by the pre-cast sheets), and the store product wording. */
interface PaywallStrings {
    val pending: String
    val awaitingPayment: String
    val coinsReceived: String
    val welcomePremium: String
    val restoredPremium: String
    fun restoredCoins(coins: Int): String
    val restoreNothing: String
    val restoreFailed: String
    val productsFailed: String

    val title: String
    val tagline: String
    val benefits: List<String>
    fun memberLine(untilMillis: Long?): String
    val reload: String
    val coinsHeading: String
    fun coinsNote(coins: Int): String
    val restore: String
    val manage: String
    val terms: String
    val privacy: String
    val planYearly: String
    val planMonthly: String
    val bestValue: String

    /** The free-trial wording for [product], or null when it has none (English needs the day count the store reported). */
    fun trialLabel(product: StoreProduct): String?
    fun startTrial(trial: String): String
    fun subscribe(product: StoreProduct): String
    fun pricePerPeriod(product: StoreProduct): String

    /** Auto-renewal disclosure required by both stores, next to the purchase button. */
    fun renewalTerms(product: StoreProduct): String
}

object ThPaywallStrings : PaywallStrings {
    private val lang = AppLanguage.TH

    override val pending = "ชำระเงินแล้ว แต่ระบบยังไม่อัปเดตสิทธิ์ โปรดอย่าซื้อซ้ำ กดกู้คืนการซื้ออีกครั้งในอีกสักครู่"
    override val awaitingPayment = "รอการชำระเงินให้เสร็จสิ้น สิทธิ์จะเข้าเองเมื่อชำระแล้ว โปรดอย่าซื้อซ้ำ"
    override val coinsReceived = "ได้รับเหรียญแล้ว เก็บไว้ใช้วันไหนก็ได้ ไม่มีวันหมดอายุ"
    override val welcomePremium = "ยินดีต้อนรับสู่ Premium"
    override val restoredPremium = "กู้คืนสมาชิก Premium แล้ว"
    override fun restoredCoins(coins: Int) = "ได้รับ $coins เหรียญแล้ว"
    override val restoreNothing = "ไม่พบการสมัครสมาชิกที่ยังใช้งานอยู่"
    override val restoreFailed = "กู้คืนไม่สำเร็จ โปรดลองใหม่อีกครั้ง"
    override val productsFailed = "ยังโหลดแพ็กเกจจากร้านค้าไม่ได้ โปรดตรวจสอบอินเทอร์เน็ตแล้วลองใหม่"

    override val title = "อี้จิง Premium"
    override val tagline = "ให้นักพรตร่วมพินิจทุกการตัดสินใจของท่าน"
    override val benefits = listOf(
        "คำทำนายจากนักพรต วันละ ๕ ครั้ง ด้วยปัญญาที่ลึกกว่า",
        "ถามต่อกับนักพรตได้ ๓ คำถามในแต่ละคำทำนาย",
        "บันทึกชะตาไม่จำกัด และกู้คืนข้ามเครื่องได้",
        "ไม่มีโฆษณา",
    )
    override fun memberLine(untilMillis: Long?) =
        "ท่านเป็นสมาชิก Premium" + (untilMillis?.let { " ถึง ${formatDate(it, lang)}" } ?: "")
    override val reload = "ลองโหลดใหม่"
    override val coinsHeading = "ไม่ชอบสมัครสมาชิก?"
    override fun coinsNote(coins: Int) = "๑ เหรียญ = ๑ คำทำนายจากนักพรต ไม่มีวันหมดอายุ · ใช้เมื่อสิทธิ์ของวันหมด · มีอยู่ $coins เหรียญ"
    override val restore = "กู้คืนการซื้อ"
    override val manage = "จัดการการสมัคร"
    override val terms = "ข้อกำหนดการใช้งาน"
    override val privacy = "นโยบายความเป็นส่วนตัว"
    override val planYearly = "รายปี"
    override val planMonthly = "รายเดือน"
    override val bestValue = "คุ้มที่สุด"

    override fun trialLabel(product: StoreProduct) = product.trialTh
    override fun startTrial(trial: String) = "เริ่ม$trial"
    override fun subscribe(product: StoreProduct) = "สมัคร ${product.price}/${product.periodTh ?: ""}"
    override fun pricePerPeriod(product: StoreProduct) = "${product.price}/${product.periodTh ?: ""}"
    override fun renewalTerms(product: StoreProduct): String {
        val period = product.periodTh ?: "รอบ"
        val trial = product.trialTh?.let { "$it จากนั้น " } ?: ""
        return "${trial}เรียกเก็บ ${product.price} ทุก$period ต่ออายุอัตโนมัติจนกว่าจะยกเลิก " +
            "ยกเลิกได้ทุกเมื่อในการตั้งค่าบัญชีของร้านค้า อย่างน้อย ๒๔ ชั่วโมงก่อนรอบบิลถัดไป"
    }
}

object EnPaywallStrings : PaywallStrings {
    private val lang = AppLanguage.EN

    private fun period(product: StoreProduct): String? = when (product.kind) {
        ProductKind.MONTHLY -> "month"
        ProductKind.YEARLY -> "year"
        ProductKind.COINS -> null
    }

    override val pending = "Payment received, but your access has not updated yet. Please do not buy again. Tap Restore purchases again in a little while."
    override val awaitingPayment = "Waiting for the payment to finish. Your access will arrive on its own once it is paid. Please do not buy again."
    override val coinsReceived = "Coins received. Keep them for any day. They never expire."
    override val welcomePremium = "Welcome to Premium"
    override val restoredPremium = "Your Premium membership is restored."
    override fun restoredCoins(coins: Int) = "$coins coins received."
    override val restoreNothing = "No active subscription was found."
    override val restoreFailed = "Could not restore. Please try again."
    override val productsFailed = "Could not load the plans from the store. Please check your connection and try again."

    override val title = "I Ching Premium"
    override val tagline = "Let the hermit reflect with you on every decision."
    override val benefits = listOf(
        "5 readings a day from the hermit, with deeper insight",
        "Ask the hermit 3 follow-up questions on each reading",
        "Unlimited journal, restorable across devices",
        "No ads",
    )
    override fun memberLine(untilMillis: Long?) =
        "You are a Premium member" + (untilMillis?.let { " until ${formatDate(it, lang)}" } ?: "")
    override val reload = "Try again"
    override val coinsHeading = "Prefer not to subscribe?"
    override fun coinsNote(coins: Int) =
        "1 coin = 1 hermit reading. Coins never expire. Used when the day's readings run out. You have $coins."
    override val restore = "Restore purchases"
    override val manage = "Manage subscription"
    override val terms = "Terms of use"
    override val privacy = "Privacy policy"
    override val planYearly = "Yearly"
    override val planMonthly = "Monthly"
    override val bestValue = "Best value"

    override fun trialLabel(product: StoreProduct) = product.trialDays?.let { "$it-day free trial" }
    override fun startTrial(trial: String) = "Start $trial"
    override fun subscribe(product: StoreProduct) = "Subscribe ${product.price}/${period(product) ?: ""}"
    override fun pricePerPeriod(product: StoreProduct) = "${product.price}/${period(product) ?: ""}"
    override fun renewalTerms(product: StoreProduct): String {
        val trial = trialLabel(product)?.let { "$it, then " } ?: ""
        val per = period(product)?.let { "per $it" } ?: "per billing period"
        return "$trial${product.price} $per. Renews automatically until you cancel. " +
            "You can cancel any time in your store account settings, at least 24 hours before the next billing date."
    }
}
