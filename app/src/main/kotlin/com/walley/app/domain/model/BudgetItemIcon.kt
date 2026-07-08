package com.walley.app.domain.model

enum class BudgetItemIcon(val label: String) {
    SALARY("Salary"),
    DIVIDENDS("Dividends"),
    INTEREST("Interest"),
    SAVING("Saving"),
    INVESTMENT("Investment"),
    TAX("Tax"),
    CAR("Car"),
    FUEL("Fuel"),
    SUBSCRIPTION("Subscription"),
    GROCERIES("Groceries"),
    CLOTHES("Clothes"),
    EATING_OUT("Eating out"),
    ENTERTAINMENT("Entertainment"),
    TRANSPORTATION("Transportation"),
    VACATIONS("Vacations"),
    TRIP("Trip"),
    GIFT("Gift"),
    INSURANCE("Insurance"),
    CURRENCY_EXCHANGE("Currency exchange"),
    ELECTRONICS("Electronics"),
    PHONE("Phone"),
    BILLS("Bills"),
    RENT("Rent"),
    PIGGY_BANK("Piggy bank"),
    ELECTRICITY("Electricity"),
    INTERNET("Internet"),
    TV("TV"),
    MUSIC("Music"),
    BARBER("Barber"),
    GAMES("Games"),
    SPORT("Sport"),
    BOOKS("Books"),
    REPAIR("Repair"),
    HEALTH("Health"),
    MEDICINE("Medicine"),
    PET("Pet"),
    MOVIE("Movie"),
    KIDS("Kids"),
    RETIREMENT("Retirement"),
    PARKING("Parking"),
    SHIP("Ship"),
    SHIPMENT("Shipment"),
    DOCTOR("Doctor")
}

/** The icon that mirrors this income category, if any ([IncomeCategory.OTHER] has no natural icon). */
fun IncomeCategory.toIcon(): BudgetItemIcon? = when (this) {
    IncomeCategory.SALARY -> BudgetItemIcon.SALARY
    IncomeCategory.DIVIDENDS -> BudgetItemIcon.DIVIDENDS
    IncomeCategory.INTEREST -> BudgetItemIcon.INTEREST
    IncomeCategory.OTHER -> null
}

/** Icons offered for Income items, matching [IncomeCategory]. */
val INCOME_ICONS: List<BudgetItemIcon> = listOf(
    BudgetItemIcon.SALARY,
    BudgetItemIcon.DIVIDENDS,
    BudgetItemIcon.INTEREST
)

/** Icons offered for free-text expense items (Income-related expenses, Fixed costs, Other costs). */
val EXPENSE_ICONS: List<BudgetItemIcon> = listOf(
    BudgetItemIcon.TAX,
    BudgetItemIcon.CAR,
    BudgetItemIcon.FUEL,
    BudgetItemIcon.SUBSCRIPTION,
    BudgetItemIcon.GROCERIES,
    BudgetItemIcon.CLOTHES,
    BudgetItemIcon.EATING_OUT,
    BudgetItemIcon.ENTERTAINMENT,
    BudgetItemIcon.TRANSPORTATION,
    BudgetItemIcon.VACATIONS,
    BudgetItemIcon.TRIP,
    BudgetItemIcon.GIFT,
    BudgetItemIcon.INSURANCE,
    BudgetItemIcon.CURRENCY_EXCHANGE,
    BudgetItemIcon.ELECTRONICS,
    BudgetItemIcon.PHONE,
    BudgetItemIcon.BILLS,
    BudgetItemIcon.RENT,
    BudgetItemIcon.PIGGY_BANK,
    BudgetItemIcon.ELECTRICITY,
    BudgetItemIcon.INTERNET,
    BudgetItemIcon.TV,
    BudgetItemIcon.MUSIC,
    BudgetItemIcon.BARBER,
    BudgetItemIcon.GAMES,
    BudgetItemIcon.SPORT,
    BudgetItemIcon.BOOKS,
    BudgetItemIcon.REPAIR,
    BudgetItemIcon.HEALTH,
    BudgetItemIcon.MEDICINE,
    BudgetItemIcon.PET,
    BudgetItemIcon.MOVIE,
    BudgetItemIcon.KIDS,
    BudgetItemIcon.RETIREMENT,
    BudgetItemIcon.PARKING,
    BudgetItemIcon.SHIP,
    BudgetItemIcon.SHIPMENT,
    BudgetItemIcon.DOCTOR
)
