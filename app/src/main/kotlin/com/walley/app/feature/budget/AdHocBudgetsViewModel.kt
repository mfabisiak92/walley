package com.walley.app.feature.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AdHocBudgetRepository
import com.walley.app.domain.model.AdHocBudgetWithItems
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AdHocBudgetsViewModel @Inject constructor(
    private val repository: AdHocBudgetRepository
) : ViewModel() {

    val budgets: StateFlow<List<AdHocBudgetWithItems>> = repository.observeAdHocBudgetsWithItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteBudget(budgetId: Long) {
        viewModelScope.launch { repository.deleteAdHocBudget(budgetId) }
    }
}
