package com.walley.app.data.repository

import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.domain.model.InvestmentTransactionType
import com.walley.app.domain.model.InvestmentWithTransactions
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface InvestmentRepository {
    fun observeInvestments(): Flow<List<InvestmentWithTransactions>>
    fun observeInvestment(investmentId: Long): Flow<InvestmentWithTransactions?>

    suspend fun addInvestment(
        name: String,
        ticker: String,
        category: InvestmentCategory,
        currency: Currency,
        currentPrice: BigDecimal,
        accountId: Long,
        firstPurchaseDate: LocalDate,
        initialQuantity: BigDecimal,
        initialPrice: BigDecimal
    )

    suspend fun updateInvestmentDetails(
        investmentId: Long,
        name: String,
        ticker: String,
        category: InvestmentCategory,
        accountId: Long
    )

    suspend fun updateCurrentPrice(investmentId: Long, currentPrice: BigDecimal)
    suspend fun deleteInvestment(investmentId: Long)

    suspend fun addTransaction(
        investmentId: Long,
        type: InvestmentTransactionType,
        date: LocalDate,
        quantity: BigDecimal,
        pricePerUnit: BigDecimal
    )

    suspend fun updateTransaction(
        transactionId: Long,
        type: InvestmentTransactionType,
        date: LocalDate,
        quantity: BigDecimal,
        pricePerUnit: BigDecimal
    )

    suspend fun deleteTransaction(transactionId: Long)
}
