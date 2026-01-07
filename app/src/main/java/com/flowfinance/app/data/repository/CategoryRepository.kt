package com.flowfinance.app.data.repository

import com.flowfinance.app.data.local.dao.CategoryDao
import com.flowfinance.app.data.local.entity.Category
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories()
    }

    suspend fun getCategoryById(id: Int): Category? {
        return categoryDao.getCategoryById(id)
    }

    suspend fun insertCategory(category: Category) {
        categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }

    suspend fun deleteAllCustomCategories() {
        categoryDao.deleteAllCustomCategories()
    }

    suspend fun deleteAllCategories() {
        categoryDao.deleteAllCategories()
    }
}
