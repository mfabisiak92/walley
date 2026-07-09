package com.walley.app.data.local

import androidx.room.TypeConverter
import com.walley.app.domain.model.AccountTaxRate
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.BudgetItemIcon
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.BudgetStatus
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.EquityStatus
import com.walley.app.domain.model.IncomeCategory
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.domain.model.InvestmentTransactionType
import java.math.BigDecimal
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate): String = date.toString()

    @TypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)

    @TypeConverter
    fun fromBudgetSectionType(section: BudgetSectionType): String = section.name

    @TypeConverter
    fun toBudgetSectionType(value: String): BudgetSectionType = BudgetSectionType.valueOf(value)

    @TypeConverter
    fun fromCurrency(currency: Currency): String = currency.name

    @TypeConverter
    fun toCurrency(value: String): Currency = Currency.valueOf(value)

    @TypeConverter
    fun fromAccountType(type: AccountType): String = type.name

    @TypeConverter
    fun toAccountType(value: String): AccountType = AccountType.valueOf(value)

    @TypeConverter
    fun fromAccountTaxRate(taxRate: AccountTaxRate): String = taxRate.name

    @TypeConverter
    fun toAccountTaxRate(value: String): AccountTaxRate = AccountTaxRate.valueOf(value)

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal): String = value.toPlainString()

    @TypeConverter
    fun toBigDecimal(value: String): BigDecimal = BigDecimal(value)

    @TypeConverter
    fun fromBudgetStatus(status: BudgetStatus): String = status.name

    @TypeConverter
    fun toBudgetStatus(value: String): BudgetStatus = BudgetStatus.valueOf(value)

    @TypeConverter
    fun fromIncomeCategory(category: IncomeCategory?): String? = category?.name

    @TypeConverter
    fun toIncomeCategory(value: String?): IncomeCategory? = value?.let { IncomeCategory.valueOf(it) }

    @TypeConverter
    fun fromBudgetItemIcon(icon: BudgetItemIcon?): String? = icon?.name

    @TypeConverter
    fun toBudgetItemIcon(value: String?): BudgetItemIcon? = value?.let { BudgetItemIcon.valueOf(it) }

    @TypeConverter
    fun fromEquityStatus(status: EquityStatus): String = status.name

    @TypeConverter
    fun toEquityStatus(value: String): EquityStatus = EquityStatus.valueOf(value)

    @TypeConverter
    fun fromInvestmentCategory(category: InvestmentCategory): String = category.name

    @TypeConverter
    fun toInvestmentCategory(value: String): InvestmentCategory = InvestmentCategory.valueOf(value)

    @TypeConverter
    fun fromInvestmentTransactionType(type: InvestmentTransactionType): String = type.name

    @TypeConverter
    fun toInvestmentTransactionType(value: String): InvestmentTransactionType = InvestmentTransactionType.valueOf(value)
}
