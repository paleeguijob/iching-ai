package com.iching.ai.domain.usecase

import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.Hexagram
import com.iching.ai.domain.model.Reading
import com.iching.ai.domain.repository.HexagramRepository
import com.iching.ai.domain.repository.JournalRepository
import com.iching.ai.domain.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

class GetHexagramsUseCase(private val hexagrams: HexagramRepository) {
    suspend operator fun invoke(): List<Hexagram> = hexagrams.all()
}

class GetHexagramUseCase(private val hexagrams: HexagramRepository) {
    suspend operator fun invoke(id: Int): Hexagram = hexagrams.byId(id)
}

/** Matches Thai name, Chinese name, pinyin, number or keyword. */
class SearchHexagramsUseCase(private val hexagrams: HexagramRepository) {
    suspend operator fun invoke(query: String): List<Hexagram> {
        val all = hexagrams.all()
        val q = query.trim().lowercase()
        if (q.isEmpty()) return all
        return all.filter { h ->
            h.id.toString() == q ||
                h.nameTh.lowercase().contains(q) ||
                h.nameCn.contains(q) ||
                h.pinyin.lowercase().contains(q) ||
                h.keywords.any { it.lowercase().contains(q) }
        }
    }
}

class ObserveJournalUseCase(private val journal: JournalRepository) {
    operator fun invoke(): Flow<List<Reading>> = journal.observeAll()
}

class GetReadingUseCase(private val journal: JournalRepository) {
    suspend operator fun invoke(id: Long): Reading? = journal.get(id)
}

class SaveReadingUseCase(private val journal: JournalRepository) {
    suspend operator fun invoke(reading: Reading): Long = journal.save(reading)
}

/** Deletes on the device and the server; offline, the server copy stays marked for the next sign-in. */
class DeleteReadingUseCase(
    private val journal: JournalRepository,
    private val deleteOnServer: suspend (String) -> Unit,
) {
    suspend operator fun invoke(id: Long) {
        val serverId = journal.get(id)?.serverId
        if (serverId != null) journal.markDeleted(serverId)
        journal.delete(id)
        if (serverId == null) return
        try {
            deleteOnServer(serverId)
            journal.deletionSynced(serverId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Still marked: AppSession retries, and merging from the server skips it meanwhile.
        }
    }
}

/** Free seekers see the last [days] days of their journal; null = everything (Premium). */
class FilterJournalUseCase {
    operator fun invoke(readings: List<Reading>, days: Int?, nowMillis: Long): Pair<List<Reading>, Int> {
        if (days == null) return readings to 0
        val cutoff = nowMillis - days * 24L * 60 * 60 * 1000
        val (visible, hidden) = readings.partition { it.createdAtMillis >= cutoff }
        return visible to hidden.size
    }
}

class ObserveSettingsUseCase(private val settings: SettingsRepository) {
    operator fun invoke(): Flow<AppSettings> = settings.settings
}

class UpdateSettingsUseCase(private val settings: SettingsRepository) {
    suspend operator fun invoke(transform: (AppSettings) -> AppSettings) = settings.update(transform)
}
