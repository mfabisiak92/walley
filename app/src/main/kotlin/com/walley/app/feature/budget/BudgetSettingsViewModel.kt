package com.walley.app.feature.budget

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.BudgetRepository
import com.walley.app.data.repository.SettingsRepository
import com.walley.app.domain.model.BudgetStatus
import com.walley.app.domain.model.BudgetWithItems
import com.walley.app.domain.model.Currency
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class BudgetSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val budgetRepository: BudgetRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val budgetId: Long = checkNotNull(savedStateHandle["budgetId"])

    val budget: StateFlow<BudgetWithItems?> = budgetRepository.observeBudget(budgetId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val baseCurrency: StateFlow<Currency> = settingsRepository.observeBaseCurrency()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.PLN)

    private val isEditable: Boolean get() = budget.value?.budget?.status != BudgetStatus.COMPLETED

    fun updateIncomeAccountEffects(enabled: Boolean) {
        if (!isEditable) return
        viewModelScope.launch { budgetRepository.updateApplyIncomeAccountEffects(budgetId, enabled) }
    }

    fun updateCostsAccountEffects(enabled: Boolean) {
        if (!isEditable) return
        viewModelScope.launch { budgetRepository.updateApplyCostsAccountEffects(budgetId, enabled) }
    }

    fun updateSavingsAccountEffects(enabled: Boolean) {
        if (!isEditable) return
        viewModelScope.launch { budgetRepository.updateApplySavingsAccountEffects(budgetId, enabled) }
    }

    fun updateInvestmentsAccountEffects(enabled: Boolean) {
        if (!isEditable) return
        viewModelScope.launch { budgetRepository.updateApplyInvestmentsAccountEffects(budgetId, enabled) }
    }

    /** Unlike the toggles above, always allowed — even for a completed budget, since it's just a goal figure, not something that moves money. */
    fun updatePlannedNetWorth(plannedNetWorth: BigDecimal) {
        viewModelScope.launch { budgetRepository.updatePlannedNetWorth(budgetId, plannedNetWorth) }
    }
}
