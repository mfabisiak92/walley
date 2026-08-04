package com.walley.app.feature.investments

import com.walley.app.core.format.toBigDecimalOrNullLenient
import com.walley.app.R
import androidx.compose.ui.res.stringResource
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

/** A price not refreshed within this many days is flagged as stale in the UI. */
private const val STALE_PRICE_THRESHOLD_DAYS = 3L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatePricesScreen(
    onNavigateBack: () -> Unit,
    viewModel: UpdatePricesViewModel = hiltViewModel()
) {
    val investments by viewModel.investments.collectAsStateWithLifecycle()
    val refreshingIds by viewModel.refreshingIds.collectAsStateWithLifecycle()
    val failedRefreshReasons by viewModel.failedRefreshReasons.collectAsStateWithLifecycle()
    val marketDataConfigured by viewModel.marketDataConfigured.collectAsStateWithLifecycle()
    val priceTexts = remember { mutableStateMapOf<Long, String>() }
    // Tracks the currentPrice each text field was last seeded from, so a market refresh (which
    // changes currentPrice in the DB) overwrites the field, while an unsaved manual edit (which
    // doesn't) is left alone.
    val seededPrices = remember { mutableStateMapOf<Long, BigDecimal>() }
    val scope = rememberCoroutineScope()

    investments.forEach { investment ->
        val seeded = seededPrices[investment.id]
        if (seeded == null || seeded.compareTo(investment.currentPrice) != 0) {
            priceTexts[investment.id] = investment.currentPrice.toPlainString()
            seededPrices[investment.id] = investment.currentPrice
        }
    }

    val parsedPrices = investments.associate { it.id to priceTexts[it.id]?.toBigDecimalOrNullLenient() }
    val allValid = parsedPrices.values.all { it != null && it.signum() > 0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.investments_update_prices_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (refreshingIds.isNotEmpty()) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 12.dp))
                    } else if (marketDataConfigured) {
                        IconButton(onClick = { viewModel.refreshFromMarket() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh all prices from market")
                        }
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
            ) { Text(stringResource(R.string.investments_action_save_all)) }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            if (!marketDataConfigured) {
                item {
                    Text(
                        "Enable Yahoo Finance integration in Settings to refresh prices automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
            itemsIndexed(investments, key = { _, investment -> investment.id }) { index, investment ->
                if (index > 0) HorizontalDivider()
                UpdatePriceRow(
                    investment = investment,
                    priceText = priceTexts[investment.id].orEmpty(),
                    onPriceChange = { priceTexts[investment.id] = it },
                    priceNotFoundReason = failedRefreshReasons[investment.id],
                    showRefreshButton = marketDataConfigured,
                    isRefreshingThis = investment.id in refreshingIds,
                    refreshDisabled = refreshingIds.isNotEmpty(),
                    onRefresh = { viewModel.refreshOne(investment.id) }
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
    onPriceChange: (String) -> Unit,
    priceNotFoundReason: String?,
    showRefreshButton: Boolean,
    isRefreshingThis: Boolean,
    refreshDisabled: Boolean,
    onRefresh: () -> Unit
) {
    val parsed = priceText.toBigDecimalOrNullLenient()
    val isValid = parsed?.let { it.signum() > 0 } == true
    val hasChanged = parsed != null && parsed.compareTo(investment.currentPrice) != 0
    val isIncrease = hasChanged && parsed!!.compareTo(investment.currentPrice) > 0
    val interactionSource = remember { MutableInteractionSource() }
    val textColor = MaterialTheme.colorScheme.onSurface
    val daysSinceUpdate = investment.lastPriceUpdate?.let { ChronoUnit.DAYS.between(it, LocalDate.now()) }
    val isStale = daysSinceUpdate == null || daysSinceUpdate >= STALE_PRICE_THRESHOLD_DAYS

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
            if (showRefreshButton) {
                IconButton(onClick = onRefresh, enabled = !refreshDisabled, modifier = Modifier.size(32.dp)) {
                    if (isRefreshingThis) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Refresh price for ${investment.ticker}",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
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
        if (priceNotFoundReason != null || isStale) {
            val warningColor = MaterialTheme.colorScheme.error
            val message = when {
                priceNotFoundReason != null -> "${investment.ticker}: $priceNotFoundReason"
                daysSinceUpdate == null -> "Price never updated"
                else -> "Not updated in $daysSinceUpdate day${if (daysSinceUpdate == 1L) "" else "s"}"
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = "Warning",
                    modifier = Modifier.size(14.dp),
                    tint = warningColor
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelSmall,
                    color = warningColor,
                    modifier = Modifier.padding(start = 4.dp)
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
