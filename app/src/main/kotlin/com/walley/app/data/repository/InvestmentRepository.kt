package com.walley.app.data.repository

import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.Investment
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow

interface InvestmentRepository {
    fun observeInvestments(): Flow<List<Investment>>
    suspend fun addInvestment(
        name: String,
        ticker: String,
        quantity: BigDecimal,
        currency: Currency,
        price: BigDecimal
    )
}
