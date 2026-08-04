package com.walley.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.InvestmentCategory
import java.math.BigDecimal
import java.time.LocalDate

@Entity(tableName = "investments")
data class InvestmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ticker: String,
    val category: InvestmentCategory = InvestmentCategory.STOCK,
    val currency: Currency,
    val currentPrice: BigDecimal,
    val accountId: Long? = null,
    val lastPriceUpdate: LocalDate? = null,
    /** currentPrice as of just before the most recent price update — lets the user revert to it. Null until a price has been updated at least once. */
    val previousPrice: BigDecimal? = null,
    // Column kept as "exchange" (from when this held a Twelve Data exchange code) to avoid another
    // migration — it now holds a full Yahoo Finance symbol instead.
    @ColumnInfo(name = "exchange") val externalTicker: String? = null
)
