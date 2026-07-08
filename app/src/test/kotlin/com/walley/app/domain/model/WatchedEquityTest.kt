package com.walley.app.domain.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WatchedEquityTest {

    private fun note(status: EquityStatus) = EquityNote(date = LocalDate.now(), status = status, note = "")

    @Test
    fun `latestStatus and previousStatus are null with no notes`() {
        val equity = WatchedEquityWithNotes(WatchedEquity(name = "Acme"), notes = emptyList())
        assertNull(equity.latestStatus)
        assertNull(equity.previousStatus)
    }

    @Test
    fun `previousStatus is null with only one note`() {
        val equity = WatchedEquityWithNotes(WatchedEquity(name = "Acme"), notes = listOf(note(EquityStatus.BUY)))
        assertEquals(EquityStatus.BUY, equity.latestStatus)
        assertNull(equity.previousStatus)
    }

    @Test
    fun `latestStatus is the first (most recent) note when several exist`() {
        // notes are stored most-recent-first
        val equity = WatchedEquityWithNotes(
            WatchedEquity(name = "Acme"),
            notes = listOf(note(EquityStatus.SELL), note(EquityStatus.HOLD), note(EquityStatus.WAIT))
        )
        assertEquals(EquityStatus.SELL, equity.latestStatus)
        assertEquals(EquityStatus.HOLD, equity.previousStatus)
    }
}
