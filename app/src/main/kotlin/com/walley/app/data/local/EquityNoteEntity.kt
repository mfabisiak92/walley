package com.walley.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.walley.app.domain.model.EquityStatus
import java.time.LocalDate

@Entity(tableName = "equity_notes")
data class EquityNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val equityId: Long,
    val date: LocalDate,
    val status: EquityStatus,
    val note: String
)
