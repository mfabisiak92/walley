package com.walley.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.walley.app.domain.model.AccountTaxRate
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.Currency
import java.math.BigDecimal

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType = AccountType.CHECKING,
    val currency: Currency,
    val balanceMinorUnits: Long,
    val taxRate: AccountTaxRate = AccountTaxRate.STANDARD_19,
    val targetAmountMinorUnits: Long? = null,
    val isDefault: Boolean = false,
    val commissionFlatMinorUnits: Long = 0,
    val commissionPercent: BigDecimal = BigDecimal.ZERO
)
