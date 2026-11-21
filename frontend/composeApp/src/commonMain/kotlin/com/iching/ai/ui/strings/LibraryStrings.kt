package com.iching.ai.ui.strings

import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.domain.oracle.linePositionTh

/** The hexagram library and its detail page. */
interface LibraryStrings {
    val title: String
    val subtitle: String
    val clear: String
    val searchPlaceholder: String
    val filterAll: String
    val noResults: String
    fun noResultsFor(query: String): String
    fun hexagramNumber(id: Int): String
    val upperTrigram: String
    val lowerTrigram: String
    val overview: String
    val sixLines: String
    val whenAllLinesChange: String
    fun linePosition(position: Int): String
}

object ThLibraryStrings : LibraryStrings {
    private val lang = AppLanguage.TH

    override val title = "คัมภีร์ ๖๔ กวา"
    override val subtitle = "สารานุกรมแห่งความเปลี่ยนแปลง"
    override val clear = "ล้าง"
    override val searchPlaceholder = "ค้นหาด้วยชื่อ เลข หรือความหมาย"
    override val filterAll = "ทั้งหมด"
    override val noResults = "ไม่พบกวาที่ตรงกับการค้นหา"
    override fun noResultsFor(query: String) = "ไม่พบกวาที่ตรงกับ “$query”"
    override fun hexagramNumber(id: Int) = "กวาที่ ${formatNumber(id, lang)}"
    override val upperTrigram = "ตรีแกรมบน"
    override val lowerTrigram = "ตรีแกรมล่าง"
    override val overview = "ภาพรวม"
    override val sixLines = "เส้นทั้งหก (จากบนลงล่าง)"
    override val whenAllLinesChange = "เมื่อเส้นทั้งหกแปร"
    override fun linePosition(position: Int) = linePositionTh(position)
}

object EnLibraryStrings : LibraryStrings {
    override val title = "The 64 Hexagrams"
    override val subtitle = "An encyclopedia of change"
    override val clear = "Clear"
    override val searchPlaceholder = "Search by name, number or meaning"
    override val filterAll = "All"
    override val noResults = "No hexagram matches your search."
    override fun noResultsFor(query: String) = "No hexagram matches “$query”."
    override fun hexagramNumber(id: Int) = "Hexagram $id"
    override val upperTrigram = "Upper trigram"
    override val lowerTrigram = "Lower trigram"
    override val overview = "Overview"
    override val sixLines = "The six lines (top to bottom)"
    override val whenAllLinesChange = "When all six lines change"
    override fun linePosition(position: Int) = "Line $position"
}
