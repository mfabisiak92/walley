package com.walley.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.AssetRepository
import com.walley.app.data.repository.ExchangeRateRepository
import com.walley.app.data.repository.LiabilityRepository
import com.walley.app.data.repository.SettingsRepository
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.Asset
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.CurrencyTotal
import com.walley.app.domain.model.ExchangeRates
import com.walley.app.domain.model.Liability
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeBalances(
    val total: List<CurrencyTotal> = emptyList(),
    val savings: List<CurrencyTotal> = emptyList()
)

data class NetWorthByCurrency(
    val currency: Currency,
    val amountInBaseCurrency: BigDecimal,
    val percent: BigDecimal
)

/** One contributor to net worth (an account or an asset), shown in the breakdown screen. */
data class NetWorthElement(
    val name: String,
    val currency: Currency,
    val originalAmount: BigDecimal,
    val amountInBaseCurrency: BigDecimal
)

data class NetWorthState(
    val currency: Currency,
    // null when conversion is impossible (rates unavailable)
    val amount: BigDecimal?,
    // rate date shown when a conversion actually happened
    val rateDate: String?,
    // breakdown of net worth by the original currency of each account, converted to base currency
    val breakdown: List<NetWorthByCurrency> = emptyList(),
    // every account and asset that contributes to net worth, for the detail/breakdown screen
    val elements: List<NetWorthElement> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    accountRepository: AccountRepository,
    assetRepository: AssetRepository,
    liabilityRepository: LiabilityRepository,
    settingsRepository: SettingsRepository,
    exchangeRateRepository: ExchangeRateRepository
) : ViewModel() {

    val homeBalances: StateFlow<HomeBalances> = accountRepository.observeAccounts()
        .map { accounts ->
            HomeBalances(
                total = currencyTotals(accounts),
                savings = currencyTotals(accounts.filter { it.type == AccountType.SAVING })
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeBalances())

    private val baseCurrencyRates = settingsRepository.observeBaseCurrency()
        .flatMapLatest { base ->
            exchangeRateRepository.observeRates(base).map { rates -> base to rates }
        }

    val netWorth: StateFlow<NetWorthState?> = combine(
        accountRepository.observeAccounts(),
        assetRepository.observeAssets(),
        liabilityRepository.observeLiabilities(),
        baseCurrencyRates
    ) { accounts, assets, liabilities, (base, rates) ->
        if (accounts.isEmpty() && assets.isEmpty() && liabilities.isEmpty()) {
            null
        } else {
            computeNetWorth(accounts, assets, liabilities, base, rates)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun computeNetWorth(
        accounts: List<Account>,
        assets: List<Asset>,
        liabilities: List<Liability>,
        base: Currency,
        rates: ExchangeRates?
    ): NetWorthState {
        val byCurrency = linkedMapOf<Currency, BigDecimal>()
        val elements = mutableListOf<NetWorthElement>()
        var usedRates = false

        fun convertToBase(amount: BigDecimal, currency: Currency): BigDecimal? {
            if (currency == base) return amount
            val rate = rates?.rates?.get(currency) ?: return null
            // rate is base -> currency, so convert back by dividing
            usedRates = true
            return amount.divide(rate, 10, RoundingMode.HALF_UP)
        }

        for (account in accounts) {
            val amountInBase = convertToBase(account.balance, account.currency)
                ?: return NetWorthState(currency = base, amount = null, rateDate = null)
            byCurrency[account.currency] = (byCurrency[account.currency] ?: BigDecimal.ZERO) + amountInBase
            elements += NetWorthElement(
                name = account.name,
                currency = account.currency,
                originalAmount = account.balance,
                amountInBaseCurrency = amountInBase.setScale(2, RoundingMode.HALF_UP)
            )
        }
        for (asset in assets) {
            val amountInBase = convertToBase(asset.currentValue, asset.currency)
                ?: return NetWorthState(currency = base, amount = null, rateDate = null)
            byCurrency[asset.currency] = (byCurrency[asset.currency] ?: BigDecimal.ZERO) + amountInBase
            elements += NetWorthElement(
                name = asset.name,
                currency = asset.currency,
                originalAmount = asset.currentValue,
                amountInBaseCurrency = amountInBase.setScale(2, RoundingMode.HALF_UP)
            )
        }
        for (liability in liabilities) {
            val amountInBase = convertToBase(liability.currentBalance, liability.currency)
                ?: return NetWorthState(currency = base, amount = null, rateDate = null)
            byCurrency[liability.currency] = (byCurrency[liability.currency] ?: BigDecimal.ZERO) - amountInBase
            elements += NetWorthElement(
                name = liability.name,
                currency = liability.currency,
                originalAmount = -liability.currentBalance,
                amountInBaseCurrency = amountInBase.negate().setScale(2, RoundingMode.HALF_UP)
            )
        }

        val total = byCurrency.values.fold(BigDecimal.ZERO) { acc, value -> acc + value }
        val breakdown = byCurrency.entries
            .filter { it.value.signum() > 0 }
            .map { (currency, amount) ->
                val percent = if (total.signum() == 0) {
                    BigDecimal.ZERO
                } else {
                    amount.divide(total, 6, RoundingMode.HALF_UP) * BigDecimal(100)
                }
                NetWorthByCurrency(currency, amount.setScale(2, RoundingMode.HALF_UP), percent)
            }
            .sortedByDescending { it.amountInBaseCurrency }
        return NetWorthState(
            currency = base,
            amount = total.setScale(2, RoundingMode.HALF_UP),
            rateDate = if (usedRates) rates?.date else null,
            breakdown = breakdown,
            elements = elements.sortedByDescending { it.amountInBaseCurrency }
        )
    }

    private fun currencyTotals(accounts: List<Account>): List<CurrencyTotal> =
        Currency.entries.mapNotNull { currency ->
            val total = accounts.filter { it.currency == currency }.sumOf { it.balance }
            if (total.signum() == 0) null else CurrencyTotal(currency, total)
        }
}
