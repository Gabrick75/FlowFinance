package com.flowfinance.app.data.repository

import com.flowfinance.app.data.local.dao.TransactionDao
import com.flowfinance.app.data.local.entity.Transaction
import com.flowfinance.app.data.local.model.CategorySummary
import com.flowfinance.app.util.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {
    fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions()
    }

    fun getTransactionsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(startDate, endDate)
    }

    fun getTotalAmountByTypeAndDateRange(type: TransactionType, startDate: LocalDate, endDate: LocalDate): Flow<Double?> {
        return transactionDao.getTotalAmountByTypeAndDateRange(type, startDate, endDate)
    }

    fun getCategorySummaryByTypeAndDateRange(type: TransactionType, startDate: LocalDate, endDate: LocalDate): Flow<List<CategorySummary>> {
        return transactionDao.getCategorySummaryByTypeAndDateRange(type, startDate, endDate)
    }

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }
}
