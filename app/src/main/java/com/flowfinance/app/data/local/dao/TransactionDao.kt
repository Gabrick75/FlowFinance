package com.flowfinance.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flowfinance.app.data.local.entity.Transaction
import com.flowfinance.app.data.local.model.CategorySummary
import com.flowfinance.app.util.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate")
    fun getTotalAmountByTypeAndDateRange(type: TransactionType, startDate: LocalDate, endDate: LocalDate): Flow<Double?>

    @Query("""
        SELECT c.*, SUM(t.amount) as totalAmount 
        FROM categories c 
        JOIN transactions t ON c.id = t.categoryId 
        WHERE t.type = :type AND t.date BETWEEN :startDate AND :endDate
        GROUP BY c.id
    """)
    fun getCategorySummaryByTypeAndDateRange(type: TransactionType, startDate: LocalDate, endDate: LocalDate): Flow<List<CategorySummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
}
