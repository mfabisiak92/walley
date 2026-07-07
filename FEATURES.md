# Walley — Feature summary

Walley is a native Android app for tracking personal finances entirely on-device (Room/SQLite, no backend). All money values use `BigDecimal`; supported currencies are **PLN, EUR, USD, GBP, NOK, CHF**.

## Navigation

Five bottom tabs, swipeable via a `HorizontalPager`: **Home**, **Accounts**, **Budget**, **Investments**, **Assets**. The active tab's icon and label turn red.

## Home

- **Net worth**: total value across all accounts and assets *minus* liabilities, converted to your chosen base currency using live FX rates, shown with the rate date used. Tapping the Net worth tile opens a breakdown screen listing every contributing account/asset/liability with its amount in the base currency (liabilities shown as negative, in red), followed by the amount in its original currency when that differs from the base currency.
- **Projected net worth**: if the current calendar month has a budget, the same tile also shows what net worth would be at month's end if that budget's still-unpaid items were all completed — current net worth plus remaining Income and Savings/Investments contributions, minus remaining Income-related-expenses, Fixed costs, and Other costs. Blank if there's no budget for the current month.
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

Exactly one account is the **default account** at a time — the first account you create becomes default automatically; tap the star on any other account's row to make it the default instead (deleting the default account promotes another one automatically, as long as any accounts remain). The default account is pre-selected wherever an account picker defaults to one (e.g. new Income/Income-related-expenses budget items).

Accounts can be deleted by swiping a row left and tapping the red trash icon (or via the edit dialog), after confirming in a dialog. **An account with linked investments can't be deleted** until those investments are unlinked or deleted first.

## Investments

- Each investment (name, ticker, quantity, currency, purchase price, current price) can be linked to an Investment-type account.
- Tracks **cost basis**, **current value**, and **gain/loss** (absolute and %).
- An account's displayed balance automatically includes the combined current value of everything linked to it, on top of its uninvested cash.
- Investments can be deleted by swiping a row left and tapping the red trash icon (or via the edit dialog), after confirming in a dialog.

## Assets & Liabilities

One bottom tab, split into two sub-tabs so each keeps its own simple list/add/edit flow without needing a second bottom-nav slot.

**Assets** — for non-liquid assets (property, vehicles, etc.) that aren't bank accounts or investments:
- Name, currency, purchase value, purchase date, and current value (manually updated over time).
- Shows gain/loss versus purchase value, same as Investments.
- Included in Home's net worth calculation and breakdown (adds to net worth).

**Liabilities** — for debts (loans, credit cards, mortgages):
- Name, currency, original amount, current balance, and start date (manually updated over time, mirroring Assets).
- Shows a payoff progress bar/percentage (how much of the original amount has been paid down).
- Included in Home's net worth calculation and breakdown (subtracts from net worth).

Both support swiping a row left and tapping the red trash icon (or the edit dialog) to delete, after confirming in a dialog.

## Budget

Monthly budgeting with a guided creation flow and payment tracking.

- **One budget per calendar month** (e.g. "July 2026"); the list is sorted newest-first. Each row shows disposable income, unallocated amount, and an overall spent-vs-planned progress bar with percentage (spent/planned cover Fixed costs, Other costs, Savings, and Investments — not Income) — all shown in your Settings **base currency**.
- **Creation wizard**, in order: Income → Income-related expenses → Fixed costs → Other costs → Savings → Investments → Summary. You can move back and forth freely until you hit Create.
  - Income, Income-related expenses, Fixed costs, and Other costs are free-text name + amount, entered in your Settings **base currency**.
  - Income and Income-related-expenses items also require picking one of your **Checking/Cash/Investment accounts** (mandatory dropdown, pre-selected with your default account) — that's the account money is considered to land in/leave from. Income items also require a **category** (Salary, Dividends, Interest, or Other), used to break income down by source in your history.
  - Fixed costs and Other costs items can *optionally* be linked to a **Checking, Cash, or Saving account** (not Investment) — for costs paid straight out of savings (e.g. an emergency withdrawal) or another account. If the linked account is a Saving account, the amount can't exceed that account's current balance.
  - Savings and Investments are picked from your existing accounts and shows that account's current balance (and target, for Savings) — the amount is entered in **that account's own currency**, then converted behind the scenes for the running totals.
  - Every section except Income and Income-related expenses shows a running **unallocated amount** and **% of disposable income** (disposable income = total income − income-related expenses).
  - Any item can optionally have a **payment day** (a specific day of the month, or the last day of the month).
  - The final step shows the full allocation breakdown plus a pie chart before you confirm creation.
- **Budgets are locked after creation** — no new items can be added; the whole budget can only be marked-paid item by item, have individual items' planned amounts edited or deleted, or be deleted outright. Unless the budget is Completed (see below), in which case nothing about it can change at all.
- **Detail screen tabs**: Summary, Income (combining Income + Income-related expenses), Fixed costs, Other costs, Savings, Investments. Every tab shows a spent-vs-planned progress header (amount spent / amount planned, plus a percentage progress bar) scoped to that tab's own section(s); the Summary tab additionally shows **projected net worth** (current net worth plus this budget's still-unpaid items — the same calculation as Home's tile, but for whichever budget you're viewing, not just the current month), the overall spending progress (Fixed + Other + Savings + Investments), the unallocated amount, and the same allocation pie chart shown during creation. All figures use your Settings base currency.
- **Paying items**: tapping an item opens a dialog to mark it fully or partially paid (with a custom amount). Items with a payment day are **automatically marked fully paid** the moment that day arrives, checked whenever the Budget tab or a budget's detail screen is opened. Each item row is compact, showing paid/planned as a fraction (e.g. "100 zł / 200 zł") with a thin progress bar underneath that shifts from red (nothing paid) through amber to green (fully paid).
- **Quick actions on an item**: swiping it right marks it fully paid immediately (no dialog), with a 5-second "Undo" snackbar reverting it to however much was paid before; long-pressing it opens a dialog to edit its planned amount (or delete it outright, with the same undo snackbar as deleting via the edit dialog's own Delete button). Editing an amount down below what's already been paid clamps the paid amount to match. Both actions are disabled once the budget is Completed.
- **Editing an item's linked account**: the long-press edit dialog lets you reassign which account any item is linked to. For Income, Income-related expenses, Savings, and Investments this is required (can't be cleared); for Fixed costs and Other costs it's optional (can be cleared to "None"). The same account-type restrictions and Saving-balance cap apply as when the item was created. If any of the item has already been paid, the already-applied balance effect moves from the old account to the new one. Reassigning a Savings or Investments item's account also updates its name and currency to match the newly picked account, since the account is that item's identity.
- **Item icons**: every item can have a small colored icon, picked from a curated set: Salary/Dividends/Interest for Income (matching its category), Saving/Investment (assigned automatically for Savings/Investments items, no picker needed), and 18 general expense icons — Tax, Car, Fuel, Subscription, Groceries, Clothes, Eating out, Entertainment, Transportation, Vacations, Trip, Gift, Insurance, Currency exchange, Electronics, Phone, Bills, Rent — offered for Income-related expenses, Fixed costs, and Other costs. Picked when adding/editing an item (both in the creation wizard and via an existing item's long-press edit dialog). Items created before this feature got an icon backfilled automatically where possible, based on their income category, section, or a keyword match against their name.
- **Account side effects**: completing a Savings item adds its amount to that account's balance; completing an Investments item adds its amount to the linked account's **uninvested cash**; completing an Income item adds its amount to its linked Checking/Cash account; completing an Income-related-expense or an Other costs item (if linked to an account) subtracts its amount from its linked account. These updates are delta-based, so partial → full transitions (and un-paying) never double-count. Editing a Saving-linked Other costs item's planned amount later is capped the same way — it can't ask for more than what's still left in the account, accounting for however much of it has already been paid out.
- **Deleting an item**: long-press it, then tap "Delete item" in the edit dialog (even in a created/locked budget, as long as it's still Active); a 5-second "Undo" snackbar follows. Deleting an item does **not** reverse any account balance change it already applied.
- **Deleting a whole budget**: swipe a budget row left and tap the red trash icon (or use the trash icon on its detail screen), then confirm. A **Completed** budget can't be deleted.
- **Status: Active → Completed**: every budget starts Active. From its detail screen, a one-way "Mark as completed" action switches it to Completed (with a confirmation, since it becomes permanently read-only). Once Completed, nothing about the budget can change — items can no longer be paid, partially paid, or deleted, and the budget itself can't be deleted. Completed budgets show a "Completed" label and a muted card color in the Budget list to set them apart from Active ones.
- **Cloning a budget**: from a budget's detail screen, the clone icon opens the creation wizard pre-filled with that budget's items (defaulting to the following month, paid/completed state reset). Every item can still be edited (tap it) or removed before creating, and a new month must be chosen if the default is already taken.

## Analytics

Reachable via an icon on Home's top bar, split into two tabs. Charts are a hand-rolled, horizontally-scrollable bar chart component (no charting library dependency, consistent with Home's pie chart); months with a missing exchange rate show as a gap rather than a wrong value.

**Budget tab** — charts every past budget (oldest → newest), converted into your Settings base currency using the same helpers as the budget detail screen:
- **Income vs Expenses vs Savings**: grouped bar chart per budget month.
- **Savings rate**: (Savings + Investments) ÷ disposable income, per month.
- **Budget adherence**: overall spent vs planned percentage per month (Fixed + Other + Savings + Investments), the same metric shown on each budget's Summary tab.

**History tab** — charts a **financial snapshot**, captured automatically every time a budget is marked completed (mirrors a monthly "close the books" habit). Each snapshot records, in that month's Settings base currency:
- Cash + Checking total, Savings total, Investments total, Assets total, Liabilities total, and net worth
- Income, Income-related expenses, disposable income
- Income broken down by category: Salary, Dividends, Interest, Other
- **Investment growth**: this month's investment total minus last month's minus this month's Investments-section contributions — i.e. market movement alone, isolated from money you added. The very first snapshot has no prior month to diff against, so growth is blank for it.

If exchange rates are unavailable at the exact moment a budget is completed, the snapshot is still recorded (budget completion always succeeds); any unconvertible figure falls back to zero rather than blocking the action.

## Settings

Split into two tabs.

**General** — Dark mode toggle (defaults to system setting); Fingerprint (biometric) unlock toggle (disabled if no fingerprint is enrolled); Base currency picker, used for net worth conversion on Home.

**Budget** — optional target % of disposable income for Fixed costs, Other costs, Savings, and Investments. Fixed costs/Other costs are spending ceilings (a target you don't want to exceed); Savings/Investments are goals (a target you want to reach or exceed). Once set, each target appears at the bottom of the matching tab — both during budget creation (the wizard's per-section footer) and on an existing budget's detail screen — showing the target itself plus how far the section's actual % of disposable income sits from it: e.g. "10% over target allocation" with a warning for Fixed/Other costs that ran over, or "5% below target allocation" with a warning for Savings/Investments that fell short; a green checkmark shows instead once Fixed/Other costs are at or under target, or Savings/Investments are at or over target.

## App lock

- On first launch, the user sets up a 4–6 digit PIN.
- The app locks itself every time it leaves the foreground (not just cold start) and requires the PIN — or fingerprint, if enabled in Settings, with a "Use PIN" fallback — before any content is shown again.

## Currency conversion

- Live exchange rates (ECB reference rates) are fetched from the [Frankfurter](https://frankfurter.dev) API and cached per base currency for 1 hour; a cached rate is shown immediately and refreshed in the background once stale.
- Used for Home's net worth conversion and for reconciling every budget figure (including foreign-currency Savings/Investments items) into your Settings base currency.
