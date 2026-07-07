package com.walley.app.data.repository

import com.walley.app.data.local.LiabilityDao
import com.walley.app.data.local.LiabilityEntity
import com.walley.app.data.local.toDomain
import com.walley.app.data.local.toMinorUnits
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.Liability
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LiabilityRepositoryImpl @Inject constructor(
    private val liabilityDao: LiabilityDao
) : LiabilityRepository {

    override fun observeLiabilities(): Flow<List<Liability>> =
        liabilityDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun addLiability(
        name: String,
        currency: Currency,
        originalAmount: BigDecimal,
        currentBalance: BigDecimal,
        startDate: LocalDate
    ) {
        liabilityDao.insert(
            LiabilityEntity(
                name = name,
                currency = currency,
                originalAmountMinorUnits = originalAmount.toMinorUnits(),
                currentBalanceMinorUnits = currentBalance.toMinorUnits(),
                startDate = startDate
            )
        )
    }

    override suspend fun updateCurrentBalance(liabilityId: Long, currentBalance: BigDecimal) {
        liabilityDao.updateCurrentBalance(liabilityId, currentBalance.toMinorUnits())
    }

    override suspend fun deleteLiability(liabilityId: Long) {
        liabilityDao.delete(liabilityId)
    }
}
