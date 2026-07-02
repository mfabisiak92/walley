package com.walley.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Query("UPDATE accounts SET name = :name, balanceMinorUnits = :balanceMinorUnits WHERE id = :accountId")
    suspend fun update(accountId: Long, name: String, balanceMinorUnits: Long)

    @Query("DELETE FROM accounts WHERE id = :accountId")
    suspend fun delete(accountId: Long)
}
