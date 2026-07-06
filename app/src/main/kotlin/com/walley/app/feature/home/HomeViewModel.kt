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
    val available: List<CurrencyTotal> = emptyList(),
    val savings: List<CurrencyTotal> = emptyList()
)

data class NetWorthState(
    val currency: Currency,
    // null when conversion is impossible (rates unavailable)
    val amount: BigDecimal?,
    // rate date shown when a conversion actually happened
    val rateDate: String?
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
                available = currencyTotals(accounts.filter { it.type != AccountType.SAVING }),
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
        var total = BigDecimal.ZERO
        var usedRates = false
        for (account in accounts) {
            if (account.currency == base) {
                total += account.balance
            } else {
                val rate = rates?.rates?.get(account.currency)
                    ?: return NetWorthState(currency = base, amount = null, rateDate = null)
                // rate is base -> account currency, so convert back by dividing
                total += account.balance.divide(rate, 10, RoundingMode.HALF_UP)
                usedRates = true
            }
        }
        return NetWorthState(
            currency = base,
            amount = total.setScale(2, RoundingMode.HALF_UP),
            rateDate = if (usedRates) rates?.date else null
        )
    }

    private fun currencyTotals(accounts: List<Account>): List<CurrencyTotal> =
        Currency.entries.mapNotNull { currency ->
            val total = accounts.filter { it.currency == currency }.sumOf { it.balance }
            if (total.signum() == 0) null else CurrencyTotal(currency, total)
        }
}
