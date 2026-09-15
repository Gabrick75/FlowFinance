package com.flowfinance.app.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flowfinance.app.util.RecurrenceFrequency
import com.flowfinance.app.util.TransactionType
import java.time.LocalDate

@Immutable
@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Int,
    val frequency: RecurrenceFrequency,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val occurrencesGenerated: Int = 0,
    val isActive: Boolean = true
)
