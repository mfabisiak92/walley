package com.walley.app.data.local

import androidx.room.Entity

/** Links a watched equity (strategy) to a portfolio investment it applies to; many-to-many. */
@Entity(tableName = "strategy_investment_links", primaryKeys = ["equityId", "investmentId"])
data class StrategyInvestmentLinkEntity(
    val equityId: Long,
    val investmentId: Long
)
