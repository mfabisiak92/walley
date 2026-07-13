package com.walley.app.feature.investments

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.csv.decodeCsvBytes
import com.walley.app.data.csv.looksLikeBossaCashOperationsExport
import com.walley.app.data.csv.looksLikeBossaExport
import com.walley.app.data.csv.looksLikeObligacjeSkarboweExport
import com.walley.app.data.csv.looksLikeXtbCashOperationsExport
import com.walley.app.data.csv.parseBossaCashOperationsCsv
import com.walley.app.data.csv.parseBossaExportCsv
import com.walley.app.data.csv.parseInvestmentImportCsv
import com.walley.app.data.csv.parseObligacjeSkarboweCsv
import com.walley.app.data.csv.parseXtbCashOperationsCsv
import com.walley.app.data.repository.AccountOperationRepository
import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.InvestmentRepository
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.CsvRowParseResult
import com.walley.app.domain.model.ImportRowOutcome
import com.walley.app.domain.model.ImportRowStatus
import com.walley.app.domain.model.InvestmentTransactionType
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

/**
 * Which (if any) toggle [ImportUiState.SelectAccount] should offer for a given format, since the same
 * on/off switch means something different depending on what the source file actually contains.
 */
enum class ImportToggleKind {
    /** Plain BOSSA: no cash rows in the file, nothing to offer. */
    NONE,
    /** BOSSA cash-operations export / XTB: file has real deposit/transfer/interest rows to import as cash operations. */
    INCLUDE_ACCOUNT_OPERATIONS,
    /** Obligacje Skarbowe: file has no deposit history at all, so cash-sufficiency checks on buys can only ever be wrong. */
    IGNORE_ACCOUNT_BALANCE
}

sealed interface ImportUiState {
    data object Loading : ImportUiState
    data class Error(val message: String) : ImportUiState
    /** BOSSA/XTB (and similar) exports don't carry an account column — the whole file is one account's history. */
    data class SelectAccount(
        val accounts: List<Account>,
        val toggleKind: ImportToggleKind
    ) : ImportUiState
    data class Preview(val outcomes: List<ImportRowOutcome>) : ImportUiState
    data object Committing : ImportUiState
    data class Done(val importedCount: Int) : ImportUiState
}

/** File formats that don't carry their own account column, so the user picks one account for the whole file. */
private enum class SingleAccountFormat { BOSSA, BOSSA_CASH_OPERATIONS, XTB, OBLIGACJE_SKARBOWE }

@HiltViewModel
class ImportInvestmentsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountRepository: AccountRepository,
    private val investmentRepository: InvestmentRepository,
    private val accountOperationRepository: AccountOperationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImportUiState>(ImportUiState.Loading)
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    private var pendingText: String? = null
    private var pendingFormat: SingleAccountFormat? = null

    /**
     * True when the user turned on "include account operations" for this import: besides the cash
     * operations themselves, every trade's cost/proceeds (including commission) is also debited/credited
     * against the account's balance on commit, so the ending balance matches the source statement
     * instead of only reflecting deposits/interest/transfers. Never set for [SingleAccountFormat.OBLIGACJE_SKARBOWE]
     * — its toggle means "ignore the balance", not "apply it", so its trades never touch the balance.
     */
    private var pendingIncludeAccountOperations = false

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
                looksLikeBossaCashOperationsExport(text) -> SingleAccountFormat.BOSSA_CASH_OPERATIONS
                looksLikeXtbCashOperationsExport(text) -> SingleAccountFormat.XTB
                looksLikeObligacjeSkarboweExport(text) -> SingleAccountFormat.OBLIGACJE_SKARBOWE
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
                val toggleKind = when (singleAccountFormat) {
                    SingleAccountFormat.BOSSA -> ImportToggleKind.NONE
                    SingleAccountFormat.BOSSA_CASH_OPERATIONS, SingleAccountFormat.XTB -> ImportToggleKind.INCLUDE_ACCOUNT_OPERATIONS
                    SingleAccountFormat.OBLIGACJE_SKARBOWE -> ImportToggleKind.IGNORE_ACCOUNT_BALANCE
                }
                _uiState.value = ImportUiState.SelectAccount(accounts = investmentAccounts, toggleKind = toggleKind)
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

    /** [toggleValue] is whatever the [ImportToggleKind] shown for [pendingFormat] means — see its enum entries. */
    fun selectAccountForImport(account: Account, toggleValue: Boolean = false) {
        val text = pendingText ?: return
        val format = pendingFormat ?: return
        pendingIncludeAccountOperations = toggleValue && format != SingleAccountFormat.OBLIGACJE_SKARBOWE
        viewModelScope.launch {
            _uiState.value = ImportUiState.Loading
            val parseResults = when (format) {
                SingleAccountFormat.BOSSA -> parseBossaExportCsv(text, accountId = account.id, accountName = account.name)
                SingleAccountFormat.BOSSA_CASH_OPERATIONS -> parseBossaCashOperationsCsv(
                    text,
                    accountId = account.id,
                    accountName = account.name,
                    includeAccountOperations = toggleValue
                )
                SingleAccountFormat.XTB -> parseXtbCashOperationsCsv(
                    text,
                    accountId = account.id,
                    accountName = account.name,
                    includeAccountOperations = toggleValue
                )
                SingleAccountFormat.OBLIGACJE_SKARBOWE -> parseObligacjeSkarboweCsv(
                    text,
                    accountId = account.id,
                    accountName = account.name,
                    ignoreAccountBalance = toggleValue
                )
            }
            validateAndShowPreview(parseResults)
        }
    }

    private suspend fun validateAndShowPreview(parseResults: List<CsvRowParseResult>) {
        val accounts = accountRepository.observeAccounts().first()
        val investmentsByAccount: Map<Long, List<InvestmentWithTransactions>> = investmentRepository.observeInvestments().first()
            .filter { it.investment.accountId != null }
            .groupBy { it.investment.accountId!! }
        val accountOperationsByAccount = accountOperationRepository.observeAll().first().groupBy { it.accountId }
        val outcomes = validateImportRows(parseResults, accounts, investmentsByAccount, accountOperationsByAccount)
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
                val cashOperation = outcome.cashOperation
                if (cashOperation != null) {
                    accountOperationRepository.recordAndApply(
                        accountId = cashOperation.accountId,
                        date = cashOperation.date,
                        description = cashOperation.description,
                        amount = cashOperation.amount
                    )
                    return@forEach
                }
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
                if (pendingIncludeAccountOperations) {
                    val tradeValue = row.quantity * row.price
                    val cashDelta = when (row.type) {
                        InvestmentTransactionType.BUY -> -(tradeValue + row.commission)
                        InvestmentTransactionType.SELL -> tradeValue - row.commission
                    }
                    accountRepository.addToBalance(row.accountId, cashDelta)
                }
            }
            _uiState.value = ImportUiState.Done(toImport.size)
        }
    }
}
