package com.walley.app.core.ui

import androidx.compose.ui.graphics.Color

/**
 * Deliberately darker than the theme's teal primary, and fixed across light/dark theme (like
 * [AccountBalanceContainerColor]) rather than following `MaterialTheme.colorScheme.primary` — the Home
 * screen's Net worth card wants a darker, more grounded background than the standard teal accent used
 * elsewhere in the app.
 */
val NetWorthCardContainerColor = Color(0xFF004D40)
val NetWorthCardContentColor = Color(0xFFFFFFFF)
