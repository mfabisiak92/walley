package com.walley.app.feature.settings

import androidx.biometric.BiometricManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.ui.FieldHint
import com.walley.app.core.ui.WalleyTopBar
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.CATEGORY_TARGET_SECTIONS
import com.walley.app.domain.model.Currency
import java.math.BigDecimal
import kotlinx.coroutines.launch

private val TABS = listOf("General", "Budget")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateHome: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { TABS.size })
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier,
        topBar = { WalleyTopBar(onTitleClick = onNavigateHome) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                TABS.forEachIndexed { index, label ->
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
                    0 -> GeneralSettingsPage(viewModel, snackbarHostState)
                    else -> BudgetSettingsPage(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeneralSettingsPage(viewModel: SettingsViewModel, snackbarHostState: SnackbarHostState) {
    val darkModeOverride by viewModel.darkModeOverride.collectAsStateWithLifecycle()
    val baseCurrency by viewModel.baseCurrency.collectAsStateWithLifecycle()
    val includeSavingsInNetWorth by viewModel.includeSavingsInNetWorth.collectAsStateWithLifecycle()
    val fingerprintUnlock by viewModel.fingerprintUnlock.collectAsStateWithLifecycle()
    val changePinError by viewModel.changePinError.collectAsStateWithLifecycle()
    val changePinSuccess by viewModel.changePinSuccess.collectAsStateWithLifecycle()
    val isDarkMode = darkModeOverride ?: isSystemInDarkTheme()
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val biometricsAvailable = remember {
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }

    LaunchedEffect(changePinSuccess) {
        if (changePinSuccess) {
            showChangePinDialog = false
            scope.launch { snackbarHostState.showSnackbar("PIN changed") }
            viewModel.dismissChangePinResult()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Dark mode", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = isDarkMode, onCheckedChange = viewModel::setDarkMode)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Fingerprint unlock", style = MaterialTheme.typography.bodyLarge)
                if (!biometricsAvailable) {
                    Text(
                        "No fingerprint enrolled on this device",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = fingerprintUnlock && biometricsAvailable,
                enabled = biometricsAvailable,
                onCheckedChange = viewModel::setFingerprintUnlock
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Base currency", style = MaterialTheme.typography.bodyLarge)
            ExposedDropdownMenuBox(
                expanded = currencyMenuExpanded,
                onExpandedChange = { currencyMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = baseCurrency.name,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyMenuExpanded) },
                    modifier = Modifier
                        .width(140.dp)
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = currencyMenuExpanded,
                    onDismissRequest = { currencyMenuExpanded = false }
                ) {
                    Currency.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.name) },
                            onClick = {
                                viewModel.setBaseCurrency(option)
                                currencyMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f).padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Include savings in net worth", style = MaterialTheme.typography.bodyLarge)
                FieldHint(
                    "Virtual Saving accounts are always excluded, since their balance already " +
                        "sits in another account."
                )
            }
            Switch(checked = includeSavingsInNetWorth, onCheckedChange = viewModel::setIncludeSavingsInNetWorth)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PIN", style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = { showChangePinDialog = true }) {
                Text("Change PIN")
            }
        }
    }

    if (showChangePinDialog) {
        ChangePinDialog(
            errorMessage = changePinError,
            onCurrentPinChanged = viewModel::dismissChangePinResult,
            onDismiss = {
                showChangePinDialog = false
                viewModel.dismissChangePinResult()
            },
            onConfirm = { currentPin, newPin -> viewModel.changePin(currentPin, newPin) }
        )
    }
}

@Composable
private fun BudgetSettingsPage(viewModel: SettingsViewModel) {
    val categoryTargets by viewModel.categoryTargets.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Set a target % of disposable income for each category. Fixed/Other costs and Savings " +
                "warn you if you go over; Investments shows a checkmark once you reach the goal.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        CATEGORY_TARGET_SECTIONS.forEach { section ->
            CategoryTargetField(
                section = section,
                value = categoryTargets[section],
                onValueChange = { percent -> viewModel.setCategoryTarget(section, percent) }
            )
        }
    }
}

@Composable
private fun CategoryTargetField(
    section: BudgetSectionType,
    value: BigDecimal?,
    onValueChange: (BigDecimal?) -> Unit
) {
    // Not re-keyed on `value` — this field is the source of truth while typing; re-keying would
    // reset the text mid-edit once the debounced persistence round-trips back through the flow.
    var text by remember { mutableStateOf(value?.toPlainString() ?: "") }
    val parsed = text.toBigDecimalOrNull()
    val isError = text.isNotBlank() && parsed == null

    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            text = newText
            val newParsed = newText.toBigDecimalOrNull()
            if (newText.isBlank()) {
                onValueChange(null)
            } else if (newParsed != null) {
                onValueChange(newParsed)
            }
        },
        label = { Text("${section.label} target %") },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}
