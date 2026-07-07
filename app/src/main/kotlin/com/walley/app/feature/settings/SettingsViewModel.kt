package com.walley.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.SecurityRepository
import com.walley.app.data.repository.SettingsRepository
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.Currency
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val securityRepository: SecurityRepository
) : ViewModel() {

    val darkModeOverride: StateFlow<Boolean?> = repository.observeDarkModeOverride()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val baseCurrency: StateFlow<Currency> = repository.observeBaseCurrency()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.PLN)

    val fingerprintUnlock: StateFlow<Boolean> = securityRepository.observeFingerprintUnlock()
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

    fun setFingerprintUnlock(enabled: Boolean) {
        viewModelScope.launch { securityRepository.setFingerprintUnlock(enabled) }
    }

    fun setCategoryTarget(section: BudgetSectionType, percent: BigDecimal?) {
        viewModelScope.launch { repository.setCategoryTarget(section, percent) }
    }
}
