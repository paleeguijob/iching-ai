package com.iching.ai.navigation

import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
/** [resumeId] is a journal entry whose cast the hermit is asked about again, instead of a new cast; a non-null [yearOf] reads that year's hexagram, with no toss. */
@Serializable data class DivinationRoute(val question: String, val category: String? = null, val resumeId: Long? = null, val yearOf: Int? = null)
/** The question screen for asking the hermit about the year hexagram. */
@Serializable data class YearAskRoute(val year: Int)
/** A paid-for Siam Si draw from [setId]; [question] may be empty. [journalId] reopens a saved slip instead of drawing. */
@Serializable data class SiamSiRoute(val setId: String, val question: String, val category: String? = null, val journalId: Long? = null)
@Serializable data object DailyRoute
@Serializable data object YearRoute
@Serializable data object LibraryRoute
@Serializable data class HexagramRoute(val id: Int)
@Serializable data object JournalRoute
@Serializable data class JournalEntryRoute(val id: Long)
@Serializable data object SettingsRoute
@Serializable data object PaywallRoute
