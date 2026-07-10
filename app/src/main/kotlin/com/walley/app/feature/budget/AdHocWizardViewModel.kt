package com.walley.app.feature.budget

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.AdHocBudgetRepository
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.AdHocBudgetItem
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val AD_HOC_STEP_DETAILS = 0
const val AD_HOC_STEP_ITEMS = 1
const val AD_HOC_STEP_SUMMARY = 2

/**
 * Autosaves progress as a Draft once the user leaves [AD_HOC_STEP_DETAILS], same mechanism as
 * [BudgetWizardViewModel] — so exiting mid-wizard doesn't lose work, and reopening the wizard for
 * that budget (via [resumeBudgetId]) picks up where it left off.
 */
@HiltViewModel
class AdHocWizardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AdHocBudgetRepository,
    accountRepository: AccountRepository
) : ViewModel() {

    private val resumeBudgetId: Long? = savedStateHandle.get<Long>("resumeBudgetId")?.takeIf { it > 0 }

    /** The budget row this session is persisting to as a Draft; null until the first autosave. */
    private var draftBudgetId: Long? = resumeBudgetId

    var currentStep by mutableIntStateOf(AD_HOC_STEP_DETAILS)
        private set

    var name by mutableStateOf("")
    var startDate by mutableStateOf(LocalDate.now())
    var endDate by mutableStateOf(LocalDate.now().plusDays(7))
    var accountId by mutableStateOf<Long?>(null)
        private set

    private val itemDrafts = mutableStateListOf<AdHocItemDraft>()
    val items: List<AdHocItemDraft> get() = itemDrafts

    val savingAccounts: StateFlow<List<Account>> = accountRepository.observeAccounts()
        .map { accounts -> accounts.filter { it.type == AccountType.SAVING } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        resumeBudgetId?.let { budgetId ->
            viewModelScope.launch {
                val source = repository.observeAdHocBudget(budgetId).first() ?: return@launch
                name = source.budget.name
                startDate = source.budget.startDate
                endDate = source.budget.endDate
                accountId = source.budget.accountId
                loadDraftItems(source.items)
                // Details are already settled for a resumed draft — land straight on the items step.
                currentStep = AD_HOC_STEP_ITEMS
            }
        }
    }

    private fun loadDraftItems(items: List<AdHocBudgetItem>) {
        items.forEachIndexed { index, item ->
            itemDrafts.add(
                AdHocItemDraft(
                    localId = System.nanoTime() + index,
                    name = item.name,
                    amount = item.amount,
                    icon = item.icon,
                    accountId = item.accountId
                )
            )
        }
    }

    fun selectAccount(id: Long?) {
        accountId = id
    }

    val selectedAccount: Account? get() = savingAccounts.value.find { it.id == accountId }

    val detailsValid: Boolean
        get() = name.isNotBlank() && accountId != null && !endDate.isBefore(startDate)

    /** The account a given item draft actually draws from: its own override, or the budget's default. */
    fun accountFor(draft: AdHocItemDraft): Account? =
        (draft.accountId ?: accountId)?.let { id -> savingAccounts.value.find { it.id == id } }

    val totalPlanned: BigDecimal get() = itemDrafts.fold(BigDecimal.ZERO) { acc, item -> acc + item.amount }

    /** Planned totals grouped by the account each item actually draws from. */
    val totalsByAccount: Map<Account, BigDecimal>
        get() = itemDrafts
            .groupBy { accountFor(it) }
            .mapNotNull { (account, drafts) ->
                account?.let { it to drafts.fold(BigDecimal.ZERO) { acc, draft -> acc + draft.amount } }
            }
            .toMap()

    val exceedsAccountBalance: Boolean
        get() = totalsByAccount.any { (account, planned) -> planned > account.balance }

    fun goNext() {
        if (currentStep < AD_HOC_STEP_SUMMARY) {
            currentStep++
            autosaveDraft()
        }
    }

    fun goBack() {
        if (currentStep > AD_HOC_STEP_DETAILS) {
            // Save while still on the step being left, so its edits aren't lost once we step back.
            autosaveDraft()
            currentStep--
        }
    }

    fun addItem(draft: AdHocItemDraft) {
        itemDrafts.add(draft)
        autosaveDraft()
    }

    fun updateItem(localId: Long, draft: AdHocItemDraft) {
        val index = itemDrafts.indexOfFirst { it.localId == localId }
        if (index != -1) itemDrafts[index] = draft
        autosaveDraft()
    }

    fun removeItem(localId: Long) {
        itemDrafts.removeAll { it.localId == localId }
        autosaveDraft()
    }

    /** Fire-and-forget persistence of current progress as a Draft; safe to call frequently. */
    private fun autosaveDraft() {
        if (currentStep <= AD_HOC_STEP_DETAILS) return
        val account = accountId ?: return
        viewModelScope.launch {
            draftBudgetId = repository.saveDraft(draftBudgetId, name.trim(), startDate, endDate, account, collectItems())
        }
    }

    private fun collectItems(): List<AdHocBudgetItem> =
        itemDrafts.map { draft ->
            AdHocBudgetItem(name = draft.name, amount = draft.amount, icon = draft.icon, accountId = draft.accountId)
        }

    /** Finalizes the wizard's budget — a one-way transition out of Draft. */
    suspend fun createBudget(): Long {
        val account = requireNotNull(accountId)
        val id = repository.submitBudget(draftBudgetId, name.trim(), startDate, endDate, account, collectItems())
        draftBudgetId = id
        return id
    }
}
