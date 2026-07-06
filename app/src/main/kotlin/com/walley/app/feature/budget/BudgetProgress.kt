package com.walley.app.feature.budget

import com.walley.app.domain.model.BudgetItem
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import java.math.BigDecimal
import java.math.RoundingMode

/** Sections that represent money going out of disposable income, as opposed to Income/Income-related expenses. */
val SPENDING_SECTIONS: Set<BudgetSectionType> = setOf(
    BudgetSectionType.FIXED_COSTS,
    BudgetSectionType.OTHER_COSTS,
    BudgetSectionType.SAVINGS,
    BudgetSectionType.INVESTMENTS
)

data class BudgetProgress(
    val spentPln: BigDecimal,
    val plannedPln: BigDecimal,
    val percent: BigDecimal
)

/** Converts an amount to PLN using PLN-based rates (i.e. `exchangeRateRepository.observeRates(Currency.PLN)`); null if unavailable. */
fun convertToPln(amount: BigDecimal, currency: Currency, plnRates: ExchangeRates?): BigDecimal? {
    if (currency == Currency.PLN) return amount
    val rate = plnRates?.rates?.get(currency) ?: return null
    return amount.divide(rate, 6, RoundingMode.HALF_UP)
}

/** Converts a PLN amount into [base] using rates observed with `observeRates(base)`; null if unavailable. */
fun convertPlnToBase(amountPln: BigDecimal, base: Currency, baseRates: ExchangeRates?): BigDecimal? {
    if (base == Currency.PLN) return amountPln
    val rate = baseRates?.rates?.get(Currency.PLN) ?: return null
    return amountPln.divide(rate, 6, RoundingMode.HALF_UP)
}

fun sectionTotalPln(items: List<BudgetItem>, section: BudgetSectionType, plnRates: ExchangeRates?): BigDecimal? {
    var total = BigDecimal.ZERO
    for (item in items) {
        if (item.section != section) continue
        total += convertToPln(item.amount, item.currency, plnRates) ?: return null
    }
    return total
}

fun disposableIncomePln(items: List<BudgetItem>, plnRates: ExchangeRates?): BigDecimal? {
    val income = sectionTotalPln(items, BudgetSectionType.INCOME, plnRates) ?: return null
    val expenses = sectionTotalPln(items, BudgetSectionType.INCOME_RELATED_EXPENSES, plnRates) ?: return null
    return income - expenses
}

/** Spent (paid) vs planned (amount) totals in PLN, summed over [sections]; null if a needed rate is unavailable. */
fun budgetProgress(items: List<BudgetItem>, sections: Set<BudgetSectionType>, plnRates: ExchangeRates?): BudgetProgress? {
    var spent = BigDecimal.ZERO
    var planned = BigDecimal.ZERO
    for (item in items) {
        if (item.section !in sections) continue
        planned += convertToPln(item.amount, item.currency, plnRates) ?: return null
        spent += convertToPln(item.paidAmount, item.currency, plnRates) ?: return null
    }
    val percent = if (planned.signum() == 0) {
        BigDecimal.ZERO
    } else {
        (spent.divide(planned, 6, RoundingMode.HALF_UP) * BigDecimal(100)).coerceIn(BigDecimal.ZERO, BigDecimal(100))
    }
    return BudgetProgress(spentPln = spent, plannedPln = planned, percent = percent)
}

/** Disposable income minus the total planned across [SPENDING_SECTIONS]; null if a needed rate is unavailable. */
fun unallocatedPln(items: List<BudgetItem>, plnRates: ExchangeRates?): BigDecimal? {
    val disposable = disposableIncomePln(items, plnRates) ?: return null
    val spending = budgetProgress(items, SPENDING_SECTIONS, plnRates) ?: return null
    return disposable - spending.plannedPln
}
