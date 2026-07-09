package com.walley.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.walley.app.domain.model.InvestmentTransactionType
import java.math.BigDecimal
import java.time.LocalDate

@Entity(tableName = "investment_transactions")
data class InvestmentTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val investmentId: Long,
    val type: InvestmentTransactionType,
    val date: LocalDate,
    val quantity: BigDecimal,
    val pricePerUnit: BigDecimal
)
