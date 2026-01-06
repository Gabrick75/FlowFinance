package com.flowfinance.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowfinance.app.data.local.entity.Category
import com.flowfinance.app.data.local.model.CategorySummary
import com.flowfinance.app.data.preferences.UserPreferencesRepository
import com.flowfinance.app.data.repository.CategoryRepository
import com.flowfinance.app.data.repository.TransactionRepository
import com.flowfinance.app.util.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class CategoryTrendsUiState(
    val monthlyData: List<MonthlyExpenseData> = emptyList(),
    val currency: String = "BRL",
    val isLoading: Boolean = false,
    val categories: List<Category> = emptyList(),
    val totalExpensesByCategory: List<CategorySummary> = emptyList() // For overall pie chart
)

data class MonthlyExpenseData(
    val yearMonth: YearMonth,
    val expensesByCategory: Map<String, Double>, // Nome da categoria -> Valor
    val totalExpenses: Double, // Total expenses for the month
    val categorySummaries: List<CategorySummary> // For monthly pie chart
)

@HiltViewModel
class CategoryTrendsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryTrendsUiState(isLoading = true))
    val uiState: StateFlow<CategoryTrendsUiState> = _uiState

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val userData = userPreferencesRepository.userData.stateIn(viewModelScope).value
            val categories = categoryRepository.getAllCategories().first().filter { 
                it.name != "Salário" && it.name != "Investimentos" && it.name != "Rendimentos"
            }
            
            val transactions = transactionRepository.getAllTransactionsWithCategory().first()
            
            if (transactions.isEmpty()) {
                _uiState.value = CategoryTrendsUiState(currency = userData.currency, isLoading = false, categories = categories)
                return@launch
            }

            // 1. Calculate Overall Total Expenses by Category (Since forever)
            val overallExpensesMap = mutableMapOf<Int, Double>() // CategoryId -> Total
            val categoryMap = categories.associateBy { it.id }

            transactions.filter { it.transaction.type == TransactionType.EXPENSE }.forEach { 
                val catId = it.transaction.categoryId
                if (categoryMap.containsKey(catId)) {
                    val current = overallExpensesMap.getOrDefault(catId, 0.0)
                    overallExpensesMap[catId] = current + it.transaction.amount
                }
            }

            val totalExpensesByCategory = overallExpensesMap.mapNotNull { (catId, amount) ->
                categoryMap[catId]?.let { category ->
                    CategorySummary(category, amount)
                }
            }.sortedByDescending { it.totalAmount }

            // 2. Group by Month for Trends
            val groupedByMonth = transactions.groupBy { 
                YearMonth.from(it.transaction.date) 
            }.toSortedMap(reverseOrder()) // Sort descending (newest first) but lists often need ascending for charts

            val monthlyDataList = mutableListOf<MonthlyExpenseData>()

            groupedByMonth.forEach { (month, monthTransactions) ->
                val expensesMap = mutableMapOf<String, Double>()
                val monthlyCategoryMap = mutableMapOf<Int, Double>()
                var monthlyTotal = 0.0
                
                // Initialize chart data with 0
                categories.forEach { category ->
                    expensesMap[category.name] = 0.0
                }
                
                val expenseTransactions = monthTransactions.filter { it.transaction.type == TransactionType.EXPENSE }
                
                expenseTransactions.forEach { 
                    val category = it.category
                    // Skip income categories if any slipped through filter
                    if (category.name != "Salário" && category.name != "Investimentos" && category.name != "Rendimentos") {
                        val categoryName = category.name
                        val currentAmount = expensesMap.getOrDefault(categoryName, 0.0)
                        expensesMap[categoryName] = currentAmount + it.transaction.amount
                        
                        val catId = category.id
                        val currentCatAmount = monthlyCategoryMap.getOrDefault(catId, 0.0)
                        monthlyCategoryMap[catId] = currentCatAmount + it.transaction.amount
                        
                        monthlyTotal += it.transaction.amount
                    }
                }

                val monthlySummaries = monthlyCategoryMap.mapNotNull { (catId, amount) ->
                     categoryMap[catId]?.let { category ->
                        CategorySummary(category, amount)
                    }
                }.sortedByDescending { it.totalAmount }
                
                monthlyDataList.add(MonthlyExpenseData(
                    yearMonth = month, 
                    expensesByCategory = expensesMap,
                    totalExpenses = monthlyTotal,
                    categorySummaries = monthlySummaries
                ))
            }

            // Re-sort monthly data ascending for line/area charts if needed, or keep descending for lists
            // Let's keep it sorted by date ascending for charts
            val sortedMonthlyData = monthlyDataList.sortedBy { it.yearMonth }

            _uiState.value = CategoryTrendsUiState(
                monthlyData = sortedMonthlyData,
                currency = userData.currency,
                isLoading = false,
                categories = categories,
                totalExpensesByCategory = totalExpensesByCategory
            )
        }
    }

    fun exportSheetToCsv(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val state = uiState.value
                val fileName = "tendencia_categorias_${System.currentTimeMillis()}.csv"
                val file = File(context.getExternalFilesDir(null), fileName)

                FileWriter(file).use { writer ->
                    // Cabeçalho
                    writer.append("Data")
                    state.categories.forEach { category ->
                        writer.append(",${category.name}")
                    }
                    writer.append("\n")

                    // Linhas
                    state.monthlyData.sortedByDescending { it.yearMonth }.forEach { data ->
                        val dateStr = data.yearMonth.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale("pt", "BR")))
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                        
                        writer.append(dateStr)
                        
                        state.categories.forEach { category ->
                            val amount = data.expensesByCategory[category.name] ?: 0.0
                            writer.append(",$amount")
                        }
                        writer.append("\n")
                    }
                }
                onResult(file.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null)
            }
        }
    }
}
