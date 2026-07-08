# Walley — Feature summary

Walley is a native Android app for tracking personal finances entirely on-device (Room/SQLite, no backend). All money values use `BigDecimal`; supported currencies are **PLN, EUR, USD, GBP, NOK, CHF**.

## Navigation

Five bottom tabs, swipeable via a `HorizontalPager`: **Home**, **Accounts**, **Budget**, **Investments**, **Assets**. The active tab's icon and label turn red.

## Home

A single scrolling column, top to bottom:

- **Net worth**: total value across all accounts and assets *minus* liabilities, converted to your chosen base currency using live FX rates. An Investment account always contributes its balance **after tax** — if it has a taxable gain (see Accounts below), the tax owed on that gain is subtracted before it's counted, everywhere net worth is calculated (this tile, the breakdown screen, projected net worth, and monthly snapshots). Tapping it opens a breakdown screen listing every contributing account/asset/liability with its amount in the base currency (liabilities shown as negative, in red) followed by the amount in its original currency when that differs from the base currency, plus the FX rate date used.
- **Projected net worth**: shown on the same tile as "Projected · X end of month" if the current calendar month has a budget — current net worth plus remaining Income and Savings/Investments contributions, minus remaining Income-related-expenses, Fixed costs, and Other costs. Absent if there's no budget for the current month.
- **This month's budget**: a compact card with a spent/planned progress bar, the percentage spent, days left in the month, and the unallocated amount — the same figures as the Budget tab's Summary, without navigating there. The progress bar shifts red → amber → green with % spent, the same color scale used on individual budget item rows. Absent if there's no budget for the current month.
- **Due soon**: up to 3 unpaid items from this month's budget that have a payment day, soonest first — overdue items are called out in red ("Overdue"), otherwise "Due today"/"Due tomorrow"/"Due in N days", each with its amount. Shows the item's own icon if it has one, or a neutral gray badge if it doesn't, so every row has a badge. Absent if nothing's due.
- **Total balance / Savings**: a compact two-column stat row, each tile the same size and broken down per currency.
- **Investments**: a full-width tile below that row (kept separate rather than a third equal-width column, so its numbers never wrap) — one line per currency, gross amount on the left and the after-tax amount on the right (gross minus tax owed on any unrealized gain, see Accounts). Both numbers are tinted per currency by that currency's overall investment gain/loss: green for a gain, red for a loss, blue when it's exactly zero. The three stat tiles (Total balance, Savings, Investments) each use a slightly different background tint to tell them apart at a glance.
- **By currency**: a slim stacked bar plus a color-keyed legend showing the % of net worth held in each currency (replaces the old pie chart).

## Accounts

Four account types, each with its own extra fields:

| Type | Notes |
|---|---|
| Checking | plain balance |
| Cash | plain balance |
| Saving | optional **target amount**; UI shows progress toward the goal and whether it's been reached |
| Investment | balance = **uninvested cash** (money not yet in a position) + current value of linked investments |

Every account also has a **currency** and a **tax rate** (Tax-free or 19%), used for tax-aware calculations on Investment accounts (below).

The Accounts screen is split into three tabs — **Cash & Checking**, **Savings**, **Investments** — each showing only accounts of its own type(s). Adding an account from the Cash & Checking tab offers a Type dropdown limited to Checking/Cash; the Savings and Investments tabs skip the dropdown entirely and create that tab's type directly, since there's nothing to choose. Editing an account's type is restricted the same way, scoped to the tab it's shown on.

On the Investments tab, whenever an account's linked positions show a net gain or loss (current value vs. cost basis), the account row shows it on its own line — green **Gain** or red **Loss**. If there's a gain and the account's tax rate isn't Tax-free, two more lines follow: a neutral blue **Tax** line (gain × the account's rate) and a green **Net profit** line (gain minus tax).

Exactly one account is the **default account** at a time (regardless of tab) — the first account you create becomes default automatically; tap the star on any other account's row to make it the default instead (deleting the default account promotes another one automatically, as long as any accounts remain). The default account is pre-selected wherever an account picker defaults to one (e.g. new Income/Income-related-expenses budget items).

Accounts can be deleted by swiping a row left and tapping the red trash icon (or via the edit dialog), after confirming in a dialog. **An account with linked investments can't be deleted** until those investments are unlinked or deleted first.

## Investments

Split into two tabs.

**Portfolio** — your actual holdings:
- Each investment (name, ticker, mandatory **category**, **purchase date**, quantity, currency, purchase price, current price) can be linked to an Investment-type account.
- **Category** is one of **Stock, ETF, Precious metal, Energy metal, Crypto** — always required, picked from a dropdown when adding or editing, shown as a small colored chip next to the investment's name in the list. Existing investments from before this field existed were backfilled as Stock and can be re-categorized individually.
- **Purchase date** defaults to today when adding one, picked via a date picker (same as Assets/Liabilities), shown as its own line (DD-MM-YYYY) between the ticker/account line and the quantity/price line, and editable afterward via long-press. Existing investments from before this field existed were backfilled to the date the app was updated, since the real historical date isn't recoverable.
- Tracks **cost basis**, **current value**, and **gain/loss** (absolute and %).
- An account's displayed balance automatically includes the combined current value of everything linked to it, on top of its uninvested cash.
- **Tapping** an investment opens a lightweight dialog to update just its **current price** (marking it to market) — the fast path for the thing you do most often. **Long-pressing** it opens the full edit dialog for everything else (name, ticker, category, quantity, purchase price, account), plus deleting it.
- Investments can be deleted by swiping a row left and tapping the red trash icon (or via the long-press edit dialog), after confirming in a dialog.

**Strategies** — a watchlist for equities you're actively monitoring but may not hold (or hold and are deciding what to do with):
- Each tracked equity has a name and an optional ticker (stored uppercase).
- Adding one requires its first **note**: a date (defaults to today, editable via a date picker), one of four statuses — **Sell, Hold, Wait, Buy** (shown as a bold, solid-colored pill for visibility) — and an optional free-text note.
- The list shows each equity's name, ticker, note count, and its status: just the latest status if it's unchanged since the previous note, or a **transition badge** (e.g. "HOLD → BUY") when the two most recent notes disagree, so a status change is obvious at a glance.
- Tapping an equity opens its full note history in **reverse-chronological order** (most recent first). A floating action button adds another dated note with a new status; tapping an existing note opens the same dialog pre-filled to edit its date, status, and text.
- Both equities (with all their notes) and individual notes can be deleted by swiping left and confirming, or via the equity detail screen's delete icon for the equity itself.

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
  - Every section except Income and Income-related expenses shows how much is already **allocated to that section** as a running money total (sum of its items so far, in base currency), alongside its **% of disposable income** and the overall **unallocated amount** across the whole budget (disposable income = total income − income-related expenses).
  - Any item can optionally have a **payment day** (a specific day of the month, or the last day of the month).
  - The final step shows the full allocation breakdown plus a pie chart before you confirm creation.
- **Drafts — leave and come back later**: once you've picked a month and moved past that step, the wizard automatically saves your progress as a **Draft** every time you add, edit, or remove an item, or move between steps — no explicit "save" action needed. A dedicated close (X) button sits in the top bar on every step from then on, so you can exit straight back out at any point without stepping back through each section one by one; the back arrow next to it still steps back one section at a time if you'd rather review earlier steps. Both, and the system back gesture, keep whatever progress was autosaved. Drafts show up in the Budget list with a muted "Draft" label instead of the usual stats; tapping one reopens the wizard exactly where you left off (skipping straight to the first section, since the month's already chosen) so you can keep adding items. A Draft can be swiped away and deleted like any unfinished budget. Nothing about a Draft is ever auto-paid, and it's excluded from Home's "this month's budget"/"due soon"/projected-net-worth figures and from the Analytics Budget-tab charts — it only becomes a real budget once you tap "Create budget" on the Summary step, at which point it turns Active.
- **Budgets are locked after creation** — no new items can be added; the whole budget can only be marked-paid item by item, have individual items' planned amounts edited or deleted, or be deleted outright. Unless the budget is Completed (see below), in which case nothing about it can change at all.
- **Detail screen tabs**: Summary, Income (combining Income + Income-related expenses), Fixed costs, Other costs, Savings, Investments. Every tab shows a spent-vs-planned progress header (amount spent / amount planned, plus a percentage progress bar) scoped to that tab's own section(s); the Summary tab additionally shows **projected net worth** (current net worth plus this budget's still-unpaid items — the same calculation as Home's tile, but for whichever budget you're viewing, not just the current month), the overall spending progress (Fixed + Other + Savings + Investments), the unallocated amount, and the same allocation pie chart shown during creation. All figures use your Settings base currency.
- **Paying items**: tapping an item opens a dialog to mark it fully or partially paid (with a custom amount). Items with a payment day are **automatically marked fully paid** the moment that day arrives, checked whenever the Budget tab or a budget's detail screen is opened. Each item row is compact, showing paid/planned as a fraction (e.g. "100 zł / 200 zł") with a thin progress bar underneath that shifts from red (nothing paid) through amber to green (fully paid).
- **Quick actions on an item**: swiping it right marks it fully paid immediately (no dialog), with a 5-second "Undo" snackbar reverting it to however much was paid before; long-pressing it opens a dialog to edit its planned amount (or delete it outright, with the same undo snackbar as deleting via the edit dialog's own Delete button). Editing an amount down below what's already been paid clamps the paid amount to match. Both actions are disabled once the budget is Completed.
- **Editing an item's linked account**: the long-press edit dialog lets you reassign which account any item is linked to. For Income, Income-related expenses, Savings, and Investments this is required (can't be cleared); for Fixed costs and Other costs it's optional (can be cleared to "None"). The same account-type restrictions and Saving-balance cap apply as when the item was created. If any of the item has already been paid, the already-applied balance effect moves from the old account to the new one. Reassigning a Savings or Investments item's account also updates its name and currency to match the newly picked account, since the account is that item's identity.
- **Item icons**: every item can have a small colored icon, picked from a curated set: Salary/Dividends/Interest for Income (matching its category), Saving/Investment (assigned automatically for Savings/Investments items, no picker needed), and 34 general expense icons — Tax, Car, Fuel, Subscription, Groceries, Clothes, Eating out, Entertainment, Transportation, Vacations, Trip, Gift, Insurance, Currency exchange, Electronics, Phone, Bills, Rent, Piggy bank, Electricity, Internet, TV, Music, Barber, Games, Sport, Books, Repair, Health, Medicine, Pet, Movie, Kids, Retirement — offered for Income-related expenses, Fixed costs, and Other costs. Picked when adding/editing an item (both in the creation wizard and via an existing item's long-press edit dialog). Items created before this feature got an icon backfilled automatically where possible, based on their income category, section, or a keyword match against their name.
- **Icon auto-suggestion while typing**: in the "Add item" dialog, the icon updates live as you type the item's name, matched against a keyword list per icon (e.g. "rent"/"czynsz" → Rent, "prąd" → Electricity, "groceries"/"spożywcze" → Groceries). Keywords cover both English and Polish so either language works without a language setting, and Polish diacritics (ą, ę, ń, ć, ś, ż, ź, ó) are optional — typing "prad" or "prąd", "samochod" or "samochód", etc. both match, since typing them without a Polish keyboard layout is common. As soon as you manually tap a different icon yourself, the suggestion stops overriding your choice for the rest of that dialog.
- **Icon picker legibility**: the icon row always shows the currently selected icon's name as text above it and auto-scrolls that icon into view, so a newly auto-suggested or tapped icon is never buried out of sight among the 30+ options.
- **Account side effects**: completing a Savings item adds its amount to that account's balance; completing an Investments item adds its amount to the linked account's **uninvested cash**; completing an Income item adds its amount to its linked Checking/Cash account; completing an Income-related-expense or an Other costs item (if linked to an account) subtracts its amount from its linked account. These updates are delta-based, so partial → full transitions (and un-paying) never double-count. Editing a Saving-linked Other costs item's planned amount later is capped the same way — it can't ask for more than what's still left in the account, accounting for however much of it has already been paid out.
- **Deleting an item**: long-press it, then tap "Delete item" in the edit dialog (even in a created/locked budget, as long as it's still Active); a 5-second "Undo" snackbar follows. Deleting an item does **not** reverse any account balance change it already applied.
- **Deleting a whole budget**: swipe a budget row left and tap the red trash icon (or use the trash icon on its detail screen), then confirm. A **Completed** budget can't be deleted, and the swipe gesture/trash icon are hidden for it entirely rather than just blocked after the fact.
- **Status: Draft → Active → Completed**: a budget starts as a Draft while being built in the wizard, becomes Active once you tap "Create budget", and can then be marked Completed. From its detail screen, a one-way "Mark as completed" action switches an Active budget to Completed (with a confirmation, since it becomes permanently read-only). Once Completed, nothing about the budget can change — items can no longer be paid, partially paid, or deleted, and the budget itself can't be deleted. Completed (and Draft) budgets show a muted card color and status label in the Budget list to set them apart from Active ones.
- **Cloning a budget**: from a budget's detail screen, the clone icon opens the creation wizard pre-filled with that budget's items (defaulting to the following month, paid/completed state reset). Every item can still be edited (tap it) or removed before creating, and a new month must be chosen if the default is already taken.

## Analytics

Reachable via an icon on Home's top bar, split into three tabs. Charts are hand-rolled (no charting library dependency, consistent with Home's pie chart/currency bar); months with a missing exchange rate show as a gap rather than a wrong value.

**Budget tab** — charts every past budget (oldest → newest), converted into your Settings base currency using the same helpers as the budget detail screen, as horizontally-scrollable grouped bar charts:
- **Income vs Expenses vs Savings**: grouped bar chart per budget month.
- **Savings rate**: (Savings + Investments) ÷ disposable income, per month.
- **Budget adherence**: overall spent vs planned percentage per month (Fixed + Other + Savings + Investments), the same metric shown on each budget's Summary tab.

**History tab** — charts a **financial snapshot**, captured automatically every time a budget is marked completed (mirrors a monthly "close the books" habit). Each snapshot records, in that month's Settings base currency:
- Cash + Checking total, Savings total, Investments total, Assets total, Liabilities total, and net worth
- Income, Income-related expenses, disposable income
- Income broken down by category: Salary, Dividends, Interest, Other
- **Investment growth**: this month's investment total minus last month's minus this month's Investments-section contributions — i.e. market movement alone, isolated from money you added. The very first snapshot has no prior month to diff against, so growth is blank for it.

A **data horizon** selector at the top of this tab (6M / 1Y / 2Y / 5Y / ∞, defaulting to 1Y) limits every chart on the tab to that many trailing months (∞ shows everything ever recorded). Each of this tab's 4 charts (Account balances, Net worth, Income by source, Investment growth) is drawn as an **area chart by default**; swiping left or right anywhere on a chart toggles it to a **line chart** and back — independently per chart, so different charts can be in different modes at once.

If exchange rates are unavailable at the exact moment a budget is completed, the snapshot is still recorded (budget completion always succeeds); any unconvertible figure falls back to zero rather than blocking the action.

**Investments tab** — a single pie chart breaking down the current value of every investment in Portfolio by its **category** (Stock, ETF, Precious metal, Energy metal, Crypto), converted to your Settings base currency, using the same slice colors as the category chips shown in Portfolio. Absent if you have no investments yet.

## Settings

Split into two tabs.

**General** — Dark mode toggle (defaults to system setting); Fingerprint (biometric) unlock toggle (disabled if no fingerprint is enrolled); Base currency picker, used for net worth conversion on Home; **Change PIN**, which asks for the current PIN (rejected with an inline error if wrong) plus a new PIN entered twice, and confirms with a snackbar once changed.

**Budget** — optional target % of disposable income for Fixed costs, Other costs, Savings, and Investments. Fixed costs/Other costs are spending ceilings (a target you don't want to exceed); Savings/Investments are goals (a target you want to reach or exceed). Once set, each target appears at the bottom of the matching tab — both during budget creation (the wizard's per-section footer) and on an existing budget's detail screen — showing the target itself plus how far the section's actual % of disposable income sits from it: e.g. "10% over target allocation" with a warning for Fixed/Other costs that ran over, or "5% below target allocation" with a warning for Savings/Investments that fell short; a green checkmark shows instead once Fixed/Other costs are at or under target, or Savings/Investments are at or over target.

## App lock

- On first launch, the user sets up a 4–6 digit PIN.
- The app locks itself when it's been in the background for more than **30 seconds** and requires the PIN — or fingerprint, if enabled in Settings, with a "Use PIN" fallback — before any content is shown again. Returning within 30 seconds (e.g. switching to another app briefly) resumes right where you left off, no re-entry needed. Rotating the screen never triggers a lock.
- The recent-apps switcher never shows a screenshot of the app's content (a system-level `FLAG_SECURE` protection), so this grace period doesn't come at the cost of financial data being visible outside the app.

## Currency conversion

- Live exchange rates (ECB reference rates) are fetched from the [Frankfurter](https://frankfurter.dev) API and cached per base currency for 1 hour; a cached rate is shown immediately and refreshed in the background once stale.
- Used for Home's net worth conversion and for reconciling every budget figure (including foreign-currency Savings/Investments items) into your Settings base currency.
