package com.walley.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetItemIconSuggestionTest {

    @Test
    fun `matches english keyword`() {
        assertEquals(BudgetItemIcon.GROCERIES, suggestIconForName("Weekly groceries"))
    }

    @Test
    fun `matches polish keyword`() {
        assertEquals(BudgetItemIcon.ELECTRICITY, suggestIconForName("Rachunek za prąd"))
    }

    @Test
    fun `matches polish keyword without diacritics`() {
        assertEquals(BudgetItemIcon.ELECTRICITY, suggestIconForName("Rachunek za prad"))
        assertEquals(BudgetItemIcon.TRIP, suggestIconForName("Podroz do Krakowa"))
        assertEquals(BudgetItemIcon.CAR, suggestIconForName("Moj samochod"))
    }

    @Test
    fun `matches polish keyword with diacritics typed correctly too`() {
        assertEquals(BudgetItemIcon.ELECTRICITY, suggestIconForName("Rachunek za prąd"))
        assertEquals(BudgetItemIcon.CAR, suggestIconForName("Mój samochód"))
    }

    @Test
    fun `matches polish multi-word keyword`() {
        assertEquals(BudgetItemIcon.RENT, suggestIconForName("Rata kredytu hipotecznego, kredyt hipoteczny"))
    }

    @Test
    fun `matches is case insensitive`() {
        assertEquals(BudgetItemIcon.CAR, suggestIconForName("CAR SERVICE"))
    }

    @Test
    fun `does not match substrings within other words`() {
        assertNull(suggestIconForName("Cartel membership"))
    }

    @Test
    fun `movie resolves to movie icon not entertainment`() {
        assertEquals(BudgetItemIcon.MOVIE, suggestIconForName("Cinema night"))
    }

    @Test
    fun `doctor resolves to doctor icon not the more generic health`() {
        assertEquals(BudgetItemIcon.DOCTOR, suggestIconForName("Doctor appointment"))
        assertEquals(BudgetItemIcon.DOCTOR, suggestIconForName("Wizyta u lekarza, wizyta lekarska"))
        assertEquals(BudgetItemIcon.HEALTH, suggestIconForName("General health checkup"))
    }

    @Test
    fun `matches new english and polish keywords for parking, ship, and shipment`() {
        assertEquals(BudgetItemIcon.PARKING, suggestIconForName("Downtown parking"))
        assertEquals(BudgetItemIcon.PARKING, suggestIconForName("Postój w centrum"))
        assertEquals(BudgetItemIcon.SHIP, suggestIconForName("Ferry to the island"))
        assertEquals(BudgetItemIcon.SHIP, suggestIconForName("Rejs statkiem"))
        assertEquals(BudgetItemIcon.SHIPMENT, suggestIconForName("Package delivery"))
        assertEquals(BudgetItemIcon.SHIPMENT, suggestIconForName("Wysyłka paczki"))
    }

    @Test
    fun `returns null when nothing matches`() {
        assertNull(suggestIconForName("Xyzabc"))
    }
}
