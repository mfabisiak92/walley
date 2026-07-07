package com.walley.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watched_equities")
data class WatchedEquityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ticker: String?
)
