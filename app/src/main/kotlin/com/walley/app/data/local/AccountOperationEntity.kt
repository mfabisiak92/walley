package com.walley.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDate

@Entity(tableName = "account_operations")
data class AccountOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val date: LocalDate,
    val description: String,
    val amount: BigDecimal
)
