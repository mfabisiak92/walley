package com.walley.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDate

/**
 * One dated price point for an investment, in that investment's own currency. Populated two ways:
 * a backfill from Yahoo Finance history, and a row recorded automatically every time the current
 * price changes — so the series is self-sufficient going forward even without ever backfilling.
 * Unique on (investmentId, date) so both paths can freely overwrite same-day rows via REPLACE.
 */
@Entity(
    tableName = "investment_price_history",
    indices = [Index(value = ["investmentId", "date"], unique = true)]
)
data class InvestmentPriceHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val investmentId: Long,
    val date: LocalDate,
    val closePrice: BigDecimal
)
