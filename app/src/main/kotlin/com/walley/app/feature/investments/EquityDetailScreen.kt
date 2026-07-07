package com.walley.app.feature.investments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.ui.EquityStatusBadge
import com.walley.app.core.ui.SwipeToDeleteBox
import com.walley.app.domain.model.EquityNote
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquityDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: EquityDetailViewModel = hiltViewModel()
) {
    val equityWithNotes by viewModel.equityWithNotes.collectAsStateWithLifecycle()
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<EquityNote?>(null) }
    var pendingDeleteNote by remember { mutableStateOf<EquityNote?>(null) }
    var showDeleteEquityConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val equity = equityWithNotes?.equity
                    Text(
                        equity?.let { listOfNotNull(it.name, it.ticker).joinToString(" · ") } ?: "Equity"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteEquityConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete equity")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddNoteDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add note")
            }
        }
    ) { innerPadding ->
        val notes = equityWithNotes?.notes.orEmpty()
        if (notes.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "No notes yet — tap + to add one.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    SwipeToDeleteBox(
                        onDelete = { pendingDeleteNote = note },
                        dismissOnDelete = false
                    ) {
                        NoteRow(note = note, onClick = { editingNote = note })
                    }
                }
            }
        }
    }

    if (showAddNoteDialog || editingNote != null) {
        val initial = editingNote
        AddEquityNoteDialog(
            initial = initial,
            onDismiss = {
                showAddNoteDialog = false
                editingNote = null
            },
            onConfirm = { date, status, note ->
                if (initial != null) {
                    viewModel.updateNote(initial.id, date, status, note)
                } else {
                    viewModel.addNote(date, status, note)
                }
                showAddNoteDialog = false
                editingNote = null
            }
        )
    }

    pendingDeleteNote?.let { note ->
        AlertDialog(
            onDismissRequest = { pendingDeleteNote = null },
            title = { Text("Delete note?") },
            text = { Text("This will permanently delete this note. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteNote(note.id)
                        pendingDeleteNote = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteNote = null }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteEquityConfirm) {
        val equityName = equityWithNotes?.equity?.name ?: "this equity"
        AlertDialog(
            onDismissRequest = { showDeleteEquityConfirm = false },
            title = { Text("Delete equity?") },
            text = { Text("This will permanently delete \"$equityName\" and all its notes. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteEquityConfirm = false
                        viewModel.deleteEquity(onNavigateBack)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteEquityConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun NoteRow(note: EquityNote, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    note.date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                EquityStatusBadge(status = note.status, modifier = Modifier.padding(start = 8.dp))
            }
            if (note.note.isNotBlank()) {
                Text(note.note, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
