package com.walley.app.feature.investments

import androidx.lifecycle.SavedStateHandle
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
class EquityDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: WatchedEquityRepository
) : ViewModel() {

    private val equityId: Long = checkNotNull(savedStateHandle["equityId"])

    val equityWithNotes: StateFlow<WatchedEquityWithNotes?> = repository.observeEquityWithNotes(equityId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun addNote(date: LocalDate, status: EquityStatus, note: String) {
        viewModelScope.launch { repository.addNote(equityId, date, status, note) }
    }

    fun updateNote(noteId: Long, date: LocalDate, status: EquityStatus, note: String) {
        viewModelScope.launch { repository.updateNote(noteId, date, status, note) }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch { repository.deleteNote(noteId) }
    }

    fun deleteEquity(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteEquity(equityId)
            onDeleted()
        }
    }
}
