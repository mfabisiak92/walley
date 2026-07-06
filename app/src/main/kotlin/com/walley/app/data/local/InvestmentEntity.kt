package com.walley.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.walley.app.domain.model.Currency
import java.math.BigDecimal

@Entity(tableName = "investments")
data class InvestmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ticker: String,
    val quantity: BigDecimal,
    val currency: Currency,
    val price: BigDecimal,
    val accountId: Long? = null
)
