package com.flowfinance.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.flowfinance.app.data.local.converters.Converters
import com.flowfinance.app.data.local.dao.CategoryDao
import com.flowfinance.app.data.local.dao.TransactionDao
import com.flowfinance.app.data.local.entity.Category
import com.flowfinance.app.data.local.entity.Transaction

@Database(entities = [Transaction::class, Category::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
}
