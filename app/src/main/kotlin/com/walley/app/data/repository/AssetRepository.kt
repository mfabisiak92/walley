package com.walley.app.data.repository

import com.walley.app.domain.model.Asset
import com.walley.app.domain.model.Currency
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface AssetRepository {
    fun observeAssets(): Flow<List<Asset>>
    suspend fun addAsset(
        name: String,
        currency: Currency,
        purchaseValue: BigDecimal,
        currentValue: BigDecimal,
        purchaseDate: LocalDate
    )
    suspend fun updateCurrentValue(assetId: Long, currentValue: BigDecimal)
    suspend fun deleteAsset(assetId: Long)
}
