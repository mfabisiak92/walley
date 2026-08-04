package com.walley.app.domain.model

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.walley.app.R
import com.walley.app.feature.budget.BudgetDetailTab
import com.walley.app.feature.home.NetWorthCategory

/**
 * Localized replacements for each domain enum's `label` field. Two flavors per enum: a
 * `@Composable` one for UI code (via [stringResource]), and a [Context]-based one for call sites
 * outside Compose (ViewModels, chart-data builders) that still need a resolved display string.
 */

@StringRes
private fun BudgetSectionType.displayNameRes(): Int = when (this) {
    BudgetSectionType.INCOME -> R.string.domain_budget_section_income
    BudgetSectionType.INCOME_RELATED_EXPENSES -> R.string.domain_budget_section_income_related_expenses
    BudgetSectionType.FIXED_COSTS -> R.string.domain_budget_section_fixed_costs
    BudgetSectionType.OTHER_COSTS -> R.string.domain_budget_section_other_costs
    BudgetSectionType.SAVINGS -> R.string.domain_budget_section_savings
    BudgetSectionType.INVESTMENTS -> R.string.domain_budget_section_investments
}

@Composable
fun BudgetSectionType.displayName(): String = stringResource(displayNameRes())
fun BudgetSectionType.displayName(context: Context): String = context.getString(displayNameRes())

@StringRes
private fun AccountEffectsGroup.displayNameRes(): Int = when (this) {
    AccountEffectsGroup.INCOME -> R.string.domain_account_effects_group_income
    AccountEffectsGroup.COSTS -> R.string.domain_account_effects_group_costs
    AccountEffectsGroup.SAVINGS -> R.string.domain_account_effects_group_savings
    AccountEffectsGroup.INVESTMENTS -> R.string.domain_account_effects_group_investments
}

@Composable
fun AccountEffectsGroup.displayName(): String = stringResource(displayNameRes())
fun AccountEffectsGroup.displayName(context: Context): String = context.getString(displayNameRes())

@StringRes
private fun InvestmentCategory.displayNameRes(): Int = when (this) {
    InvestmentCategory.STOCK -> R.string.domain_investment_category_stock
    InvestmentCategory.ETF -> R.string.domain_investment_category_etf
    InvestmentCategory.BOND -> R.string.domain_investment_category_bond
    InvestmentCategory.PRECIOUS_METAL -> R.string.domain_investment_category_precious_metal
    InvestmentCategory.ENERGY_METAL -> R.string.domain_investment_category_energy_metal
    InvestmentCategory.CRYPTO -> R.string.domain_investment_category_crypto
}

@Composable
fun InvestmentCategory.displayName(): String = stringResource(displayNameRes())
fun InvestmentCategory.displayName(context: Context): String = context.getString(displayNameRes())

@StringRes
private fun AccountType.displayNameRes(): Int = when (this) {
    AccountType.CHECKING -> R.string.domain_account_type_checking
    AccountType.SAVING -> R.string.domain_account_type_saving
    AccountType.INVESTMENT -> R.string.domain_account_type_investment
    AccountType.CASH -> R.string.domain_account_type_cash
}

@Composable
fun AccountType.displayName(): String = stringResource(displayNameRes())
fun AccountType.displayName(context: Context): String = context.getString(displayNameRes())

@StringRes
private fun IncomeCategory.displayNameRes(): Int = when (this) {
    IncomeCategory.SALARY -> R.string.domain_income_category_salary
    IncomeCategory.DIVIDENDS -> R.string.domain_income_category_dividends
    IncomeCategory.INTEREST -> R.string.domain_income_category_interest
    IncomeCategory.OTHER -> R.string.domain_income_category_other
}

@Composable
fun IncomeCategory.displayName(): String = stringResource(displayNameRes())
fun IncomeCategory.displayName(context: Context): String = context.getString(displayNameRes())

@StringRes
private fun AccountTaxRate.displayNameRes(): Int = when (this) {
    AccountTaxRate.TAX_FREE -> R.string.domain_account_tax_rate_tax_free
    AccountTaxRate.STANDARD_19 -> R.string.domain_account_tax_rate_standard_19
}

@Composable
fun AccountTaxRate.displayName(): String = stringResource(displayNameRes())
fun AccountTaxRate.displayName(context: Context): String = context.getString(displayNameRes())

@StringRes
private fun AccountBalanceGroup.displayNameRes(): Int = when (this) {
    AccountBalanceGroup.CASH_CHECKING -> R.string.domain_account_balance_group_cash_checking
    AccountBalanceGroup.SAVINGS -> R.string.domain_account_balance_group_savings
    AccountBalanceGroup.INVESTMENTS -> R.string.domain_account_balance_group_investments
}

@Composable
fun AccountBalanceGroup.displayName(): String = stringResource(displayNameRes())
fun AccountBalanceGroup.displayName(context: Context): String = context.getString(displayNameRes())

@StringRes
private fun EquityStatus.displayNameRes(): Int = when (this) {
    EquityStatus.SELL -> R.string.domain_equity_status_sell
    EquityStatus.HOLD -> R.string.domain_equity_status_hold
    EquityStatus.WAIT -> R.string.domain_equity_status_wait
    EquityStatus.BUY -> R.string.domain_equity_status_buy
}

@Composable
fun EquityStatus.displayName(): String = stringResource(displayNameRes())
fun EquityStatus.displayName(context: Context): String = context.getString(displayNameRes())

@StringRes
private fun InvestmentTransactionType.displayNameRes(): Int = when (this) {
    InvestmentTransactionType.BUY -> R.string.domain_investment_transaction_type_buy
    InvestmentTransactionType.SELL -> R.string.domain_investment_transaction_type_sell
}

@Composable
fun InvestmentTransactionType.displayName(): String = stringResource(displayNameRes())
fun InvestmentTransactionType.displayName(context: Context): String = context.getString(displayNameRes())

@StringRes
private fun NetWorthCategory.displayNameRes(): Int = when (this) {
    NetWorthCategory.CHECKING -> R.string.domain_net_worth_category_checking
    NetWorthCategory.SAVING -> R.string.domain_net_worth_category_saving
    NetWorthCategory.CASH -> R.string.domain_net_worth_category_cash
    NetWorthCategory.INVESTMENT -> R.string.domain_net_worth_category_investment
    NetWorthCategory.ASSET -> R.string.domain_net_worth_category_asset
    NetWorthCategory.LIABILITY -> R.string.domain_net_worth_category_liability
}

@Composable
fun NetWorthCategory.displayName(): String = stringResource(displayNameRes())
fun NetWorthCategory.displayName(context: Context): String = context.getString(displayNameRes())

@StringRes
private fun BudgetDetailTab.displayNameRes(): Int = when (this) {
    BudgetDetailTab.SUMMARY -> R.string.domain_budget_detail_tab_summary
    BudgetDetailTab.INCOME -> R.string.domain_budget_detail_tab_income
    BudgetDetailTab.FIXED_COSTS -> R.string.domain_budget_detail_tab_fixed_costs
    BudgetDetailTab.OTHER_COSTS -> R.string.domain_budget_detail_tab_other_costs
    BudgetDetailTab.SAVINGS -> R.string.domain_budget_detail_tab_savings
    BudgetDetailTab.INVESTMENTS -> R.string.domain_budget_detail_tab_investments
}

@Composable
fun BudgetDetailTab.displayName(): String = stringResource(displayNameRes())
fun BudgetDetailTab.displayName(context: Context): String = context.getString(displayNameRes())

@StringRes
private fun BudgetItemIcon.displayNameRes(): Int = when (this) {
    BudgetItemIcon.SALARY -> R.string.domain_budget_item_icon_salary
    BudgetItemIcon.DIVIDENDS -> R.string.domain_budget_item_icon_dividends
    BudgetItemIcon.INTEREST -> R.string.domain_budget_item_icon_interest
    BudgetItemIcon.SAVING -> R.string.domain_budget_item_icon_saving
    BudgetItemIcon.INVESTMENT -> R.string.domain_budget_item_icon_investment
    BudgetItemIcon.TAX -> R.string.domain_budget_item_icon_tax
    BudgetItemIcon.CAR -> R.string.domain_budget_item_icon_car
    BudgetItemIcon.FUEL -> R.string.domain_budget_item_icon_fuel
    BudgetItemIcon.SUBSCRIPTION -> R.string.domain_budget_item_icon_subscription
    BudgetItemIcon.GROCERIES -> R.string.domain_budget_item_icon_groceries
    BudgetItemIcon.CLOTHES -> R.string.domain_budget_item_icon_clothes
    BudgetItemIcon.EATING_OUT -> R.string.domain_budget_item_icon_eating_out
    BudgetItemIcon.ENTERTAINMENT -> R.string.domain_budget_item_icon_entertainment
    BudgetItemIcon.TRANSPORTATION -> R.string.domain_budget_item_icon_transportation
    BudgetItemIcon.VACATIONS -> R.string.domain_budget_item_icon_vacations
    BudgetItemIcon.TRIP -> R.string.domain_budget_item_icon_trip
    BudgetItemIcon.GIFT -> R.string.domain_budget_item_icon_gift
    BudgetItemIcon.INSURANCE -> R.string.domain_budget_item_icon_insurance
    BudgetItemIcon.CURRENCY_EXCHANGE -> R.string.domain_budget_item_icon_currency_exchange
    BudgetItemIcon.ELECTRONICS -> R.string.domain_budget_item_icon_electronics
    BudgetItemIcon.PHONE -> R.string.domain_budget_item_icon_phone
    BudgetItemIcon.BILLS -> R.string.domain_budget_item_icon_bills
    BudgetItemIcon.RENT -> R.string.domain_budget_item_icon_rent
    BudgetItemIcon.PIGGY_BANK -> R.string.domain_budget_item_icon_piggy_bank
    BudgetItemIcon.ELECTRICITY -> R.string.domain_budget_item_icon_electricity
    BudgetItemIcon.INTERNET -> R.string.domain_budget_item_icon_internet
    BudgetItemIcon.TV -> R.string.domain_budget_item_icon_tv
    BudgetItemIcon.MUSIC -> R.string.domain_budget_item_icon_music
    BudgetItemIcon.BARBER -> R.string.domain_budget_item_icon_barber
    BudgetItemIcon.GAMES -> R.string.domain_budget_item_icon_games
    BudgetItemIcon.SPORT -> R.string.domain_budget_item_icon_sport
    BudgetItemIcon.BOOKS -> R.string.domain_budget_item_icon_books
    BudgetItemIcon.REPAIR -> R.string.domain_budget_item_icon_repair
    BudgetItemIcon.HEALTH -> R.string.domain_budget_item_icon_health
    BudgetItemIcon.MEDICINE -> R.string.domain_budget_item_icon_medicine
    BudgetItemIcon.PET -> R.string.domain_budget_item_icon_pet
    BudgetItemIcon.MOVIE -> R.string.domain_budget_item_icon_movie
    BudgetItemIcon.KIDS -> R.string.domain_budget_item_icon_kids
    BudgetItemIcon.RETIREMENT -> R.string.domain_budget_item_icon_retirement
    BudgetItemIcon.PARKING -> R.string.domain_budget_item_icon_parking
    BudgetItemIcon.SHIP -> R.string.domain_budget_item_icon_ship
    BudgetItemIcon.SHIPMENT -> R.string.domain_budget_item_icon_shipment
    BudgetItemIcon.DOCTOR -> R.string.domain_budget_item_icon_doctor
}

@Composable
fun BudgetItemIcon.displayName(): String = stringResource(displayNameRes())
fun BudgetItemIcon.displayName(context: Context): String = context.getString(displayNameRes())
