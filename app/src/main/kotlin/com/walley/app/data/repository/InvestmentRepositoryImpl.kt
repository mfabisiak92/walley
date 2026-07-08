package com.walley.app.data.repository

import com.walley.app.data.local.InvestmentDao
import com.walley.app.data.local.InvestmentEntity
import com.walley.app.data.local.toDomain
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.Investment
import com.walley.app.domain.model.InvestmentCategory
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InvestmentRepositoryImpl @Inject constructor(
    private val investmentDao: InvestmentDao
) : InvestmentRepository {

    override fun observeInvestments(): Flow<List<Investment>> =
        investmentDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun addInvestment(
        name: String,
        ticker: String,
        category: InvestmentCategory,
        purchaseDate: LocalDate,
        quantity: BigDecimal,
        currency: Currency,
        price: BigDecimal,
        currentPrice: BigDecimal,
        accountId: Long
    ) {
        investmentDao.insert(
            InvestmentEntity(
                name = name,
                ticker = ticker,
                category = category,
                purchaseDate = purchaseDate,
                quantity = quantity,
                currency = currency,
                price = price,
                currentPrice = currentPrice,
                accountId = accountId
            )
        )
    }

    override suspend fun updateInvestment(
        investmentId: Long,
        name: String,
        ticker: String,
        category: InvestmentCategory,
        purchaseDate: LocalDate,
        quantity: BigDecimal,
        price: BigDecimal,
        currentPrice: BigDecimal,
        accountId: Long
    ) {
        investmentDao.update(investmentId, name, ticker, category, purchaseDate, quantity, price, currentPrice, accountId)
    }

    override suspend fun updateCurrentPrice(investmentId: Long, currentPrice: BigDecimal) {
        investmentDao.updateCurrentPrice(investmentId, currentPrice)
    }

    override suspend fun deleteInvestment(investmentId: Long) {
        investmentDao.delete(investmentId)
    }
}
