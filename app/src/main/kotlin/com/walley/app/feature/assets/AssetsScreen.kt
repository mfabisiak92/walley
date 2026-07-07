package com.walley.app.feature.assets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.SwipeToDeleteBox
import com.walley.app.core.ui.WalleyTopBar
import com.walley.app.domain.model.Asset
import com.walley.app.domain.model.Liability
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(
    modifier: Modifier = Modifier,
    onNavigateHome: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        topBar = { WalleyTopBar(onTitleClick = onNavigateHome) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                listOf("Assets", "Liabilities").forEachIndexed { index, label ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(label) }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> AssetsListPage()
                    else -> LiabilitiesListPage()
                }
            }
        }
    }
}

@Composable
private fun AssetsListPage(viewModel: AssetsViewModel = hiltViewModel()) {
    val assets by viewModel.assets.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAsset by remember { mutableStateOf<Asset?>(null) }
    var pendingDeleteAsset by remember { mutableStateOf<Asset?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add asset")
            }
        }
    ) { innerPadding ->
        if (assets.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "No assets yet — tap + to add one.",
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
                items(assets, key = { it.id }) { asset ->
                    SwipeToDeleteBox(
                        onDelete = { pendingDeleteAsset = asset },
                        dismissOnDelete = false
                    ) {
                        AssetRow(asset = asset, onClick = { editingAsset = asset })
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddAssetDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, currency, purchaseValue, currentValue, purchaseDate ->
                viewModel.addAsset(name, currency, purchaseValue, currentValue, purchaseDate)
                showAddDialog = false
            }
        )
    }

    editingAsset?.let { asset ->
        EditAssetDialog(
            asset = asset,
            onDismiss = { editingAsset = null },
            onSave = { currentValue ->
                viewModel.updateCurrentValue(asset.id, currentValue)
                editingAsset = null
            },
            onDelete = {
                viewModel.deleteAsset(asset.id)
                editingAsset = null
            }
        )
    }

    pendingDeleteAsset?.let { asset ->
        AlertDialog(
            onDismissRequest = { pendingDeleteAsset = null },
            title = { Text("Delete asset?") },
            text = { Text("This will permanently delete \"${asset.name}\". This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAsset(asset.id)
                        pendingDeleteAsset = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteAsset = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AssetRow(asset: Asset, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(asset.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = asset.purchaseDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    formatMoney(asset.currentValue, asset.currency),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Purchased for ${formatMoney(asset.purchaseValue, asset.currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                GainLossText(asset, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun GainLossText(asset: Asset, modifier: Modifier = Modifier) {
    val gain = asset.gain
    val isGain = gain.signum() >= 0
    val color = if (isGain) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
    val sign = if (isGain) "+" else ""
    val percent = asset.gainPercent?.setScale(1, RoundingMode.HALF_UP)

    Text(
        text = "$sign${formatMoney(gain, asset.currency)}" +
            (percent?.let { " ($sign${it.toPlainString()}%)" } ?: ""),
        style = MaterialTheme.typography.bodySmall,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun LiabilitiesListPage(viewModel: LiabilitiesViewModel = hiltViewModel()) {
    val liabilities by viewModel.liabilities.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingLiability by remember { mutableStateOf<Liability?>(null) }
    var pendingDeleteLiability by remember { mutableStateOf<Liability?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add liability")
            }
        }
    ) { innerPadding ->
        if (liabilities.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.CreditCard,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "No liabilities yet — tap + to add one.",
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
                items(liabilities, key = { it.id }) { liability ->
                    SwipeToDeleteBox(
                        onDelete = { pendingDeleteLiability = liability },
                        dismissOnDelete = false
                    ) {
                        LiabilityRow(liability = liability, onClick = { editingLiability = liability })
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddLiabilityDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, currency, originalAmount, currentBalance, startDate ->
                viewModel.addLiability(name, currency, originalAmount, currentBalance, startDate)
                showAddDialog = false
            }
        )
    }

    editingLiability?.let { liability ->
        EditLiabilityDialog(
            liability = liability,
            onDismiss = { editingLiability = null },
            onSave = { currentBalance ->
                viewModel.updateCurrentBalance(liability.id, currentBalance)
                editingLiability = null
            },
            onDelete = {
                viewModel.deleteLiability(liability.id)
                editingLiability = null
            }
        )
    }

    pendingDeleteLiability?.let { liability ->
        AlertDialog(
            onDismissRequest = { pendingDeleteLiability = null },
            title = { Text("Delete liability?") },
            text = { Text("This will permanently delete \"${liability.name}\". This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteLiability(liability.id)
                        pendingDeleteLiability = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteLiability = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun LiabilityRow(liability: Liability, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(liability.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = "Since ${liability.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    formatMoney(liability.currentBalance, liability.currency),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(
                text = "Original: ${formatMoney(liability.originalAmount, liability.currency)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val payoffPercent = liability.paidOffPercent
            if (payoffPercent != null) {
                LiabilityPayoffProgress(liability = liability, payoffPercent = payoffPercent)
            }
        }
    }
}

@Composable
private fun LiabilityPayoffProgress(liability: Liability, payoffPercent: BigDecimal) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        LinearProgressIndicator(
            progress = { payoffPercent.divide(BigDecimal(100), 4, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Color(0xFF2E7D32)
        )
        Text(
            text = "${payoffPercent.setScale(0, RoundingMode.HALF_UP)}% paid off " +
                "(${formatMoney(liability.paidOffAmount, liability.currency)})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
