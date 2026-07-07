package com.walley.app.data.repository

import com.walley.app.data.local.EquityNoteEntity
import com.walley.app.data.local.WatchedEquityDao
import com.walley.app.data.local.WatchedEquityEntity
import com.walley.app.data.local.toDomain
import com.walley.app.domain.model.EquityNote
import com.walley.app.domain.model.EquityStatus
import com.walley.app.domain.model.WatchedEquityWithNotes
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class WatchedEquityRepositoryImpl @Inject constructor(
    private val dao: WatchedEquityDao
) : WatchedEquityRepository {

    override fun observeEquitiesWithNotes(): Flow<List<WatchedEquityWithNotes>> =
        combine(dao.observeEquities(), dao.observeAllNotes()) { equities, notes ->
            equities.map { equityEntity ->
                WatchedEquityWithNotes(
                    equity = equityEntity.toDomain(),
                    notes = notes
                        .filter { it.equityId == equityEntity.id }
                        .map { it.toDomain() }
                        .sortedWith(compareByDescending<EquityNote> { it.date }.thenByDescending { it.id })
                )
            }
        }

    override fun observeEquityWithNotes(equityId: Long): Flow<WatchedEquityWithNotes?> =
        combine(dao.observeEquityById(equityId), dao.observeNotesForEquity(equityId)) { equityEntity, notes ->
            equityEntity?.let {
                WatchedEquityWithNotes(equity = it.toDomain(), notes = notes.map { note -> note.toDomain() })
            }
        }

    override suspend fun addEquity(
        name: String,
        ticker: String?,
        date: LocalDate,
        status: EquityStatus,
        note: String
    ): Long {
        val equityId = dao.insertEquity(
            WatchedEquityEntity(name = name, ticker = ticker?.trim()?.uppercase()?.takeIf { it.isNotBlank() })
        )
        dao.insertNote(EquityNoteEntity(equityId = equityId, date = date, status = status, note = note))
        return equityId
    }

    override suspend fun addNote(equityId: Long, date: LocalDate, status: EquityStatus, note: String) {
        dao.insertNote(EquityNoteEntity(equityId = equityId, date = date, status = status, note = note))
    }

    override suspend fun updateNote(noteId: Long, date: LocalDate, status: EquityStatus, note: String) {
        dao.updateNote(noteId, date, status, note)
    }

    override suspend fun deleteEquity(equityId: Long) {
        dao.deleteEquityWithNotes(equityId)
    }

    override suspend fun deleteNote(noteId: Long) {
        dao.deleteNote(noteId)
    }
}
