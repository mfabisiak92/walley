package com.walley.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.SettingsRepository
import com.walley.app.domain.model.Currency
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    val darkModeOverride: StateFlow<Boolean?> = repository.observeDarkModeOverride()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val baseCurrency: StateFlow<Currency> = repository.observeBaseCurrency()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.PLN)

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { repository.setDarkModeOverride(enabled) }
    }

    fun setBaseCurrency(currency: Currency) {
        viewModelScope.launch { repository.setBaseCurrency(currency) }
    }
}
