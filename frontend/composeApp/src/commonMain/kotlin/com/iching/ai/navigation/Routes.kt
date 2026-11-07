package com.iching.ai.navigation

import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
/** [resumeId] is a journal entry whose cast the hermit is asked about again, instead of a new cast. */
@Serializable data class DivinationRoute(val question: String, val category: String? = null, val resumeId: Long? = null)
@Serializable data object DailyRoute
@Serializable data object YearRoute
@Serializable data object LibraryRoute
@Serializable data class HexagramRoute(val id: Int)
@Serializable data object JournalRoute
@Serializable data class JournalEntryRoute(val id: Long)
@Serializable data object SettingsRoute
@Serializable data object PaywallRoute
