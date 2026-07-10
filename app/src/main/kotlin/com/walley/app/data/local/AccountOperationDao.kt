package com.walley.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountOperationDao {
    @Query("SELECT * FROM account_operations ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<AccountOperationEntity>>

    @Query("SELECT * FROM account_operations WHERE accountId = :accountId ORDER BY date DESC, id DESC")
    fun observeForAccount(accountId: Long): Flow<List<AccountOperationEntity>>

    @Insert
    suspend fun insert(operation: AccountOperationEntity): Long

    @Query("DELETE FROM account_operations WHERE accountId = :accountId")
    suspend fun deleteForAccount(accountId: Long)
}
