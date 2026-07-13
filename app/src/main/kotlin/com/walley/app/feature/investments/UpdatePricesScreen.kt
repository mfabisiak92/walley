package com.walley.app.feature.investments

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.format.formatMoney
import com.walley.app.domain.model.Investment
import java.math.BigDecimal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatePricesScreen(
    onNavigateBack: () -> Unit,
    viewModel: UpdatePricesViewModel = hiltViewModel()
) {
    val investments by viewModel.investments.collectAsStateWithLifecycle()
    val priceTexts = remember { mutableStateMapOf<Long, String>() }
    val scope = rememberCoroutineScope()

    investments.forEach { investment ->
        if (investment.id !in priceTexts) priceTexts[investment.id] = investment.currentPrice.toPlainString()
    }

    val parsedPrices = investments.associate { it.id to priceTexts[it.id]?.toBigDecimalOrNull() }
    val allValid = parsedPrices.values.all { it != null && it.signum() > 0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Update prices") },
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
                    val changed = investments.mapNotNull { investment ->
                        val parsed = parsedPrices[investment.id]
                        if (parsed != null && parsed.compareTo(investment.currentPrice) != 0) {
                            investment.id to parsed
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
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            itemsIndexed(investments, key = { _, investment -> investment.id }) { index, investment ->
                if (index > 0) HorizontalDivider()
                UpdatePriceRow(
                    investment = investment,
                    priceText = priceTexts[investment.id].orEmpty(),
                    onPriceChange = { priceTexts[investment.id] = it }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdatePriceRow(
    investment: Investment,
    priceText: String,
    onPriceChange: (String) -> Unit
) {
    val parsed = priceText.toBigDecimalOrNull()
    val isValid = parsed?.let { it.signum() > 0 } == true
    val hasChanged = parsed != null && parsed.compareTo(investment.currentPrice) != 0
    val isIncrease = hasChanged && parsed!!.compareTo(investment.currentPrice) > 0
    val interactionSource = remember { MutableInteractionSource() }
    val textColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${investment.name} · ${investment.ticker}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            BasicTextField(
                value = priceText,
                onValueChange = onPriceChange,
                modifier = Modifier.width(138.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                interactionSource = interactionSource,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
            ) { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = priceText,
                    innerTextField = innerTextField,
                    enabled = true,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    isError = !isValid,
                    suffix = { Text(investment.currency.symbol, style = MaterialTheme.typography.bodySmall) },
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
        if (hasChanged) {
            val changeColor = if (isIncrease) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isIncrease) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                    contentDescription = if (isIncrease) "Increased" else "Decreased",
                    modifier = Modifier.size(14.dp),
                    tint = changeColor
                )
                Text(
                    text = "was ${formatMoney(investment.currentPrice, investment.currency)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = changeColor,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}
