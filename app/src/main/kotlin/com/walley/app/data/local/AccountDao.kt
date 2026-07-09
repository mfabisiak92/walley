package com.walley.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.walley.app.domain.model.AccountTaxRate
import com.walley.app.domain.model.AccountType
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Query(
        """
        UPDATE accounts
        SET name = :name, type = :type, taxRate = :taxRate, balanceMinorUnits = :balanceMinorUnits,
            targetAmountMinorUnits = :targetAmountMinorUnits
        WHERE id = :accountId
        """
    )
    suspend fun update(
        accountId: Long,
        name: String,
        type: AccountType,
        taxRate: AccountTaxRate,
        balanceMinorUnits: Long,
        targetAmountMinorUnits: Long?
    )

    @Query("DELETE FROM accounts WHERE id = :accountId")
    suspend fun delete(accountId: Long)

    @Query("UPDATE accounts SET balanceMinorUnits = balanceMinorUnits + :deltaMinorUnits WHERE id = :accountId")
    suspend fun addToBalance(accountId: Long, deltaMinorUnits: Long)

    @Query("UPDATE accounts SET balanceMinorUnits = :balanceMinorUnits WHERE id = :accountId")
    suspend fun updateBalance(accountId: Long, balanceMinorUnits: Long)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM accounts WHERE isDefault = 1")
    suspend fun countDefault(): Int

    @Query("SELECT id FROM accounts ORDER BY id ASC LIMIT 1")
    suspend fun firstAccountId(): Long?

    @Query("UPDATE accounts SET isDefault = CASE WHEN id = :accountId THEN 1 ELSE 0 END")
    suspend fun setDefaultAccount(accountId: Long)
}
