package com.walley.app.data.local

import com.walley.app.domain.model.Currency
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * In-memory fake exercising [LiabilityDao.upsertEstimatedTaxLiability]'s real default-method logic
 * (not a reimplementation of it) against a plain list, so its resync/skip decision can be unit
 * tested without a real Room database.
 */
internal class FakeLiabilityDao : LiabilityDao {
    val entities = mutableListOf<LiabilityEntity>()
    private var nextId = 1L

    override fun observeAll(): Flow<List<LiabilityEntity>> = flowOf(entities.toList())

    override suspend fun findByName(name: String): LiabilityEntity? = entities.find { it.name == name }

    override suspend fun insert(liability: LiabilityEntity): Long {
        val id = nextId++
        entities += liability.copy(id = id)
        return id
    }

    override suspend fun insertAll(liabilities: List<LiabilityEntity>) {
        liabilities.forEach { insert(it) }
    }

    override suspend fun updateCurrentBalance(liabilityId: Long, currentBalanceMinorUnits: Long) {
        replace(liabilityId) { it.copy(currentBalanceMinorUnits = currentBalanceMinorUnits) }
    }

    override suspend fun resyncOriginalAndCurrentAmount(liabilityId: Long, amountMinorUnits: Long) {
        replace(liabilityId) { it.copy(originalAmountMinorUnits = amountMinorUnits, currentBalanceMinorUnits = amountMinorUnits) }
    }

    override suspend fun delete(liabilityId: Long) {
        entities.removeAll { it.id == liabilityId }
    }

    private fun replace(id: Long, transform: (LiabilityEntity) -> LiabilityEntity) {
        val index = entities.indexOfFirst { it.id == id }
        entities[index] = transform(entities[index])
    }
}

class LiabilityDaoTest {

    @Test
    fun `a brand new estimated tax liability is inserted`() = runBlocking {
        val dao = FakeLiabilityDao()

        dao.upsertEstimatedTaxLiability("Tax 2026", Currency.PLN, 50_000, LocalDate.of(2026, 1, 1))

        val stored = dao.entities.single()
        assertEquals(50_000L, stored.originalAmountMinorUnits)
        assertEquals(50_000L, stored.currentBalanceMinorUnits)
    }

    @Test
    fun `an unpaid liability's balance tracks a changed estimate`() = runBlocking {
        val dao = FakeLiabilityDao()
        dao.upsertEstimatedTaxLiability("Tax 2026", Currency.PLN, 50_000, LocalDate.of(2026, 1, 1))

        dao.upsertEstimatedTaxLiability("Tax 2026", Currency.PLN, 62_000, LocalDate.of(2026, 1, 1))

        val stored = dao.entities.single()
        assertEquals(62_000L, stored.originalAmountMinorUnits)
        assertEquals(62_000L, stored.currentBalanceMinorUnits)
    }

    @Test
    fun `a fully paid liability is left alone even when the estimate changes`() = runBlocking {
        val dao = FakeLiabilityDao()
        dao.upsertEstimatedTaxLiability("Tax 2026", Currency.PLN, 50_000, LocalDate.of(2026, 1, 1))
        dao.updateCurrentBalance(dao.entities.single().id, 0)

        // Simulates an unrelated import shifting FIFO lot matching and nudging the estimate.
        dao.upsertEstimatedTaxLiability("Tax 2026", Currency.PLN, 50_150, LocalDate.of(2026, 1, 1))

        val stored = dao.entities.single()
        assertEquals(0L, stored.currentBalanceMinorUnits)
        assertEquals(50_000L, stored.originalAmountMinorUnits)
    }

    @Test
    fun `a fully paid liability with an unchanged estimate stays untouched`() = runBlocking {
        val dao = FakeLiabilityDao()
        dao.upsertEstimatedTaxLiability("Tax 2026", Currency.PLN, 50_000, LocalDate.of(2026, 1, 1))
        dao.updateCurrentBalance(dao.entities.single().id, 0)

        dao.upsertEstimatedTaxLiability("Tax 2026", Currency.PLN, 50_000, LocalDate.of(2026, 1, 1))

        val stored = dao.entities.single()
        assertEquals(0L, stored.currentBalanceMinorUnits)
        assertEquals(50_000L, stored.originalAmountMinorUnits)
    }
}
