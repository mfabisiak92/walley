package com.walley.app.feature.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.BudgetRepository
import com.walley.app.domain.model.BudgetWithItems
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val repository: BudgetRepository
) : ViewModel() {

    val budgets: StateFlow<List<BudgetWithItems>> = repository.observeBudgetsWithItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { repository.checkAndAutoCompleteDueItems() }
    }
}
