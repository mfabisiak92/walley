package com.walley.app.data.repository

import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.Investment
import com.walley.app.domain.model.InvestmentCategory
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface InvestmentRepository {
    fun observeInvestments(): Flow<List<Investment>>
    suspend fun addInvestment(
        name: String,
        ticker: String,
        category: InvestmentCategory,
        purchaseDate: LocalDate,
        quantity: BigDecimal,
        currency: Currency,
        price: BigDecimal,
        currentPrice: BigDecimal,
        accountId: Long
    )
    suspend fun updateInvestment(
        investmentId: Long,
        name: String,
        ticker: String,
        category: InvestmentCategory,
        purchaseDate: LocalDate,
        quantity: BigDecimal,
        price: BigDecimal,
        currentPrice: BigDecimal,
        accountId: Long
    )
    suspend fun updateCurrentPrice(investmentId: Long, currentPrice: BigDecimal)
    suspend fun deleteInvestment(investmentId: Long)
}
