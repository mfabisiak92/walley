package com.walley.app.feature.investments

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.csv.decodeCsvBytes
import com.walley.app.data.csv.looksLikeBossaExport
import com.walley.app.data.csv.looksLikeXtbCashOperationsExport
import com.walley.app.data.csv.parseBossaExportCsv
import com.walley.app.data.csv.parseInvestmentImportCsv
import com.walley.app.data.csv.parseXtbCashOperationsCsv
import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.InvestmentRepository
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.CsvRowParseResult
import com.walley.app.domain.model.ImportRowOutcome
import com.walley.app.domain.model.ImportRowStatus
import com.walley.app.domain.model.InvestmentWithTransactions
import com.walley.app.domain.model.validateImportRows
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface ImportUiState {
    data object Loading : ImportUiState
    data class Error(val message: String) : ImportUiState
    /** BOSSA/XTB (and similar) exports don't carry an account column — the whole file is one account's history. */
    data class SelectAccount(val accounts: List<Account>) : ImportUiState
    data class Preview(val outcomes: List<ImportRowOutcome>) : ImportUiState
    data object Committing : ImportUiState
    data class Done(val importedCount: Int) : ImportUiState
}

/** File formats that don't carry their own account column, so the user picks one account for the whole file. */
private enum class SingleAccountFormat { BOSSA, XTB }

@HiltViewModel
class ImportInvestmentsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountRepository: AccountRepository,
    private val investmentRepository: InvestmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImportUiState>(ImportUiState.Loading)
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    private var pendingText: String? = null
    private var pendingFormat: SingleAccountFormat? = null

    fun load(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ImportUiState.Loading
            val text = try {
                context.contentResolver.openInputStream(uri)?.use { decodeCsvBytes(it.readBytes()) }
            } catch (e: Exception) {
                null
            }
            if (text.isNullOrBlank()) {
                _uiState.value = ImportUiState.Error("Couldn't read that file. Make sure it's a CSV file and try again.")
                return@launch
            }

            val singleAccountFormat = when {
                looksLikeBossaExport(text) -> SingleAccountFormat.BOSSA
                looksLikeXtbCashOperationsExport(text) -> SingleAccountFormat.XTB
                else -> null
            }
            if (singleAccountFormat != null) {
                val investmentAccounts = accountRepository.observeAccounts().first()
                    .filter { it.type == AccountType.INVESTMENT }
                if (investmentAccounts.isEmpty()) {
                    _uiState.value = ImportUiState.Error("Create an investment account first, then import into it.")
                    return@launch
                }
                pendingText = text
                pendingFormat = singleAccountFormat
                _uiState.value = ImportUiState.SelectAccount(investmentAccounts)
                return@launch
            }

            val accounts = accountRepository.observeAccounts().first()
            val parseResults = parseInvestmentImportCsv(text, accounts)
            if (parseResults.isEmpty()) {
                _uiState.value = ImportUiState.Error("That file has no rows to import.")
                return@launch
            }
            validateAndShowPreview(parseResults)
        }
    }

    fun selectAccountForImport(account: Account) {
        val text = pendingText ?: return
        val format = pendingFormat ?: return
        viewModelScope.launch {
            _uiState.value = ImportUiState.Loading
            val parseResults = when (format) {
                SingleAccountFormat.BOSSA -> parseBossaExportCsv(text, accountId = account.id, accountName = account.name)
                SingleAccountFormat.XTB -> parseXtbCashOperationsCsv(text, accountId = account.id, accountName = account.name)
            }
            validateAndShowPreview(parseResults)
        }
    }

    private suspend fun validateAndShowPreview(parseResults: List<CsvRowParseResult>) {
        val accounts = accountRepository.observeAccounts().first()
        val investmentsByAccount: Map<Long, List<InvestmentWithTransactions>> = investmentRepository.observeInvestments().first()
            .filter { it.investment.accountId != null }
            .groupBy { it.investment.accountId!! }
        val outcomes = validateImportRows(parseResults, accounts, investmentsByAccount)
        _uiState.value = ImportUiState.Preview(outcomes)
    }

    fun confirmImport() {
        val state = _uiState.value
        if (state !is ImportUiState.Preview) return
        val toImport = state.outcomes.filter { it.status is ImportRowStatus.ToImport }

        viewModelScope.launch {
            _uiState.value = ImportUiState.Committing
            val accountsById = accountRepository.observeAccounts().first().associateBy { it.id }
            toImport.forEach { outcome ->
                val row = checkNotNull(outcome.row)
                val currency = accountsById.getValue(row.accountId).currency
                investmentRepository.importTransaction(
                    accountId = row.accountId,
                    ticker = row.ticker,
                    name = row.name,
                    category = row.category,
                    currency = currency,
                    type = row.type,
                    date = row.date,
                    quantity = row.quantity,
                    pricePerUnit = row.price,
                    commission = row.commission
                )
            }
            _uiState.value = ImportUiState.Done(toImport.size)
        }
    }
}
