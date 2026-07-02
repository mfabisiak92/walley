package com.walley.app.data.local

import androidx.room.TypeConverter
import com.walley.app.domain.model.Currency

class Converters {
    @TypeConverter
    fun fromCurrency(currency: Currency): String = currency.name

    @TypeConverter
    fun toCurrency(value: String): Currency = Currency.valueOf(value)
}
