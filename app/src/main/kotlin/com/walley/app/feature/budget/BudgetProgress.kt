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
    val spent: BigDecimal,
    val planned: BigDecimal,
    val percent: BigDecimal
)

/**
 * Converts [amount] (in [currency]) into [targetCurrency], using rates observed with
 * `exchangeRateRepository.observeRates(targetCurrency)`; null if a needed rate is unavailable.
 */
fun convertToCurrency(amount: BigDecimal, currency: Currency, targetCurrency: Currency, rates: ExchangeRates?): BigDecimal? {
    if (currency == targetCurrency) return amount
    val rate = rates?.rates?.get(currency) ?: return null
    return amount.divide(rate, 6, RoundingMode.HALF_UP)
}

fun sectionTotal(
    items: List<BudgetItem>,
    section: BudgetSectionType,
    targetCurrency: Currency,
    rates: ExchangeRates?
): BigDecimal? {
    var total = BigDecimal.ZERO
    for (item in items) {
        if (item.section != section) continue
        total += convertToCurrency(item.amount, item.currency, targetCurrency, rates) ?: return null
    }
    return total
}

fun disposableIncome(items: List<BudgetItem>, targetCurrency: Currency, rates: ExchangeRates?): BigDecimal? {
    val income = sectionTotal(items, BudgetSectionType.INCOME, targetCurrency, rates) ?: return null
    val expenses = sectionTotal(items, BudgetSectionType.INCOME_RELATED_EXPENSES, targetCurrency, rates) ?: return null
    return income - expenses
}

/** Spent (paid) vs planned (amount) totals in [targetCurrency], summed over [sections]; null if a rate is unavailable. */
fun budgetProgress(
    items: List<BudgetItem>,
    sections: Set<BudgetSectionType>,
    targetCurrency: Currency,
    rates: ExchangeRates?
): BudgetProgress? {
    var spent = BigDecimal.ZERO
    var planned = BigDecimal.ZERO
    for (item in items) {
        if (item.section !in sections) continue
        planned += convertToCurrency(item.amount, item.currency, targetCurrency, rates) ?: return null
        spent += convertToCurrency(item.paidAmount, item.currency, targetCurrency, rates) ?: return null
    }
    val percent = if (planned.signum() == 0) {
        BigDecimal.ZERO
    } else {
        (spent.divide(planned, 6, RoundingMode.HALF_UP) * BigDecimal(100)).coerceIn(BigDecimal.ZERO, BigDecimal(100))
    }
    return BudgetProgress(spent = spent, planned = planned, percent = percent)
}

/** Disposable income minus the total planned across [SPENDING_SECTIONS]; null if a needed rate is unavailable. */
fun unallocatedAmount(items: List<BudgetItem>, targetCurrency: Currency, rates: ExchangeRates?): BigDecimal? {
    val disposable = disposableIncome(items, targetCurrency, rates) ?: return null
    val spending = budgetProgress(items, SPENDING_SECTIONS, targetCurrency, rates) ?: return null
    return disposable - spending.planned
}

/**
 * Net change to net worth from this budget's still-unpaid items, in [targetCurrency]; null if a
 * needed rate is unavailable. Income/Savings/Investments add (they land in an account once paid);
 * Income-related-expenses/Fixed costs/Other costs subtract (they represent money going out).
 */
fun projectedNetWorthDelta(items: List<BudgetItem>, targetCurrency: Currency, rates: ExchangeRates?): BigDecimal? {
    fun remaining(section: BudgetSectionType): BigDecimal? {
        val progress = budgetProgress(items, setOf(section), targetCurrency, rates) ?: return null
        return progress.planned - progress.spent
    }

    val income = remaining(BudgetSectionType.INCOME) ?: return null
    val incomeRelatedExpenses = remaining(BudgetSectionType.INCOME_RELATED_EXPENSES) ?: return null
    val savings = remaining(BudgetSectionType.SAVINGS) ?: return null
    val investments = remaining(BudgetSectionType.INVESTMENTS) ?: return null
    val fixedCosts = remaining(BudgetSectionType.FIXED_COSTS) ?: return null
    val otherCosts = remaining(BudgetSectionType.OTHER_COSTS) ?: return null

    return income - incomeRelatedExpenses + savings + investments - fixedCosts - otherCosts
}

/**
 * Whether [section]'s [actualPercent] of disposable income should be flagged as a warning against
 * its [targetPercent], for [CategoryTargetIndicator]:
 * - Fixed/Other costs (spending ceilings) and Savings (treated as a ceiling too — a savings item
 *   eating up more of disposable income than planned is worth flagging, undershooting isn't): over
 *   target warns, at/under target doesn't.
 * - Investments (a goal): any deviation from target, over or under, warns — since the percentages
 *   across every section have to add up, overshooting one still means another section got
 *   shortchanged, so it's just as worth flagging as falling short of the goal.
 */
fun isCategoryTargetWarning(section: BudgetSectionType, actualPercent: BigDecimal, targetPercent: BigDecimal): Boolean {
    val diff = actualPercent.setScale(1, RoundingMode.HALF_UP) - targetPercent.setScale(1, RoundingMode.HALF_UP)
    val isOver = diff.signum() > 0
    val isUnder = diff.signum() < 0
    val warnsOnUndershoot = section == BudgetSectionType.INVESTMENTS
    return if (warnsOnUndershoot) (isOver || isUnder) else isOver
}
