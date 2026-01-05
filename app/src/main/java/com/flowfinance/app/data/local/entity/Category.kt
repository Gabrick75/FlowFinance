package com.flowfinance.app.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val color: Int, // Color int
    val icon: String? = null,
    val budgetLimit: Double? = null, // Optional monthly budget for this category
    val isDefault: Boolean = false // Flag to identify default categories
)
