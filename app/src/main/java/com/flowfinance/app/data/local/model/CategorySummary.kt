package com.flowfinance.app.data.local.model

import androidx.room.Embedded
import com.flowfinance.app.data.local.entity.Category

data class CategorySummary(
    @Embedded val category: Category,
    val totalAmount: Double
)
