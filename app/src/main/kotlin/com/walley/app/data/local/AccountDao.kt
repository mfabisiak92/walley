package com.walley.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.walley.app.domain.model.AccountTaxRate
import com.walley.app.domain.model.AccountType
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Insert
    suspend fun insertAll(accounts: List<AccountEntity>): List<Long>

    @Query(
        """
        UPDATE accounts
        SET name = :name, type = :type, taxRate = :taxRate, balanceMinorUnits = :balanceMinorUnits,
            targetAmountMinorUnits = :targetAmountMinorUnits, targetDate = :targetDate,
            commissionFlatMinorUnits = :commissionFlatMinorUnits,
            commissionPercent = :commissionPercent, isVirtual = :isVirtual
        WHERE id = :accountId
        """
    )
    suspend fun update(
        accountId: Long,
        name: String,
        type: AccountType,
        taxRate: AccountTaxRate,
        balanceMinorUnits: Long,
        targetAmountMinorUnits: Long?,
        targetDate: LocalDate?,
        commissionFlatMinorUnits: Long,
        commissionPercent: BigDecimal,
        isVirtual: Boolean
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

    @Query("SELECT * FROM accounts WHERE id = :accountId")
    suspend fun getById(accountId: Long): AccountEntity?

    @Query("UPDATE accounts SET isClosed = :isClosed WHERE id = :accountId")
    suspend fun setClosed(accountId: Long, isClosed: Boolean)

    @Query("SELECT id FROM accounts WHERE isClosed = 0 ORDER BY id ASC LIMIT 1")
    suspend fun firstOpenAccountId(): Long?
}
