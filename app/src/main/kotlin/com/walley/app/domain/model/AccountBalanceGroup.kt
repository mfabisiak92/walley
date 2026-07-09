package com.walley.app.domain.model

/** Groups account types for the bulk "Update balances" screen, matching the Accounts tab split. */
enum class AccountBalanceGroup(val label: String, val types: List<AccountType>) {
    CASH_CHECKING("Cash & Checking", listOf(AccountType.CHECKING, AccountType.CASH)),
    SAVINGS("Savings", listOf(AccountType.SAVING)),
    INVESTMENTS("Investments", listOf(AccountType.INVESTMENT))
}
