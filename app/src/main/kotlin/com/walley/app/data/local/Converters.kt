package com.walley.app.data.local

import androidx.room.TypeConverter
import com.walley.app.domain.model.AccountTaxRate
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.Currency
import java.math.BigDecimal
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate): String = date.toString()

    @TypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)
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
}
