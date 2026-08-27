package com.walley.app.data.repository

import com.walley.app.data.local.FakeLiabilityDao
import com.walley.app.domain.model.Currency
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiabilityRepositoryImplTest {

    @Test
    fun `a year that drops out of the estimate is deleted when still unpaid`() = runBlocking {
        val dao = FakeLiabilityDao()
        val repository = LiabilityRepositoryImpl(dao)
        repository.syncEstimatedTaxLiabilities(mapOf(2025 to BigDecimal("500")), Currency.PLN)

        // The year no longer owes anything (e.g. its sells were removed from the import).
        repository.syncEstimatedTaxLiabilities(emptyMap(), Currency.PLN)

        assertTrue(dao.entities.isEmpty())
    }

    @Test
    fun `a fully paid year is kept as a closed record even if it drops out of the estimate`() = runBlocking {
        val dao = FakeLiabilityDao()
        val repository = LiabilityRepositoryImpl(dao)
        repository.syncEstimatedTaxLiabilities(mapOf(2025 to BigDecimal("500")), Currency.PLN)
        dao.updateCurrentBalance(dao.entities.single().id, 0)

        // Recomputing FIFO from an unrelated later import nudges 2025's estimate to exactly zero.
        repository.syncEstimatedTaxLiabilities(emptyMap(), Currency.PLN)

        val stored = dao.entities.single()
        assertEquals("Tax 2025", stored.name)
        assertEquals(0L, stored.currentBalanceMinorUnits)
    }

    @Test
    fun `startDate maps to January 1st of the tax year`() = runBlocking {
        val dao = FakeLiabilityDao()
        val repository = LiabilityRepositoryImpl(dao)

        repository.syncEstimatedTaxLiabilities(mapOf(2024 to BigDecimal("100")), Currency.PLN)

        assertEquals(LocalDate.of(2024, 1, 1), dao.entities.single().startDate)
    }
}
