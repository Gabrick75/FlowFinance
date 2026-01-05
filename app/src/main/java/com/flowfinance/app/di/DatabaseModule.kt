package com.flowfinance.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.flowfinance.app.data.local.AppDatabase
import com.flowfinance.app.data.local.dao.CategoryDao
import com.flowfinance.app.data.local.dao.TransactionDao
import com.flowfinance.app.data.local.entity.Category
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        categoryDaoProvider: Provider<CategoryDao>
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "flowfinance_db"
        )
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Pre-populate categories
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = categoryDaoProvider.get()
                    val initialCategories = listOf(
                        Category(name = "Alimentação", color = 0xFFEF5350.toInt(), icon = "restaurant", isDefault = true),
                        Category(name = "Lazer", color = 0xFF42A5F5.toInt(), icon = "attractions", isDefault = true),
                        Category(name = "Transporte", color = 0xFFFFA726.toInt(), icon = "commute", isDefault = true),
                        Category(name = "Saúde", color = 0xFF66BB6A.toInt(), icon = "health_and_safety", isDefault = true),
                        Category(name = "Educação", color = 0xFFAB47BC.toInt(), icon = "school", isDefault = true),
                        Category(name = "Salário", color = 0xFF26A69A.toInt(), icon = "payments", isDefault = true),
                        Category(name = "Investimentos", color = 0xFF7E57C2.toInt(), icon = "trending_up", isDefault = true),
                        Category(name = "Rendimentos", color = 0xFF4CAF50.toInt(), icon = "trending_up", isDefault = true)
                    )
                    initialCategories.forEach { dao.insertCategory(it) }
                }
            }
        })
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideTransactionDao(database: AppDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        return database.categoryDao()
    }
}
