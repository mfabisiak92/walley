package com.walley.app.data.repository

import com.walley.app.data.local.AssetDao
import com.walley.app.data.local.AssetEntity
import com.walley.app.data.local.toDomain
import com.walley.app.data.local.toMinorUnits
import com.walley.app.domain.model.Asset
import com.walley.app.domain.model.Currency
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AssetRepositoryImpl @Inject constructor(
    private val assetDao: AssetDao
) : AssetRepository {

    override fun observeAssets(): Flow<List<Asset>> =
        assetDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun addAsset(
        name: String,
        currency: Currency,
        purchaseValue: BigDecimal,
        currentValue: BigDecimal,
        purchaseDate: LocalDate
    ) {
        assetDao.insert(
            AssetEntity(
                name = name,
                currency = currency,
                purchaseValueMinorUnits = purchaseValue.toMinorUnits(),
                currentValueMinorUnits = currentValue.toMinorUnits(),
                purchaseDate = purchaseDate
            )
        )
    }

    override suspend fun updateCurrentValue(assetId: Long, currentValue: BigDecimal) {
        assetDao.updateCurrentValue(assetId, currentValue.toMinorUnits())
    }

    override suspend fun deleteAsset(assetId: Long) {
        assetDao.delete(assetId)
    }
}
