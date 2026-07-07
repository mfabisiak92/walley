package com.walley.app.feature.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.WatchedEquityRepository
import com.walley.app.domain.model.EquityStatus
import com.walley.app.domain.model.WatchedEquityWithNotes
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class StrategiesViewModel @Inject constructor(
    private val repository: WatchedEquityRepository
) : ViewModel() {

    val equities: StateFlow<List<WatchedEquityWithNotes>> = repository.observeEquitiesWithNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addEquity(name: String, ticker: String?, date: LocalDate, status: EquityStatus, note: String) {
        viewModelScope.launch { repository.addEquity(name, ticker, date, status, note) }
    }

    fun deleteEquity(equityId: Long) {
        viewModelScope.launch { repository.deleteEquity(equityId) }
    }
}
