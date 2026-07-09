package com.walley.app.data.repository

import com.walley.app.data.local.InvestmentDao
import com.walley.app.data.local.InvestmentEntity
import com.walley.app.data.local.InvestmentTransactionEntity
import com.walley.app.data.local.toDomain
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.domain.model.InvestmentTransactionType
import com.walley.app.domain.model.InvestmentWithTransactions
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class InvestmentRepositoryImpl @Inject constructor(
    private val investmentDao: InvestmentDao
) : InvestmentRepository {

    override fun observeInvestments(): Flow<List<InvestmentWithTransactions>> =
        combine(investmentDao.observeAll(), investmentDao.observeAllTransactions()) { investments, transactions ->
            investments.map { entity ->
                InvestmentWithTransactions(
                    investment = entity.toDomain(),
                    transactions = transactions.filter { it.investmentId == entity.id }.map { it.toDomain() }
                )
            }
        }

    override fun observeInvestment(investmentId: Long): Flow<InvestmentWithTransactions?> =
        combine(
            investmentDao.observeById(investmentId),
            investmentDao.observeTransactionsForInvestment(investmentId)
        ) { entity, transactions ->
            entity?.let {
                InvestmentWithTransactions(investment = it.toDomain(), transactions = transactions.map { t -> t.toDomain() })
            }
        }

    override suspend fun addInvestment(
        name: String,
        ticker: String,
        category: InvestmentCategory,
        currency: Currency,
        currentPrice: BigDecimal,
        accountId: Long,
        firstPurchaseDate: LocalDate,
        initialQuantity: BigDecimal,
        initialPrice: BigDecimal
    ) {
        val investmentId = investmentDao.insert(
            InvestmentEntity(
                name = name,
                ticker = ticker,
                category = category,
                currency = currency,
                currentPrice = currentPrice,
                accountId = accountId
            )
        )
        investmentDao.insertTransaction(
            InvestmentTransactionEntity(
                investmentId = investmentId,
                type = InvestmentTransactionType.BUY,
                date = firstPurchaseDate,
                quantity = initialQuantity,
                pricePerUnit = initialPrice
            )
        )
    }

    override suspend fun updateInvestmentDetails(
        investmentId: Long,
        name: String,
        ticker: String,
        category: InvestmentCategory,
        accountId: Long
    ) {
        investmentDao.update(investmentId, name, ticker, category, accountId)
    }

    override suspend fun updateCurrentPrice(investmentId: Long, currentPrice: BigDecimal) {
        investmentDao.updateCurrentPrice(investmentId, currentPrice)
    }

    override suspend fun deleteInvestment(investmentId: Long) {
        investmentDao.deleteInvestmentWithTransactions(investmentId)
    }

    override suspend fun addTransaction(
        investmentId: Long,
        type: InvestmentTransactionType,
        date: LocalDate,
        quantity: BigDecimal,
        pricePerUnit: BigDecimal
    ) {
        investmentDao.insertTransaction(
            InvestmentTransactionEntity(
                investmentId = investmentId,
                type = type,
                date = date,
                quantity = quantity,
                pricePerUnit = pricePerUnit
            )
        )
    }

    override suspend fun updateTransaction(
        transactionId: Long,
        type: InvestmentTransactionType,
        date: LocalDate,
        quantity: BigDecimal,
        pricePerUnit: BigDecimal
    ) {
        investmentDao.updateTransaction(transactionId, type, date, quantity, pricePerUnit)
    }

    override suspend fun deleteTransaction(transactionId: Long) {
        investmentDao.deleteTransaction(transactionId)
    }
}
