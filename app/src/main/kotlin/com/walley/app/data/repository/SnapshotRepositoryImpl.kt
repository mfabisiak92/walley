package com.walley.app.data.repository

import com.walley.app.data.local.FinancialSnapshotDao
import com.walley.app.data.local.FinancialSnapshotEntity
import com.walley.app.data.local.toDomain
import com.walley.app.data.local.toMinorUnits
import com.walley.app.domain.model.FinancialSnapshot
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SnapshotRepositoryImpl @Inject constructor(
    private val dao: FinancialSnapshotDao
) : SnapshotRepository {

    override fun observeSnapshots(): Flow<List<FinancialSnapshot>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun previousSnapshot(year: Int, month: Int): FinancialSnapshot? =
        dao.getPrevious(year, month)?.toDomain()

    override suspend fun addSnapshot(snapshot: FinancialSnapshot) {
        dao.insert(
            FinancialSnapshotEntity(
                budgetId = snapshot.budgetId,
                year = snapshot.year,
                month = snapshot.month,
                baseCurrency = snapshot.baseCurrency,
                cashAndCheckingMinorUnits = snapshot.cashAndChecking.toMinorUnits(),
                savingsMinorUnits = snapshot.savings.toMinorUnits(),
                investmentsMinorUnits = snapshot.investments.toMinorUnits(),
                assetsMinorUnits = snapshot.assets.toMinorUnits(),
                liabilitiesMinorUnits = snapshot.liabilities.toMinorUnits(),
                netWorthMinorUnits = snapshot.netWorth.toMinorUnits(),
                incomeMinorUnits = snapshot.income.toMinorUnits(),
                incomeRelatedExpensesMinorUnits = snapshot.incomeRelatedExpenses.toMinorUnits(),
                disposableIncomeMinorUnits = snapshot.disposableIncome.toMinorUnits(),
                salaryIncomeMinorUnits = snapshot.salaryIncome.toMinorUnits(),
                dividendsIncomeMinorUnits = snapshot.dividendsIncome.toMinorUnits(),
                interestIncomeMinorUnits = snapshot.interestIncome.toMinorUnits(),
                otherIncomeMinorUnits = snapshot.otherIncome.toMinorUnits(),
                investmentGrowthMinorUnits = snapshot.investmentGrowth?.toMinorUnits()
            )
        )
    }
}
