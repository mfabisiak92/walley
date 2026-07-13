package com.walley.app.feature.analytics

import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.BudgetWithItems
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import com.walley.app.feature.budget.convertToCurrency
import java.math.BigDecimal
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val CATEGORIZED_SECTIONS = setOf(BudgetSectionType.FIXED_COSTS, BudgetSectionType.OTHER_COSTS)
private const val UNCATEGORIZED_LABEL = "Uncategorized"
private const val OTHER_LABEL = "Other"

data class CategoryAmounts(val planned: BigDecimal, val paid: BigDecimal)

data class CategorySpendPoint(
    val yearMonth: YearMonth,
    val label: String,
    /** Keyed by the item's icon label (or [UNCATEGORIZED_LABEL]); only categories with an item that month appear. */
    val amountsByCategory: Map<String, CategoryAmounts>
)

/**
 * One point per budget in [budgetsWithItems] (assumed already filtered/sorted by the caller, same
 * convention as [AnalyticsViewModel.toHistoryPoint]), summing Fixed + Other costs items' planned and
 * paid amounts per icon category, converted to [targetCurrency]. Unlike [com.walley.app.feature.budget.sectionTotal],
 * an item whose currency has no available rate is simply excluded from its category's total rather
 * than nulling out the whole point — one bad-currency item shouldn't blank an entire month's chart.
 */
fun categorySpendHistory(
    budgetsWithItems: List<BudgetWithItems>,
    targetCurrency: Currency,
    rates: ExchangeRates?
): List<CategorySpendPoint> = budgetsWithItems.map { budgetWithItems ->
    val amounts = mutableMapOf<String, CategoryAmounts>()
    for (item in budgetWithItems.items) {
        if (item.section !in CATEGORIZED_SECTIONS) continue
        val plannedConverted = convertToCurrency(item.amount, item.currency, targetCurrency, rates)
        val paidConverted = convertToCurrency(item.paidAmount, item.currency, targetCurrency, rates)
        if (plannedConverted == null && paidConverted == null) continue
        val category = item.icon?.label ?: UNCATEGORIZED_LABEL
        val existing = amounts[category] ?: CategoryAmounts(BigDecimal.ZERO, BigDecimal.ZERO)
        amounts[category] = CategoryAmounts(
            planned = existing.planned + (plannedConverted ?: BigDecimal.ZERO),
            paid = existing.paid + (paidConverted ?: BigDecimal.ZERO)
        )
    }
    CategorySpendPoint(
        yearMonth = budgetWithItems.budget.yearMonth,
        label = shortLabel(budgetWithItems.budget.yearMonth),
        amountsByCategory = amounts
    )
}

/** Every distinct category label seen across [points], in descending order of total spend. */
fun distinctCategories(points: List<CategorySpendPoint>): List<String> =
    points.flatMap { it.amountsByCategory.entries }
        .groupBy({ it.key }, { it.value.paid })
        .mapValues { (_, values) -> values.fold(BigDecimal.ZERO) { acc, v -> acc + v } }
        .entries
        .sortedByDescending { it.value }
        .map { it.key }

data class CappedCategorySpend(
    val categoryLabels: List<String>,
    /** One list per [categoryLabels] entry, parallel to the source points, in the same order. */
    val seriesByCategory: List<List<Float?>>
)

/**
 * Reduces [points] to the top [topN] categories by total paid amount across the whole window
 * (descending), collapsing every other category's paid amount per point into a synthetic "Other"
 * bucket appended last. A month with no item for a category contributes 0, not null — a stacked
 * chart can't skip a slice without changing what the bar's total height means.
 */
fun capToTopCategories(points: List<CategorySpendPoint>, topN: Int = 5): CappedCategorySpend {
    val ranked = distinctCategories(points)
    val topCategories = ranked.take(topN)
    val hasOther = ranked.size > topN

    val topSeries = topCategories.map { category ->
        points.map { point -> (point.amountsByCategory[category]?.paid ?: BigDecimal.ZERO).toFloat() }
    }
    val labels = if (hasOther) topCategories + OTHER_LABEL else topCategories
    val series = if (hasOther) {
        val otherSeries = points.map { point ->
            point.amountsByCategory
                .filterKeys { it !in topCategories }
                .values
                .fold(BigDecimal.ZERO) { acc, amounts -> acc + amounts.paid }
                .toFloat()
        }
        topSeries + listOf(otherSeries)
    } else {
        topSeries
    }
    return CappedCategorySpend(labels, series)
}

data class CategoryVarianceSeries(val monthLabels: List<String>, val planned: List<Float?>, val actual: List<Float?>)

/** Planned-vs-paid amounts for [category] across every point in [points]; 0 (not null) where that month had no such item. */
fun varianceForCategory(points: List<CategorySpendPoint>, category: String): CategoryVarianceSeries = CategoryVarianceSeries(
    monthLabels = points.map { it.label },
    planned = points.map { (it.amountsByCategory[category]?.planned ?: BigDecimal.ZERO).toFloat() },
    actual = points.map { (it.amountsByCategory[category]?.paid ?: BigDecimal.ZERO).toFloat() }
)

private fun shortLabel(yearMonth: YearMonth): String {
    val monthLabel = yearMonth.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
    val shortYear = (yearMonth.year % 100).toString().padStart(2, '0')
    return "$monthLabel '$shortYear"
}
