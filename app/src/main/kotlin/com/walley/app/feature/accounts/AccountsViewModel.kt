package com.walley.app.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountHasLinkedActiveBudgetException
import com.walley.app.data.repository.AccountHasLinkedInvestmentsException
import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.ExchangeRateRepository
import com.walley.app.data.repository.InvestmentRepository
import com.walley.app.data.repository.SettingsRepository
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountTaxRate
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.InvestmentWithTransactions
import com.walley.app.domain.model.PortfolioTaxEstimate
import com.walley.app.domain.model.estimatePortfolioTax
import com.walley.app.feature.budget.convertToCurrency
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val repository: AccountRepository,
    investmentRepository: InvestmentRepository,
    settingsRepository: SettingsRepository,
    exchangeRateRepository: ExchangeRateRepository
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = repository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Every investment, grouped by the account it's linked to — used to compute realized-gain tax estimates. */
    val investmentsByAccount: StateFlow<Map<Long, List<InvestmentWithTransactions>>> = investmentRepository.observeInvestments()
        .map { investments -> investments.filter { it.investment.accountId != null }.groupBy { it.investment.accountId!! } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val baseCurrency: StateFlow<Currency> = settingsRepository.observeBaseCurrency()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.PLN)

    private val baseCurrencyRates = baseCurrency
        .flatMapLatest { base -> exchangeRateRepository.observeRates(base).map { rates -> base to rates } }

    /** Combined realized-gain tax estimate across every taxable investment account, for the current calendar year. */
    val portfolioTaxEstimate: StateFlow<PortfolioTaxEstimate> = combine(
        accounts,
        investmentsByAccount,
        baseCurrencyRates
    ) { accts, byAccount, (base, rates) ->
        estimatePortfolioTax(accts, byAccount, LocalDate.now().year) { amount, currency ->
            convertToCurrency(amount, currency, base, rates) ?: BigDecimal.ZERO
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PortfolioTaxEstimate(BigDecimal.ZERO, BigDecimal.ZERO)
    )

    /** Sum of every investment account's unrealized net profit (after tax), converted to base currency. */
    val investmentsNetProfit: StateFlow<BigDecimal> = combine(accounts, baseCurrencyRates) { accts, (base, rates) ->
        accts.filter { it.type == AccountType.INVESTMENT }
            .fold(BigDecimal.ZERO) { acc, account ->
                val netProfit = account.investmentNetProfit ?: BigDecimal.ZERO
                acc + (convertToCurrency(netProfit, account.currency, base, rates) ?: BigDecimal.ZERO)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BigDecimal.ZERO)

    private val _deleteBlockedMessage = MutableStateFlow<String?>(null)
    val deleteBlockedMessage: StateFlow<String?> = _deleteBlockedMessage.asStateFlow()

    fun dismissDeleteBlockedMessage() {
        _deleteBlockedMessage.value = null
    }

    fun addAccount(
        name: String,
        type: AccountType,
        currency: Currency,
        initialBalance: BigDecimal,
        taxRate: AccountTaxRate,
        targetAmount: BigDecimal?,
        commissionFlat: BigDecimal = BigDecimal.ZERO,
        commissionPercent: BigDecimal = BigDecimal.ZERO,
        isVirtual: Boolean = false
    ) {
        viewModelScope.launch {
            repository.addAccount(
                name,
                type,
                currency,
                initialBalance,
                taxRate,
                targetAmount,
                commissionFlat,
                commissionPercent,
                isVirtual
            )
        }
    }

    fun updateAccount(
        accountId: Long,
        name: String,
        type: AccountType,
        taxRate: AccountTaxRate,
        newBalance: BigDecimal,
        targetAmount: BigDecimal?,
        commissionFlat: BigDecimal = BigDecimal.ZERO,
        commissionPercent: BigDecimal = BigDecimal.ZERO,
        isVirtual: Boolean = false
    ) {
        viewModelScope.launch {
            repository.updateAccount(
                accountId,
                name,
                type,
                taxRate,
                newBalance,
                targetAmount,
                commissionFlat,
                commissionPercent,
                isVirtual
            )
        }
    }

    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteAccount(accountId)
            } catch (e: AccountHasLinkedInvestmentsException) {
                _deleteBlockedMessage.value =
                    "This account still has linked investments. Delete or move them first."
            } catch (e: AccountHasLinkedActiveBudgetException) {
                _deleteBlockedMessage.value =
                    "This account is linked to an active budget item. Remove it from the budget first, " +
                        "or mark the budget completed."
            }
        }
    }

    fun setDefaultAccount(accountId: Long) {
        viewModelScope.launch { repository.setDefaultAccount(accountId) }
    }
}
