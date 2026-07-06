# Walley — Feature summary

Walley is a native Android app for tracking personal finances entirely on-device (Room/SQLite, no backend). All money values use `BigDecimal`; supported currencies are **PLN, EUR, USD, GBP, NOK, CHF**.

## Navigation

Five bottom tabs, swipeable via a `HorizontalPager`: **Home**, **Accounts**, **Budget**, **Investments**, **Assets**. The active tab's icon and label turn red.

## Home

- **Net worth**: total balance across all accounts, converted to your chosen base currency using live FX rates, shown with the rate date used.
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

## Investments

- Each investment (name, ticker, quantity, currency, purchase price, current price) can be linked to an Investment-type account.
- Tracks **cost basis**, **current value**, and **gain/loss** (absolute and %).
- An account's displayed balance automatically includes the combined current value of everything linked to it, on top of its uninvested cash.

## Assets

For non-liquid assets (property, vehicles, etc.) that aren't bank accounts or investments:
- Name, currency, purchase value, purchase date, and current value (manually updated over time).
- Shows gain/loss versus purchase value, same as Investments.

## Budget

Monthly budgeting with a guided creation flow and payment tracking.

- **One budget per calendar month** (e.g. "July 2026"); the list is sorted newest-first and shows each budget's disposable income.
- **Creation wizard**, in order: Income → Income-related expenses → Fixed costs → Other costs → Savings → Investments → Summary. You can move back and forth freely until you hit Create.
  - Income, Income-related expenses, Fixed costs, and Other costs are free-text name + amount, always in **PLN**.
  - Savings and Investments are picked from your existing accounts and shows that account's current balance (and target, for Savings) — the amount is entered in **that account's own currency**, then converted to PLN behind the scenes for the running totals.
  - Every section except Income and Income-related expenses shows a running **unallocated amount** and **% of disposable income** (disposable income = total income − income-related expenses).
  - Any item can optionally have a **payment day** (a specific day of the month, or the last day of the month).
  - The final step shows the full allocation breakdown plus a pie chart before you confirm creation.
- **Budgets are locked after creation** — items can't be added, removed, or edited; the whole budget can only be marked-paid item by item or deleted outright.
- **Paying items**: each item can be marked fully paid or partially paid (with a custom amount). Items with a payment day are **automatically marked fully paid** the moment that day arrives, checked whenever the Budget tab or a budget's detail screen is opened.
- **Account side effects**: completing a Savings item adds its amount to that account's balance; completing an Investments item adds its amount to the linked account's **uninvested cash** (making clear it's cash sitting in the account, not an actual invested position). These updates are delta-based, so partial → full transitions never double-count.
- Deleting a budget does **not** reverse balance changes already applied by items completed before deletion.

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
