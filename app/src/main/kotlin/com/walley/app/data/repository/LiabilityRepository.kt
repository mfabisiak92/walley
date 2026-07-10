package com.walley.app.data.repository

import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.Liability
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface LiabilityRepository {
    fun observeLiabilities(): Flow<List<Liability>>
    suspend fun addLiability(
        name: String,
        currency: Currency,
        originalAmount: BigDecimal,
        currentBalance: BigDecimal,
        startDate: LocalDate
    )
    suspend fun updateCurrentBalance(liabilityId: Long, currentBalance: BigDecimal)
    suspend fun deleteLiability(liabilityId: Long)

    /**
     * Keeps one "Tax {year}" liability per entry in [amountsByYear] (amounts already
     * in [currency]) in sync with the live estimate: creates missing ones, and resets both
     * originalAmount and currentBalance only when the estimate itself has moved since last sync —
     * an unchanged estimate leaves currentBalance alone so a manual payment or "mark as fully paid"
     * sticks instead of being overwritten on the next sync.
     * Removes any auto-tax liability for a year no longer in the map (nothing owed anymore).
     */
    suspend fun syncEstimatedTaxLiabilities(amountsByYear: Map<Int, BigDecimal>, currency: Currency)
}
