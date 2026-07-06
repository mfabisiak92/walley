package com.walley.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountRepository
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.CurrencyTotal
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeBalances(
    val total: List<CurrencyTotal> = emptyList(),
    val available: List<CurrencyTotal> = emptyList(),
    val savings: List<CurrencyTotal> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: AccountRepository
) : ViewModel() {

    val homeBalances: StateFlow<HomeBalances> = repository.observeAccounts()
        .map { accounts ->
            HomeBalances(
                total = currencyTotals(accounts),
                available = currencyTotals(accounts.filter { it.type != AccountType.SAVING }),
                savings = currencyTotals(accounts.filter { it.type == AccountType.SAVING })
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeBalances())

    private fun currencyTotals(accounts: List<Account>): List<CurrencyTotal> =
        Currency.entries.mapNotNull { currency ->
            val total = accounts.filter { it.currency == currency }.sumOf { it.balance }
            if (total.signum() == 0) null else CurrencyTotal(currency, total)
        }
}
