package com.walley.app.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tv
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
    BudgetItemIcon.RENT to BudgetItemIconStyle(Icons.Filled.Home, Color(0xFF3949AB)),
    BudgetItemIcon.PIGGY_BANK to BudgetItemIconStyle(Icons.Filled.Savings, Color(0xFFD81B60)),
    BudgetItemIcon.ELECTRICITY to BudgetItemIconStyle(Icons.Filled.Bolt, Color(0xFFF9A825)),
    BudgetItemIcon.INTERNET to BudgetItemIconStyle(Icons.Filled.AlternateEmail, Color(0xFF0277BD)),
    BudgetItemIcon.TV to BudgetItemIconStyle(Icons.Filled.Tv, Color(0xFF546E7A)),
    BudgetItemIcon.MUSIC to BudgetItemIconStyle(Icons.Filled.MusicNote, Color(0xFF8E24AA)),
    BudgetItemIcon.BARBER to BudgetItemIconStyle(Icons.Filled.ContentCut, Color(0xFF8D6E63)),
    BudgetItemIcon.GAMES to BudgetItemIconStyle(Icons.Filled.SportsEsports, Color(0xFF7C4DFF)),
    BudgetItemIcon.SPORT to BudgetItemIconStyle(Icons.Filled.SportsSoccer, Color(0xFF43A047)),
    BudgetItemIcon.BOOKS to BudgetItemIconStyle(Icons.Filled.MenuBook, Color(0xFF3F51B5)),
    BudgetItemIcon.REPAIR to BudgetItemIconStyle(Icons.Filled.Handyman, Color(0xFFFF7043)),
    BudgetItemIcon.HEALTH to BudgetItemIconStyle(Icons.Filled.HealthAndSafety, Color(0xFFE53935)),
    BudgetItemIcon.MEDICINE to BudgetItemIconStyle(Icons.Filled.Medication, Color(0xFF26A69A)),
    BudgetItemIcon.PET to BudgetItemIconStyle(Icons.Filled.Pets, Color(0xFFFB8C00)),
    BudgetItemIcon.MOVIE to BudgetItemIconStyle(Icons.Filled.Movie, Color(0xFF7B1FA2)),
    BudgetItemIcon.KIDS to BudgetItemIconStyle(Icons.Filled.ChildCare, Color(0xFFF06292)),
    BudgetItemIcon.RETIREMENT to BudgetItemIconStyle(Icons.Filled.Elderly, Color(0xFF607D8B)),
    BudgetItemIcon.PARKING to BudgetItemIconStyle(Icons.Filled.LocalParking, Color(0xFF1E88E5)),
    BudgetItemIcon.SHIP to BudgetItemIconStyle(Icons.Filled.DirectionsBoat, Color(0xFF00796B)),
    BudgetItemIcon.SHIPMENT to BudgetItemIconStyle(Icons.Filled.LocalShipping, Color(0xFF5D4037)),
    BudgetItemIcon.DOCTOR to BudgetItemIconStyle(Icons.Filled.MedicalServices, Color(0xFFD32F2F))
)
