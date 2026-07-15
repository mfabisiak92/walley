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

fun plannedDisposableIncome(items: List<BudgetItem>, targetCurrency: Currency, rates: ExchangeRates?): BigDecimal? {
    val income = sectionTotal(items, BudgetSectionType.INCOME, targetCurrency, rates) ?: return null
    val expenses = sectionTotal(items, BudgetSectionType.INCOME_RELATED_EXPENSES, targetCurrency, rates) ?: return null
    return income - expenses
}

/**
 * [plannedDisposableIncome] adjusted by [additionalAmountToSpend], i.e. what's actually available
 * to spend once finalized Income/Income-related-expenses deviations from plan are accounted for;
 * null if a needed rate is unavailable.
 */
fun disposableIncome(items: List<BudgetItem>, targetCurrency: Currency, rates: ExchangeRates?): BigDecimal? {
    val planned = plannedDisposableIncome(items, targetCurrency, rates) ?: return null
    val additional = additionalAmountToSpend(items, targetCurrency, rates) ?: return null
    return planned + additional
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

/**
 * [disposableIncome] (i.e. including any [additionalAmountToSpend]) minus the total planned across
 * [SPENDING_SECTIONS]; null if a needed rate is unavailable.
 */
fun unallocatedAmount(items: List<BudgetItem>, targetCurrency: Currency, rates: ExchangeRates?): BigDecimal? {
    val disposable = disposableIncome(items, targetCurrency, rates) ?: return null
    val spending = budgetProgress(items, SPENDING_SECTIONS, targetCurrency, rates) ?: return null
    return disposable - spending.planned
}

/**
 * Net change to net worth from this budget's still-unpaid items, in [targetCurrency]; null if a
 * needed rate is unavailable.
 *
 * Income adds (it lands in an account once received); Income-related-expenses/Fixed costs/Other
 * costs subtract (they represent money actually leaving the tracked accounts). Savings and
 * Investments are excluded: paying those items just moves money from one tracked account to
 * another of the user's own (savings/investment) accounts, so it doesn't change net worth whether
 * it's paid yet or not — it's still sitting in an account either way. The one exception is a real
 * Saving account that's deliberately excluded from net worth ([includeSavings] false): its unpaid
 * remainder still counts as net worth (it hasn't left for an excluded account yet), so it has to be
 * added back to offset the exclusion.
 *
 * A finalized Income/Income-related-expenses item ([BudgetItem.isFinalized]) is excluded from its
 * section's remainder entirely — it's locked in as final, so any gap between what was planned and
 * what was actually paid is never going to close. That deviation shows up separately, as spendable
 * budget, via [additionalAmountToSpend].
 */
fun projectedNetWorthDelta(
    items: List<BudgetItem>,
    targetCurrency: Currency,
    rates: ExchangeRates?,
    includeSavings: Boolean = true
): BigDecimal? {
    fun remaining(section: BudgetSectionType): BigDecimal? {
        val progress = budgetProgress(items.filterNot { it.isFinalized }, setOf(section), targetCurrency, rates)
            ?: return null
        return progress.planned - progress.spent
    }

    val income = remaining(BudgetSectionType.INCOME) ?: return null
    val incomeRelatedExpenses = remaining(BudgetSectionType.INCOME_RELATED_EXPENSES) ?: return null
    val savings = remaining(BudgetSectionType.SAVINGS) ?: return null
    val fixedCosts = remaining(BudgetSectionType.FIXED_COSTS) ?: return null
    val otherCosts = remaining(BudgetSectionType.OTHER_COSTS) ?: return null

    val savingsAdjustment = if (includeSavings) BigDecimal.ZERO else savings
    return income - incomeRelatedExpenses + savingsAdjustment - fixedCosts - otherCosts
}

/**
 * Extra budget freed up (or eaten into) by finalized Income/Income-related-expenses items' final
 * deviation from plan, in [targetCurrency]; null if a needed rate is unavailable. Unfinalized items
 * don't contribute — their deviation isn't locked in yet, and may still change.
 *
 * - Income: overpaid adds (more came in than planned), underpaid subtracts (less did).
 * - Income-related expenses: overpaid subtracts (more went out than planned), underpaid adds (less did).
 */
fun additionalAmountToSpend(items: List<BudgetItem>, targetCurrency: Currency, rates: ExchangeRates?): BigDecimal? {
    var total = BigDecimal.ZERO
    for (item in items) {
        if (!item.isFinalized) continue
        val planned = convertToCurrency(item.amount, item.currency, targetCurrency, rates) ?: return null
        val paid = convertToCurrency(item.paidAmount, item.currency, targetCurrency, rates) ?: return null
        total += when (item.section) {
            BudgetSectionType.INCOME -> paid - planned
            BudgetSectionType.INCOME_RELATED_EXPENSES -> planned - paid
            else -> BigDecimal.ZERO
        }
    }
    return total
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
