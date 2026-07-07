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
}
