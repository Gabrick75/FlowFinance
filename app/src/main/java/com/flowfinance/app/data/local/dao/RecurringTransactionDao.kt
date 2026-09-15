package com.flowfinance.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flowfinance.app.data.local.entity.RecurringTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions ORDER BY isActive DESC, description ASC")
    fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1")
    suspend fun getActiveRecurringTransactions(): List<RecurringTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTransaction(recurringTransaction: RecurringTransaction)

    @Update
    suspend fun updateRecurringTransaction(recurringTransaction: RecurringTransaction)

    @Delete
    suspend fun deleteRecurringTransaction(recurringTransaction: RecurringTransaction)
}
