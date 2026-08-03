package com.walley.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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
    suspend fun insertEquities(equities: List<WatchedEquityEntity>): List<Long>

    @Insert
    suspend fun insertNote(note: EquityNoteEntity): Long

    @Insert
    suspend fun insertNotes(notes: List<EquityNoteEntity>)

    @Query("UPDATE equity_notes SET date = :date, status = :status, note = :note WHERE id = :noteId")
    suspend fun updateNote(noteId: Long, date: LocalDate, status: EquityStatus, note: String)

    @Query("DELETE FROM watched_equities WHERE id = :equityId")
    suspend fun deleteEquity(equityId: Long)

    @Query("DELETE FROM equity_notes WHERE equityId = :equityId")
    suspend fun deleteNotesForEquity(equityId: Long)

    @Query("DELETE FROM equity_notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: Long)

    @Query("SELECT * FROM strategy_investment_links")
    fun observeAllStrategyLinks(): Flow<List<StrategyInvestmentLinkEntity>>

    @Query("SELECT investmentId FROM strategy_investment_links WHERE equityId = :equityId")
    fun observeLinkedInvestmentIds(equityId: Long): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStrategyLinks(links: List<StrategyInvestmentLinkEntity>)

    @Query("DELETE FROM strategy_investment_links WHERE equityId = :equityId")
    suspend fun deleteStrategyLinksForEquity(equityId: Long)

    @Transaction
    suspend fun setLinkedInvestments(equityId: Long, investmentIds: Set<Long>) {
        deleteStrategyLinksForEquity(equityId)
        insertStrategyLinks(investmentIds.map { StrategyInvestmentLinkEntity(equityId = equityId, investmentId = it) })
    }

    @Transaction
    suspend fun deleteEquityWithNotes(equityId: Long) {
        deleteNotesForEquity(equityId)
        deleteStrategyLinksForEquity(equityId)
        deleteEquity(equityId)
    }
}
