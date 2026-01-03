package com.flowfinance.app.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.flowfinance.app.data.local.entity.Category
import com.flowfinance.app.data.local.entity.Transaction

data class TransactionWithCategory(
    @Embedded val transaction: Transaction,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: Category
)
