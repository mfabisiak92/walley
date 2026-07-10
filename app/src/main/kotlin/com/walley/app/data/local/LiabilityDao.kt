package com.walley.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.walley.app.domain.model.Currency
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface LiabilityDao {
    @Query("SELECT * FROM liabilities ORDER BY name ASC")
    fun observeAll(): Flow<List<LiabilityEntity>>

    @Query("SELECT * FROM liabilities WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): LiabilityEntity?

    @Insert
    suspend fun insert(liability: LiabilityEntity): Long

    @Query("UPDATE liabilities SET currentBalanceMinorUnits = :currentBalanceMinorUnits WHERE id = :liabilityId")
    suspend fun updateCurrentBalance(liabilityId: Long, currentBalanceMinorUnits: Long)

    @Query(
        "UPDATE liabilities SET originalAmountMinorUnits = :amountMinorUnits, " +
            "currentBalanceMinorUnits = :amountMinorUnits WHERE id = :liabilityId"
    )
    suspend fun resyncOriginalAndCurrentAmount(liabilityId: Long, amountMinorUnits: Long)

    @Query("DELETE FROM liabilities WHERE id = :liabilityId")
    suspend fun delete(liabilityId: Long)

    // Room serializes @Transaction methods against each other, so the find-then-insert-or-update
    // below is atomic even when triggered repeatedly in quick succession (e.g. once per imported
    // row across several investment accounts) — the second call always sees the first one's write.
    @Transaction
    suspend fun upsertEstimatedTaxLiability(
        name: String,
        currency: Currency,
        amountMinorUnits: Long,
        startDate: LocalDate
    ) {
        val existing = findByName(name)
        when {
            existing == null -> insert(
                LiabilityEntity(
                    name = name,
                    currency = currency,
                    originalAmountMinorUnits = amountMinorUnits,
                    currentBalanceMinorUnits = amountMinorUnits,
                    startDate = startDate
                )
            )
            existing.currentBalanceMinorUnits != amountMinorUnits ->
                resyncOriginalAndCurrentAmount(existing.id, amountMinorUnits)
        }
    }
}
