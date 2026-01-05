package com.flowfinance.app.data.local.model

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import com.flowfinance.app.data.local.entity.Category

@Immutable
data class CategorySummary(
    @Embedded val category: Category,
    val totalAmount: Double
)
