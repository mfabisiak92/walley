package com.walley.app.domain.model

enum class BudgetStatus {
    /** Being built in the creation wizard; not yet submitted, so no auto-payment or side effects apply. */
    DRAFT,
    ACTIVE,
    COMPLETED
}
