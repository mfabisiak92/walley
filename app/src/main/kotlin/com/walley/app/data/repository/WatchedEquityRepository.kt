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
}
