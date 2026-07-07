package com.walley.app.domain.model

import java.time.LocalDate

data class WatchedEquity(
    val id: Long = 0,
    val name: String,
    /** Optional, e.g. "AAPL"; always stored uppercase. */
    val ticker: String? = null
)

data class EquityNote(
    val id: Long = 0,
    val equityId: Long = 0,
    val date: LocalDate,
    val status: EquityStatus,
    val note: String
)

data class WatchedEquityWithNotes(
    val equity: WatchedEquity,
    /** In reverse-chronological order (most recent first). */
    val notes: List<EquityNote>
) {
    /** The most recently dated note's status, if any notes exist. */
    val latestStatus: EquityStatus? get() = notes.firstOrNull()?.status

    /** The status just before the latest one, if at least two notes exist. */
    val previousStatus: EquityStatus? get() = notes.getOrNull(1)?.status
}
