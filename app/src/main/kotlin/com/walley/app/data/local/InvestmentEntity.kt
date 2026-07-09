package com.walley.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.InvestmentCategory
import java.math.BigDecimal

@Entity(tableName = "investments")
data class InvestmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ticker: String,
    val category: InvestmentCategory = InvestmentCategory.STOCK,
    val currency: Currency,
    val currentPrice: BigDecimal,
    val accountId: Long? = null
)
