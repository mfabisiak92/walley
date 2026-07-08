package com.walley.app.feature.budget

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.BudgetRepository
import com.walley.app.domain.model.BudgetStatus
import com.walley.app.domain.model.BudgetWithItems
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class BudgetSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val budgetId: Long = checkNotNull(savedStateHandle["budgetId"])

    val budget: StateFlow<BudgetWithItems?> = budgetRepository.observeBudget(budgetId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
}
