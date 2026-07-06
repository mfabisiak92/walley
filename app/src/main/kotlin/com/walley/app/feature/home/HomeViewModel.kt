package com.walley.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.ExchangeRateRepository
import com.walley.app.data.repository.SettingsRepository
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.CurrencyTotal
import com.walley.app.domain.model.ExchangeRates
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeBalances(
    val total: List<CurrencyTotal> = emptyList(),
    val savings: List<CurrencyTotal> = emptyList()
)

data class NetWorthByCurrency(
    val currency: Currency,
    val amountInBaseCurrency: BigDecimal,
    val percent: BigDecimal
)

data class NetWorthState(
    val currency: Currency,
    // null when conversion is impossible (rates unavailable)
    val amount: BigDecimal?,
    // rate date shown when a conversion actually happened
    val rateDate: String?,
    // breakdown of net worth by the original currency of each account, converted to base currency
    val breakdown: List<NetWorthByCurrency> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    accountRepository: AccountRepository,
    settingsRepository: SettingsRepository,
    exchangeRateRepository: ExchangeRateRepository
) : ViewModel() {

    val homeBalances: StateFlow<HomeBalances> = accountRepository.observeAccounts()
        .map { accounts ->
            HomeBalances(
                total = currencyTotals(accounts),
                savings = currencyTotals(accounts.filter { it.type == AccountType.SAVING })
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeBalances())

    private val baseCurrencyRates = settingsRepository.observeBaseCurrency()
        .flatMapLatest { base ->
            exchangeRateRepository.observeRates(base).map { rates -> base to rates }
        }

    val netWorth: StateFlow<NetWorthState?> = combine(
        accountRepository.observeAccounts(),
        baseCurrencyRates
    ) { accounts, (base, rates) ->
        if (accounts.isEmpty()) null else computeNetWorth(accounts, base, rates)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun computeNetWorth(accounts: List<Account>, base: Currency, rates: ExchangeRates?): NetWorthState {
        val byCurrency = linkedMapOf<Currency, BigDecimal>()
        var usedRates = false
        for (account in accounts) {
            val amountInBase = if (account.currency == base) {
                account.balance
            } else {
                val rate = rates?.rates?.get(account.currency)
                    ?: return NetWorthState(currency = base, amount = null, rateDate = null)
                // rate is base -> account currency, so convert back by dividing
                usedRates = true
                account.balance.divide(rate, 10, RoundingMode.HALF_UP)
            }
            byCurrency[account.currency] = (byCurrency[account.currency] ?: BigDecimal.ZERO) + amountInBase
        }
        val total = byCurrency.values.fold(BigDecimal.ZERO) { acc, value -> acc + value }
        val breakdown = byCurrency.entries
            .filter { it.value.signum() > 0 }
            .map { (currency, amount) ->
                val percent = if (total.signum() == 0) {
                    BigDecimal.ZERO
                } else {
                    amount.divide(total, 6, RoundingMode.HALF_UP) * BigDecimal(100)
                }
                NetWorthByCurrency(currency, amount.setScale(2, RoundingMode.HALF_UP), percent)
            }
            .sortedByDescending { it.amountInBaseCurrency }
        return NetWorthState(
            currency = base,
            amount = total.setScale(2, RoundingMode.HALF_UP),
            rateDate = if (usedRates) rates?.date else null,
            breakdown = breakdown
        )
    }

    private fun currencyTotals(accounts: List<Account>): List<CurrencyTotal> =
        Currency.entries.mapNotNull { currency ->
            val total = accounts.filter { it.currency == currency }.sumOf { it.balance }
            if (total.signum() == 0) null else CurrencyTotal(currency, total)
        }
}
