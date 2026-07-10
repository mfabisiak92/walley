package com.walley.app.feature.budget

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

const val AD_HOC_STEP_DETAILS = 0
const val AD_HOC_STEP_ITEMS = 1
const val AD_HOC_STEP_SUMMARY = 2

/**
 * Unlike [BudgetWizardViewModel], this wizard has no Draft/autosave concept — it's short enough
 * (3 steps, one sitting) that the budget is only persisted once, when [createBudget] is called.
 */
@HiltViewModel
class AdHocWizardViewModel @Inject constructor(
    private val repository: AdHocBudgetRepository,
    accountRepository: AccountRepository
) : ViewModel() {

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
        if (currentStep < AD_HOC_STEP_SUMMARY) currentStep++
    }

    fun goBack() {
        if (currentStep > AD_HOC_STEP_DETAILS) currentStep--
    }

    fun addItem(draft: AdHocItemDraft) {
        itemDrafts.add(draft)
    }

    fun updateItem(localId: Long, draft: AdHocItemDraft) {
        val index = itemDrafts.indexOfFirst { it.localId == localId }
        if (index != -1) itemDrafts[index] = draft
    }

    fun removeItem(localId: Long) {
        itemDrafts.removeAll { it.localId == localId }
    }

    suspend fun createBudget(): Long {
        val account = requireNotNull(accountId)
        return repository.createAdHocBudget(
            name = name.trim(),
            startDate = startDate,
            endDate = endDate,
            accountId = account,
            items = itemDrafts.map { draft ->
                AdHocBudgetItem(name = draft.name, amount = draft.amount, icon = draft.icon, accountId = draft.accountId)
            }
        )
    }
}
