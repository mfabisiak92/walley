package com.walley.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountRepository
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.CurrencyTotal
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: AccountRepository
) : ViewModel() {

    val currencyTotals: StateFlow<List<CurrencyTotal>> = repository.observeAccounts()
        .map { accounts ->
            Currency.entries.mapNotNull { currency ->
                val total = accounts.filter { it.currency == currency }.sumOf { it.balance }
                if (total.signum() == 0) null else CurrencyTotal(currency, total)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
