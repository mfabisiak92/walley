package com.walley.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.walley.app.domain.model.Currency
import java.time.LocalDate

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val currency: Currency,
    val purchaseValueMinorUnits: Long,
    val currentValueMinorUnits: Long,
    val purchaseDate: LocalDate
)
