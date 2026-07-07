package com.walley.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.walley.app.domain.model.EquityStatus
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedEquityDao {
    @Query("SELECT * FROM watched_equities ORDER BY name ASC")
    fun observeEquities(): Flow<List<WatchedEquityEntity>>

    @Query("SELECT * FROM watched_equities WHERE id = :equityId")
    fun observeEquityById(equityId: Long): Flow<WatchedEquityEntity?>

    @Query("SELECT * FROM equity_notes")
    fun observeAllNotes(): Flow<List<EquityNoteEntity>>

    @Query("SELECT * FROM equity_notes WHERE equityId = :equityId ORDER BY date DESC, id DESC")
    fun observeNotesForEquity(equityId: Long): Flow<List<EquityNoteEntity>>

    @Insert
    suspend fun insertEquity(equity: WatchedEquityEntity): Long

    @Insert
    suspend fun insertNote(note: EquityNoteEntity): Long

    @Query("UPDATE equity_notes SET date = :date, status = :status, note = :note WHERE id = :noteId")
    suspend fun updateNote(noteId: Long, date: LocalDate, status: EquityStatus, note: String)

    @Query("DELETE FROM watched_equities WHERE id = :equityId")
    suspend fun deleteEquity(equityId: Long)

    @Query("DELETE FROM equity_notes WHERE equityId = :equityId")
    suspend fun deleteNotesForEquity(equityId: Long)

    @Query("DELETE FROM equity_notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: Long)

    @Transaction
    suspend fun deleteEquityWithNotes(equityId: Long) {
        deleteNotesForEquity(equityId)
        deleteEquity(equityId)
    }
}
