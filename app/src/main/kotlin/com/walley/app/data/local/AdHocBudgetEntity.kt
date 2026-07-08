package com.walley.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.walley.app.domain.model.Currency
import java.time.LocalDate

@Entity(tableName = "adhoc_budgets")
data class AdHocBudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val accountId: Long,
    val currency: Currency,
    val applyAccountEffects: Boolean = true
)
