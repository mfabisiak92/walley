package com.walley.app.data.repository

import com.walley.app.data.local.LiabilityDao
import com.walley.app.data.local.LiabilityEntity
import com.walley.app.data.local.toDomain
import com.walley.app.data.local.toMinorUnits
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.Liability
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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

    override suspend fun syncEstimatedTaxLiabilities(amountsByYear: Map<Int, BigDecimal>, currency: Currency) {
        for ((year, amount) in amountsByYear) {
            val rounded = amount.setScale(2, RoundingMode.HALF_UP)
            liabilityDao.upsertEstimatedTaxLiability(
                name = "Tax $year",
                currency = currency,
                amountMinorUnits = rounded.toMinorUnits(),
                startDate = LocalDate.of(year, 1, 1)
            )
        }

        val existingByYear = liabilityDao.observeAll().first()
            .map { it.toDomain() }
            .mapNotNull { liability ->
                ESTIMATED_TAX_NAME.matchEntire(liability.name)
                    ?.groupValues?.get(1)?.toIntOrNull()
                    ?.let { year -> year to liability }
            }
            .toMap()

        // A year that no longer owes anything (e.g. its sells were removed) shouldn't keep a stale liability around.
        for ((year, liability) in existingByYear) {
            if (year !in amountsByYear) {
                deleteLiability(liability.id)
            }
        }
    }

    private companion object {
        val ESTIMATED_TAX_NAME = Regex("""^Tax (\d{4})$""")
    }
}
