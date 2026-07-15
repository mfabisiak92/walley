package com.walley.app.core.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** A removable summary chip for one active sort/filter choice — tapping it clears just that one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemovableChip(label: String, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    InputChip(
        selected = true,
        onClick = onRemove,
        label = { Text(label) },
        modifier = modifier,
        trailingIcon = {
            Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
        }
    )
}
