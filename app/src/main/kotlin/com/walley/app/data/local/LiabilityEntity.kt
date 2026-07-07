package com.walley.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.walley.app.domain.model.Currency
import java.time.LocalDate

@Entity(tableName = "liabilities")
data class LiabilityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val currency: Currency,
    val originalAmountMinorUnits: Long,
    val currentBalanceMinorUnits: Long,
    val startDate: LocalDate
)
