package com.walley.app.feature.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.IntegrationsRepository
import com.walley.app.data.repository.SecurityRepository
import com.walley.app.data.repository.SettingsRepository
import com.walley.app.domain.model.AppLanguage
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.Currency
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val securityRepository: SecurityRepository,
    private val integrationsRepository: IntegrationsRepository
) : ViewModel() {

    val darkModeOverride: StateFlow<Boolean?> = repository.observeDarkModeOverride()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val baseCurrency: StateFlow<Currency> = repository.observeBaseCurrency()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.PLN)

    val includeSavingsInNetWorth: StateFlow<Boolean> = repository.observeIncludeSavingsInNetWorth()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    // AppCompatDelegate persists the per-app locale itself (its own store, applied automatically
    // at each Activity's attachBaseContext), so this mirrors its current value rather than going
    // through SettingsDataStore.
    private val _language = MutableStateFlow(currentAppLanguage())
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    val fingerprintUnlock: StateFlow<Boolean> = securityRepository.observeFingerprintUnlock()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val yahooFinanceEnabled: StateFlow<Boolean> = integrationsRepository.observeYahooFinanceEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val categoryTargets: StateFlow<Map<BudgetSectionType, BigDecimal?>> = combine(
        repository.observeCategoryTarget(BudgetSectionType.FIXED_COSTS),
        repository.observeCategoryTarget(BudgetSectionType.OTHER_COSTS),
        repository.observeCategoryTarget(BudgetSectionType.SAVINGS),
        repository.observeCategoryTarget(BudgetSectionType.INVESTMENTS)
    ) { fixed, other, savings, investments ->
        mapOf(
            BudgetSectionType.FIXED_COSTS to fixed,
            BudgetSectionType.OTHER_COSTS to other,
            BudgetSectionType.SAVINGS to savings,
            BudgetSectionType.INVESTMENTS to investments
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { repository.setDarkModeOverride(enabled) }
    }

    fun setBaseCurrency(currency: Currency) {
        viewModelScope.launch { repository.setBaseCurrency(currency) }
    }

    fun setIncludeSavingsInNetWorth(enabled: Boolean) {
        viewModelScope.launch { repository.setIncludeSavingsInNetWorth(enabled) }
    }

    fun setLanguage(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(
            language.tag?.let { LocaleListCompat.forLanguageTags(it) } ?: LocaleListCompat.getEmptyLocaleList()
        )
        _language.value = language
    }

    private fun currentAppLanguage(): AppLanguage {
        val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        return AppLanguage.entries.find { it.tag == tag } ?: AppLanguage.SYSTEM
    }

    fun setFingerprintUnlock(enabled: Boolean) {
        viewModelScope.launch { securityRepository.setFingerprintUnlock(enabled) }
    }

    fun setYahooFinanceEnabled(enabled: Boolean) {
        viewModelScope.launch { integrationsRepository.setYahooFinanceEnabled(enabled) }
    }

    fun setCategoryTarget(section: BudgetSectionType, percent: BigDecimal?) {
        viewModelScope.launch { repository.setCategoryTarget(section, percent) }
    }

    private val _changePinError = MutableStateFlow(false)
    val changePinError: StateFlow<Boolean> = _changePinError.asStateFlow()

    private val _changePinSuccess = MutableStateFlow(false)
    val changePinSuccess: StateFlow<Boolean> = _changePinSuccess.asStateFlow()

    fun changePin(currentPin: String, newPin: String) {
        viewModelScope.launch {
            if (!securityRepository.verifyPin(currentPin)) {
                _changePinError.value = true
                return@launch
            }
            securityRepository.setPin(newPin)
            _changePinError.value = false
            _changePinSuccess.value = true
        }
    }

    fun dismissChangePinResult() {
        _changePinError.value = false
        _changePinSuccess.value = false
    }
}
