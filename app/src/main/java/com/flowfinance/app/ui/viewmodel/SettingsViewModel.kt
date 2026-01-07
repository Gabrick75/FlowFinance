package com.flowfinance.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowfinance.app.data.local.model.TransactionWithCategory
import com.flowfinance.app.data.preferences.UserData
import com.flowfinance.app.data.preferences.UserPreferencesRepository
import com.flowfinance.app.data.repository.CategoryRepository
import com.flowfinance.app.data.repository.TransactionRepository
import com.flowfinance.app.util.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val userData: StateFlow<UserData> = userPreferencesRepository.userData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserData("Usuário", "BRL", false)
        )

    fun updateTheme(isDark: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDarkTheme(isDark)
        }
    }
    
    fun updateUserName(name: String) {
        viewModelScope.launch {
            userPreferencesRepository.setUserName(name)
        }
    }
    
    fun updateCurrency(currency: String) {
        viewModelScope.launch {
            userPreferencesRepository.setCurrency(currency)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            // First delete all transactions
            transactionRepository.deleteAllTransactions()
            // Then delete all custom categories
            categoryRepository.deleteAllCustomCategories()
        }
    }

    fun exportDataToCsv(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                // Fetch data
                val transactionsWithCategory = transactionRepository.getAllTransactionsWithCategory().first()
                val allCategories = categoryRepository.getAllCategories().first()
                
                val fileName = "flowfinance_export_${System.currentTimeMillis()}.xls"
                val file = File(context.getExternalFilesDir(null), fileName)
                
                FileWriter(file).use { writer ->
                     writer.write("<?xml version=\"1.0\"?>\n")
                     writer.write("<?mso-application progid=\"Excel.Sheet\"?>\n")
                     writer.write("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n")
                     writer.write(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n")
                     writer.write(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n")
                     writer.write(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\"\n")
                     writer.write(" xmlns:html=\"http://www.w3.org/TR/REC-html40\">\n")
                     
                     // Style for headers
                     writer.write(" <Styles>\n")
                     writer.write("  <Style ss:ID=\"Header\">\n")
                     writer.write("   <Font ss:Bold=\"1\"/>\n")
                     writer.write("  </Style>\n")
                     writer.write(" </Styles>\n")
                     
                     // 1. Sheet: Transações
                     writer.write(" <Worksheet ss:Name=\"Transacoes\">\n")
                     writer.write("  <Table>\n")
                     // Headers
                     writer.write("   <Row ss:StyleID=\"Header\">\n")
                     listOf("ID", "Descrição", "Valor", "Data", "Tipo", "Categoria").forEach {
                         writer.write("    <Cell><Data ss:Type=\"String\">$it</Data></Cell>\n")
                     }
                     writer.write("   </Row>\n")
                     
                     // Data
                     transactionsWithCategory.forEach { (transaction, category) ->
                         writer.write("   <Row>\n")
                         writer.write("    <Cell><Data ss:Type=\"Number\">${transaction.id}</Data></Cell>\n")
                         writer.write("    <Cell><Data ss:Type=\"String\">${escapeXml(transaction.description)}</Data></Cell>\n")
                         writer.write("    <Cell><Data ss:Type=\"Number\">${transaction.amount}</Data></Cell>\n")
                         writer.write("    <Cell><Data ss:Type=\"String\">${transaction.date}</Data></Cell>\n")
                         writer.write("    <Cell><Data ss:Type=\"String\">${transaction.type}</Data></Cell>\n")
                         writer.write("    <Cell><Data ss:Type=\"String\">${escapeXml(category.name)}</Data></Cell>\n")
                         writer.write("   </Row>\n")
                     }
                     writer.write("  </Table>\n")
                     writer.write(" </Worksheet>\n")
                     
                     // 2. Sheet: Categorias
                     writer.write(" <Worksheet ss:Name=\"Categorias\">\n")
                     writer.write("  <Table>\n")
                     // Headers
                     writer.write("   <Row ss:StyleID=\"Header\">\n")
                     listOf("ID", "Nome", "Orçamento Mensal", "Padrão").forEach {
                         writer.write("    <Cell><Data ss:Type=\"String\">$it</Data></Cell>\n")
                     }
                     writer.write("   </Row>\n")
                     
                     allCategories.forEach { category ->
                         writer.write("   <Row>\n")
                         writer.write("    <Cell><Data ss:Type=\"Number\">${category.id}</Data></Cell>\n")
                         writer.write("    <Cell><Data ss:Type=\"String\">${escapeXml(category.name)}</Data></Cell>\n")
                         val budget = category.budgetLimit?.toString() ?: ""
                         writer.write("    <Cell><Data ss:Type=\"String\">$budget</Data></Cell>\n")
                         val isDefault = if (category.isDefault) "Sim" else "Não"
                         writer.write("    <Cell><Data ss:Type=\"String\">$isDefault</Data></Cell>\n")
                         writer.write("   </Row>\n")
                     }
                     writer.write("  </Table>\n")
                     writer.write(" </Worksheet>\n")

                     // 3. Sheet: Fluxo Financeiro
                     writer.write(" <Worksheet ss:Name=\"Fluxo Financeiro\">\n")
                     writer.write("  <Table>\n")
                     writer.write("   <Row ss:StyleID=\"Header\">\n")
                     listOf("Data", "Salário", "Rendimento Mensal", "Rendimento Acumulado", "Saldo Acumulado", "Patrimônio Total").forEach {
                         writer.write("    <Cell><Data ss:Type=\"String\">$it</Data></Cell>\n")
                     }
                     writer.write("   </Row>\n")

                     if (transactionsWithCategory.isNotEmpty()) {
                        val sortedTransactions = transactionsWithCategory.sortedBy { it.transaction.date }
                        val startMonth = YearMonth.from(sortedTransactions.first().transaction.date)
                        val endMonth = YearMonth.now()
                        
                        var currentMonth = startMonth
                        var runningAccumulatedYield = 0.0
                        var runningAccumulatedBalance = 0.0
                        var runningTotalWealth = 0.0

                        while (!currentMonth.isAfter(endMonth)) {
                            val monthTransactions = sortedTransactions.filter { 
                                YearMonth.from(it.transaction.date) == currentMonth 
                            }

                            val salary = monthTransactions
                                .filter { it.transaction.type == TransactionType.INCOME && !it.category.name.equals("Rendimentos", ignoreCase = true) }
                                .sumOf { it.transaction.amount }

                            val monthlyYield = monthTransactions
                                .filter { it.category.name.equals("Rendimentos", ignoreCase = true) && it.transaction.type == TransactionType.INCOME }
                                .sumOf { it.transaction.amount }

                            val totalIncomeOfMonth = salary + monthlyYield

                            val totalExpenseOfMonth = monthTransactions
                                .filter { it.transaction.type == TransactionType.EXPENSE }
                                .sumOf { it.transaction.amount }

                            runningAccumulatedYield += monthlyYield
                            runningAccumulatedBalance += (totalIncomeOfMonth - totalExpenseOfMonth)
                            runningTotalWealth += totalIncomeOfMonth

                            val dateStr = currentMonth.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale("pt", "BR")))
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

                            writer.write("   <Row>\n")
                            writer.write("    <Cell><Data ss:Type=\"String\">$dateStr</Data></Cell>\n")
                            writer.write("    <Cell><Data ss:Type=\"Number\">$salary</Data></Cell>\n")
                            writer.write("    <Cell><Data ss:Type=\"Number\">$monthlyYield</Data></Cell>\n")
                            writer.write("    <Cell><Data ss:Type=\"Number\">$runningAccumulatedYield</Data></Cell>\n")
                            writer.write("    <Cell><Data ss:Type=\"Number\">$runningAccumulatedBalance</Data></Cell>\n")
                            writer.write("    <Cell><Data ss:Type=\"Number\">$runningTotalWealth</Data></Cell>\n")
                            writer.write("   </Row>\n")

                            currentMonth = currentMonth.plusMonths(1)
                        }
                     }
                     writer.write("  </Table>\n")
                     writer.write(" </Worksheet>\n")

                     // 4. Sheet: Tendência por Categoria
                     writer.write(" <Worksheet ss:Name=\"Tendencia por Categoria\">\n")
                     writer.write("  <Table>\n")
                     
                     // Filter categories as per Trend screen logic
                     val trendCategories = allCategories.filter { 
                        it.name != "Salário" && it.name != "Investimentos" && it.name != "Rendimentos"
                     }

                     // Headers
                     writer.write("   <Row ss:StyleID=\"Header\">\n")
                     writer.write("    <Cell><Data ss:Type=\"String\">Data</Data></Cell>\n")
                     trendCategories.forEach { category ->
                         writer.write("    <Cell><Data ss:Type=\"String\">${escapeXml(category.name)}</Data></Cell>\n")
                     }
                     writer.write("   </Row>\n")

                     if (transactionsWithCategory.isNotEmpty()) {
                         val groupedByMonth = transactionsWithCategory.groupBy { 
                            YearMonth.from(it.transaction.date) 
                         }.toSortedMap()

                         groupedByMonth.forEach { (month, monthTransactions) ->
                            val expensesMap = mutableMapOf<String, Double>()
                            trendCategories.forEach { expensesMap[it.name] = 0.0 }

                            monthTransactions
                                .filter { it.transaction.type == TransactionType.EXPENSE }
                                .forEach { 
                                    if (expensesMap.containsKey(it.category.name)) {
                                        expensesMap[it.category.name] = expensesMap.getOrDefault(it.category.name, 0.0) + it.transaction.amount
                                    }
                                }

                            val dateStr = month.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale("pt", "BR")))
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

                            writer.write("   <Row>\n")
                            writer.write("    <Cell><Data ss:Type=\"String\">$dateStr</Data></Cell>\n")
                            trendCategories.forEach { category ->
                                val amount = expensesMap[category.name] ?: 0.0
                                writer.write("    <Cell><Data ss:Type=\"Number\">$amount</Data></Cell>\n")
                            }
                            writer.write("   </Row>\n")
                         }
                     }
                     writer.write("  </Table>\n")
                     writer.write(" </Worksheet>\n")

                     // 5. Sheet: Resumo Financeiro
                     writer.write(" <Worksheet ss:Name=\"Resumo Financeiro\">\n")
                     writer.write("  <Table>\n")
                     writer.write("   <Row ss:StyleID=\"Header\">\n")
                     listOf("Período", "Receita Total", "Despesa Total", "Saldo (Não Gasto)").forEach {
                         writer.write("    <Cell><Data ss:Type=\"String\">$it</Data></Cell>\n")
                     }
                     writer.write("   </Row>\n")

                     // Calculate summaries
                     val currentDate = LocalDate.now()
                     val currentYear = currentDate.year
                     val currentMonth = YearMonth.from(currentDate)
                     
                     // Helper for summary
                     fun getSummary(txs: List<TransactionWithCategory>): Triple<Double, Double, Double> {
                         var income = 0.0
                         var expense = 0.0
                         txs.forEach { 
                             if (it.transaction.type == TransactionType.INCOME) income += it.transaction.amount 
                             else expense += it.transaction.amount 
                         }
                         return Triple(income, expense, income - expense)
                     }

                     // Total
                     val total = getSummary(transactionsWithCategory)
                     writer.write("   <Row>\n")
                     writer.write("    <Cell><Data ss:Type=\"String\">Total (Desde o Início)</Data></Cell>\n")
                     writer.write("    <Cell><Data ss:Type=\"Number\">${total.first}</Data></Cell>\n")
                     writer.write("    <Cell><Data ss:Type=\"Number\">${total.second}</Data></Cell>\n")
                     writer.write("    <Cell><Data ss:Type=\"Number\">${total.third}</Data></Cell>\n")
                     writer.write("   </Row>\n")

                     // Annual
                     val annualTxs = transactionsWithCategory.filter { it.transaction.date.year == currentYear }
                     val annual = getSummary(annualTxs)
                     writer.write("   <Row>\n")
                     writer.write("    <Cell><Data ss:Type=\"String\">Ano Atual ($currentYear)</Data></Cell>\n")
                     writer.write("    <Cell><Data ss:Type=\"Number\">${annual.first}</Data></Cell>\n")
                     writer.write("    <Cell><Data ss:Type=\"Number\">${annual.second}</Data></Cell>\n")
                     writer.write("    <Cell><Data ss:Type=\"Number\">${annual.third}</Data></Cell>\n")
                     writer.write("   </Row>\n")

                     // Monthly
                     val monthlyTxs = transactionsWithCategory.filter { YearMonth.from(it.transaction.date) == currentMonth }
                     val monthly = getSummary(monthlyTxs)
                     val monthStr = currentMonth.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale("pt", "BR")))
                         .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                     writer.write("   <Row>\n")
                     writer.write("    <Cell><Data ss:Type=\"String\">Mês Atual ($monthStr)</Data></Cell>\n")
                     writer.write("    <Cell><Data ss:Type=\"Number\">${monthly.first}</Data></Cell>\n")
                     writer.write("    <Cell><Data ss:Type=\"Number\">${monthly.second}</Data></Cell>\n")
                     writer.write("    <Cell><Data ss:Type=\"Number\">${monthly.third}</Data></Cell>\n")
                     writer.write("   </Row>\n")

                     writer.write("  </Table>\n")
                     writer.write(" </Worksheet>\n")
                     
                     writer.write("</Workbook>\n")
                }
                
                onResult(file.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null)
            }
        }
    }

    private fun escapeXml(s: String): String {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;")
    }
}
