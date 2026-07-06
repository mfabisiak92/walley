# Walley — Feature summary

Walley is a native Android app for tracking personal finances entirely on-device (Room/SQLite, no backend). All money values use `BigDecimal`; supported currencies are **PLN, EUR, USD, GBP, NOK, CHF**.

## Navigation

Five bottom tabs, swipeable via a `HorizontalPager`: **Home**, **Accounts**, **Budget**, **Investments**, **Assets**. The active tab's icon and label turn red.

## Home

- **Net worth**: total value across all accounts *and* assets, converted to your chosen base currency using live FX rates, shown with the rate date used. Tapping the Net worth tile opens a breakdown screen listing every contributing account/asset with its amount in the base currency, followed by the amount in its original currency when that differs from the base currency.
- **Currency breakdown**: total balance and total savings broken out per currency.
- **Pie chart**: share of net worth held in each currency.

## Accounts

Four account types, each with its own extra fields:

| Type | Notes |
|---|---|
| Checking | plain balance |
| Cash | plain balance |
| Saving | optional **target amount**; UI shows progress toward the goal and whether it's been reached |
| Investment | balance = **uninvested cash** (money not yet in a position) + current value of linked investments |

Every account also has a **currency** and a **tax rate** (Tax-free or 19%), used for future tax-aware calculations.

Accounts can be deleted by swiping a row left and tapping the red trash icon (or via the edit dialog), after confirming in a dialog. **An account with linked investments can't be deleted** until those investments are unlinked or deleted first.

## Investments

- Each investment (name, ticker, quantity, currency, purchase price, current price) can be linked to an Investment-type account.
- Tracks **cost basis**, **current value**, and **gain/loss** (absolute and %).
- An account's displayed balance automatically includes the combined current value of everything linked to it, on top of its uninvested cash.
- Investments can be deleted by swiping a row left and tapping the red trash icon (or via the edit dialog), after confirming in a dialog.

## Assets

For non-liquid assets (property, vehicles, etc.) that aren't bank accounts or investments:
- Name, currency, purchase value, purchase date, and current value (manually updated over time).
- Shows gain/loss versus purchase value, same as Investments.
- Included in Home's net worth calculation and breakdown.
- Assets can be deleted by swiping a row left and tapping the red trash icon (or via the edit dialog), after confirming in a dialog.

## Budget

Monthly budgeting with a guided creation flow and payment tracking.

- **One budget per calendar month** (e.g. "July 2026"); the list is sorted newest-first. Each row shows disposable income, unallocated amount (converted to your Settings base currency), and an overall spent-vs-planned progress bar with percentage (spent/planned cover Fixed costs, Other costs, Savings, and Investments — not Income).
- **Creation wizard**, in order: Income → Income-related expenses → Fixed costs → Other costs → Savings → Investments → Summary. You can move back and forth freely until you hit Create.
  - Income, Income-related expenses, Fixed costs, and Other costs are free-text name + amount, always in **PLN**.
  - Savings and Investments are picked from your existing accounts and shows that account's current balance (and target, for Savings) — the amount is entered in **that account's own currency**, then converted to PLN behind the scenes for the running totals.
  - Every section except Income and Income-related expenses shows a running **unallocated amount** and **% of disposable income** (disposable income = total income − income-related expenses).
  - Any item can optionally have a **payment day** (a specific day of the month, or the last day of the month).
  - The final step shows the full allocation breakdown plus a pie chart before you confirm creation.
- **Budgets are locked after creation** — items can't be added or edited; the whole budget can only be marked-paid item by item or deleted outright. Individual items *can* still be deleted (see below), as an exception to the lock — unless the budget is Completed (see below), in which case nothing about it can change at all.
- **Detail screen tabs**: Summary, Income (combining Income + Income-related expenses), Fixed costs, Other costs, Savings, Investments. Every tab shows a spent-vs-planned progress header (amount spent / amount planned, plus a percentage progress bar) scoped to that tab's own section(s); the Summary tab additionally shows the overall spending progress (Fixed + Other + Savings + Investments), the unallocated amount, and the same allocation pie chart shown during creation.
- **Paying items**: each item can be marked fully paid or partially paid (with a custom amount). Items with a payment day are **automatically marked fully paid** the moment that day arrives, checked whenever the Budget tab or a budget's detail screen is opened.
- **Account side effects**: completing a Savings item adds its amount to that account's balance; completing an Investments item adds its amount to the linked account's **uninvested cash** (making clear it's cash sitting in the account, not an actual invested position). These updates are delta-based, so partial → full transitions never double-count.
- **Deleting an item**: swipe a budget item left and tap the red trash icon to delete it immediately (even in a created/locked budget, as long as it's still Active); a 5-second "Undo" snackbar follows. Deleting an item does **not** reverse any account balance change it already applied.
- **Deleting a whole budget**: swipe a budget row left and tap the red trash icon (or use the trash icon on its detail screen), then confirm. A **Completed** budget can't be deleted.
- **Status: Active → Completed**: every budget starts Active. From its detail screen, a one-way "Mark as completed" action switches it to Completed (with a confirmation, since it becomes permanently read-only). Once Completed, nothing about the budget can change — items can no longer be paid, partially paid, or deleted, and the budget itself can't be deleted. Completed budgets show a "Completed" label and a muted card color in the Budget list to set them apart from Active ones.
- **Cloning a budget**: from a budget's detail screen, the clone icon opens the creation wizard pre-filled with that budget's items (defaulting to the following month, paid/completed state reset). Every item can still be edited (tap it) or removed before creating, and a new month must be chosen if the default is already taken.

## Settings

- Dark mode toggle (defaults to system setting).
- Fingerprint (biometric) unlock toggle — disabled if no fingerprint is enrolled on the device.
- Base currency picker, used for net worth conversion on Home.

## App lock

- On first launch, the user sets up a 4–6 digit PIN.
- The app locks itself every time it leaves the foreground (not just cold start) and requires the PIN — or fingerprint, if enabled in Settings, with a "Use PIN" fallback — before any content is shown again.

## Currency conversion

- Live exchange rates (ECB reference rates) are fetched from the [Frankfurter](https://frankfurter.dev) API and cached per base currency for 1 hour; a cached rate is shown immediately and refreshed in the background once stale.
- Used for Home's net worth conversion and for reconciling foreign-currency Savings/Investments budget items back into PLN.
