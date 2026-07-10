package com.walley.app.feature.investments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.walley.app.domain.model.InvestmentWithTransactions

@Composable
fun LinkInvestmentsDialog(
    investments: List<InvestmentWithTransactions>,
    initiallyLinkedIds: Set<Long>,
    onDismiss: () -> Unit,
    onConfirm: (Set<Long>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(initiallyLinkedIds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link investments") },
        text = {
            if (investments.isEmpty()) {
                Text("No investments in your portfolio yet.")
            } else {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    investments.forEach { data ->
                        val investment = data.investment
                        val isSelected = investment.id in selectedIds
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedIds = if (isSelected) selectedIds - investment.id else selectedIds + investment.id
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        selectedIds = if (it) selectedIds + investment.id else selectedIds - investment.id
                                    }
                                )
                                Column {
                                    Text(investment.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        investment.ticker,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedIds) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
