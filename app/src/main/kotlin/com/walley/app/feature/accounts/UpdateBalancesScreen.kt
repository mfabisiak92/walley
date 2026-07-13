package com.walley.app.feature.accounts

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountBalanceGroup
import java.math.BigDecimal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateBalancesScreen(
    onNavigateBack: () -> Unit,
    viewModel: UpdateBalancesViewModel = hiltViewModel()
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val balanceTexts = remember { mutableStateMapOf<Long, String>() }
    val scope = rememberCoroutineScope()
    val isInvestments = viewModel.group == AccountBalanceGroup.INVESTMENTS

    fun editableValue(account: Account): BigDecimal = if (isInvestments) account.uninvestedCash else account.balance

    accounts.forEach { account ->
        if (account.id !in balanceTexts) balanceTexts[account.id] = editableValue(account).toPlainString()
    }

    val parsedBalances = accounts.associate { it.id to balanceTexts[it.id]?.toBigDecimalOrNull() }
    val allValid = parsedBalances.values.all { it != null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Update balances") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    val changed = accounts.mapNotNull { account ->
                        val parsed = parsedBalances[account.id]
                        if (parsed != null && parsed.compareTo(editableValue(account)) != 0) {
                            account.id to parsed
                        } else {
                            null
                        }
                    }.toMap()
                    scope.launch {
                        viewModel.saveAll(changed)
                        onNavigateBack()
                    }
                },
                enabled = allValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) { Text("Save all") }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (isInvestments) {
                Text(
                    "Uninvested cash only — an account's total balance also includes the current market " +
                        "value of its linked investments, updated from the Investments tab.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                itemsIndexed(accounts, key = { _, account -> account.id }) { index, account ->
                    if (index > 0) HorizontalDivider()
                    UpdateBalanceRow(
                        account = account,
                        balanceText = balanceTexts[account.id].orEmpty(),
                        onBalanceChange = { balanceTexts[account.id] = it }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateBalanceRow(
    account: Account,
    balanceText: String,
    onBalanceChange: (String) -> Unit
) {
    val isValid = balanceText.toBigDecimalOrNull() != null
    val interactionSource = remember { MutableInteractionSource() }
    val textColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = account.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        BasicTextField(
            value = balanceText,
            onValueChange = onBalanceChange,
            modifier = Modifier.width(138.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            interactionSource = interactionSource,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
        ) { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = balanceText,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                isError = !isValid,
                suffix = { Text(account.currency.symbol, style = MaterialTheme.typography.bodySmall) },
                contentPadding = OutlinedTextFieldDefaults.contentPadding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 4.dp),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = true,
                        isError = !isValid,
                        interactionSource = interactionSource,
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                }
            )
        }
    }
}
