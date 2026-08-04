package com.walley.app.data.repository

import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.domain.model.InvestmentTransactionType
import com.walley.app.domain.model.InvestmentWithTransactions
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** Result of fetching one investment's price from the market data provider. */
sealed interface PriceFetchOutcome {
    data class Success(val price: BigDecimal) : PriceFetchOutcome
    /** [reason] is shown to the user, e.g. an API error message or a currency mismatch explanation. */
    data class NotFound(val reason: String) : PriceFetchOutcome
}

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
        initialPrice: BigDecimal,
        initialCommission: BigDecimal = BigDecimal.ZERO,
        externalTicker: String? = null
    )

    suspend fun updateInvestmentDetails(
        investmentId: Long,
        name: String,
        ticker: String,
        category: InvestmentCategory,
        accountId: Long,
        externalTicker: String? = null
    )

    suspend fun updateCurrentPrice(investmentId: Long, currentPrice: BigDecimal)

    /** Reverts to the price in effect just before the most recent update; a no-op if there isn't one. */
    suspend fun revertToPreviousPrice(investmentId: Long)

    suspend fun deleteInvestment(investmentId: Long)

    /** Fetches current market prices for [investmentIds] and applies any that resolve successfully. */
    suspend fun refreshMarketPrices(investmentIds: Collection<Long>): Map<Long, PriceFetchOutcome>

    /**
     * Same lookup as [refreshMarketPrices] but never writes to the database — used by the bulk price
     * update screen, where a fetched price is only a preview until the user reviews and confirms it.
     */
    suspend fun fetchMarketPrices(investmentIds: Collection<Long>): Map<Long, PriceFetchOutcome>

    suspend fun addTransaction(
        investmentId: Long,
        type: InvestmentTransactionType,
        date: LocalDate,
        quantity: BigDecimal,
        pricePerUnit: BigDecimal,
        commission: BigDecimal = BigDecimal.ZERO
    )

    suspend fun updateTransaction(
        transactionId: Long,
        type: InvestmentTransactionType,
        date: LocalDate,
        quantity: BigDecimal,
        pricePerUnit: BigDecimal,
        commission: BigDecimal = BigDecimal.ZERO
    )

    suspend fun deleteTransaction(transactionId: Long)

    /**
     * Used by CSV import: finds the existing investment for (accountId, ticker) or creates one
     * (using [name]/[category]/[currentPrice] only when creating), then appends the transaction to it.
     */
    suspend fun importTransaction(
        accountId: Long,
        ticker: String,
        name: String,
        category: InvestmentCategory,
        currency: Currency,
        type: InvestmentTransactionType,
        date: LocalDate,
        quantity: BigDecimal,
        pricePerUnit: BigDecimal,
        commission: BigDecimal
    )
}
