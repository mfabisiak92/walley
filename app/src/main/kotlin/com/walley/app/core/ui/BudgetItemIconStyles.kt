package com.walley.app.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.walley.app.domain.model.BudgetItemIcon

data class BudgetItemIconStyle(val vector: ImageVector, val color: Color)

/** Simple, colorful icon + tint for each [BudgetItemIcon], distinct enough to tell items apart at a glance. */
val BudgetItemIconStyles: Map<BudgetItemIcon, BudgetItemIconStyle> = mapOf(
    BudgetItemIcon.SALARY to BudgetItemIconStyle(Icons.Filled.Payments, Color(0xFF2E7D32)),
    BudgetItemIcon.DIVIDENDS to BudgetItemIconStyle(Icons.Filled.AccountBalance, Color(0xFF00695C)),
    BudgetItemIcon.INTEREST to BudgetItemIconStyle(Icons.Filled.Percent, Color(0xFF1565C0)),
    BudgetItemIcon.SAVING to BudgetItemIconStyle(Icons.Filled.Savings, Color(0xFF2E7D32)),
    BudgetItemIcon.INVESTMENT to BudgetItemIconStyle(Icons.Filled.TrendingUp, Color(0xFF00695C)),
    BudgetItemIcon.TAX to BudgetItemIconStyle(Icons.Filled.ReceiptLong, Color(0xFFC62828)),
    BudgetItemIcon.CAR to BudgetItemIconStyle(Icons.Filled.DirectionsCar, Color(0xFF1565C0)),
    BudgetItemIcon.FUEL to BudgetItemIconStyle(Icons.Filled.LocalGasStation, Color(0xFFEF6C00)),
    BudgetItemIcon.SUBSCRIPTION to BudgetItemIconStyle(Icons.Filled.Subscriptions, Color(0xFF6A1B9A)),
    BudgetItemIcon.GROCERIES to BudgetItemIconStyle(Icons.Filled.ShoppingCart, Color(0xFF2E7D32)),
    BudgetItemIcon.CLOTHES to BudgetItemIconStyle(Icons.Filled.Checkroom, Color(0xFFAD1457)),
    BudgetItemIcon.EATING_OUT to BudgetItemIconStyle(Icons.Filled.Restaurant, Color(0xFFEF6C00)),
    BudgetItemIcon.ENTERTAINMENT to BudgetItemIconStyle(Icons.Filled.Theaters, Color(0xFF6A1B9A)),
    BudgetItemIcon.TRANSPORTATION to BudgetItemIconStyle(Icons.Filled.DirectionsBus, Color(0xFF1565C0)),
    BudgetItemIcon.VACATIONS to BudgetItemIconStyle(Icons.Filled.BeachAccess, Color(0xFF00ACC1)),
    BudgetItemIcon.TRIP to BudgetItemIconStyle(Icons.Filled.Flight, Color(0xFF1565C0)),
    BudgetItemIcon.GIFT to BudgetItemIconStyle(Icons.Filled.CardGiftcard, Color(0xFFC62828)),
    BudgetItemIcon.INSURANCE to BudgetItemIconStyle(Icons.Filled.Shield, Color(0xFF37474F)),
    BudgetItemIcon.CURRENCY_EXCHANGE to BudgetItemIconStyle(Icons.Filled.CurrencyExchange, Color(0xFFFFB300)),
    BudgetItemIcon.ELECTRONICS to BudgetItemIconStyle(Icons.Filled.Devices, Color(0xFF5E35B1)),
    BudgetItemIcon.PHONE to BudgetItemIconStyle(Icons.Filled.Smartphone, Color(0xFF00838F)),
    BudgetItemIcon.BILLS to BudgetItemIconStyle(Icons.Filled.Description, Color(0xFF6D4C41)),
    BudgetItemIcon.RENT to BudgetItemIconStyle(Icons.Filled.Home, Color(0xFF3949AB))
)
