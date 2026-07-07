package com.walley.app.data.local

import com.walley.app.domain.model.EquityNote
import com.walley.app.domain.model.WatchedEquity

fun WatchedEquityEntity.toDomain(): WatchedEquity = WatchedEquity(id = id, name = name, ticker = ticker)

fun EquityNoteEntity.toDomain(): EquityNote = EquityNote(
    id = id,
    equityId = equityId,
    date = date,
    status = status,
    note = note
)
