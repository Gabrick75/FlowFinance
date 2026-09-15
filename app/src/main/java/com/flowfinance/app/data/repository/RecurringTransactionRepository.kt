package com.flowfinance.app.data.repository

import com.flowfinance.app.data.local.dao.RecurringTransactionDao
import com.flowfinance.app.data.local.entity.RecurringTransaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringTransactionRepository @Inject constructor(
    private val recurringTransactionDao: RecurringTransactionDao
) {
    fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>> {
        return recurringTransactionDao.getAllRecurringTransactions()
    }

    suspend fun getActiveRecurringTransactions(): List<RecurringTransaction> {
        return recurringTransactionDao.getActiveRecurringTransactions()
    }

    suspend fun insertRecurringTransaction(recurringTransaction: RecurringTransaction) {
        recurringTransactionDao.insertRecurringTransaction(recurringTransaction)
    }

    suspend fun updateRecurringTransaction(recurringTransaction: RecurringTransaction) {
        recurringTransactionDao.updateRecurringTransaction(recurringTransaction)
    }

    suspend fun deleteRecurringTransaction(recurringTransaction: RecurringTransaction) {
        recurringTransactionDao.deleteRecurringTransaction(recurringTransaction)
    }
}
