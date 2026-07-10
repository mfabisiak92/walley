package com.walley.app.data.repository

import com.walley.app.domain.model.EquityStatus
import com.walley.app.domain.model.WatchedEquityWithNotes
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface WatchedEquityRepository {
    fun observeEquitiesWithNotes(): Flow<List<WatchedEquityWithNotes>>
    fun observeEquityWithNotes(equityId: Long): Flow<WatchedEquityWithNotes?>

    /** Creates a new equity along with its first note; returns the new equity's id. */
    suspend fun addEquity(
        name: String,
        ticker: String?,
        date: LocalDate,
        status: EquityStatus,
        note: String
    ): Long

    suspend fun addNote(equityId: Long, date: LocalDate, status: EquityStatus, note: String)

    suspend fun updateNote(noteId: Long, date: LocalDate, status: EquityStatus, note: String)

    /** Deletes the equity and all of its notes. */
    suspend fun deleteEquity(equityId: Long)

    suspend fun deleteNote(noteId: Long)

    fun observeLinkedInvestmentIds(equityId: Long): Flow<List<Long>>

    /** Replaces the full set of investments this equity/strategy is linked to. */
    suspend fun setLinkedInvestments(equityId: Long, investmentIds: Set<Long>)

    /**
     * For each linked investment id, the strategy that applies to it. An investment linked to more
     * than one strategy shows whichever has the most recently dated note.
     */
    fun observeStrategiesByInvestmentId(): Flow<Map<Long, WatchedEquityWithNotes>>
}
