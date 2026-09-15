# FlowFinance — ViewModels

Local: `app/src/main/java/com/flowfinance/app/ui/viewmodel/` · Todos são `@HiltViewModel` com injeção por construtor.

## Tabela de referência

| ViewModel | Alimenta | Carregamento | Mutações |
|-----------|----------|--------------|----------|
| `DashboardViewModel` | DashboardScreen | Reativo | — |
| `TransactionsViewModel` | TransactionsScreen | Reativo | navegação de mês, busca, excluir |
| `PlanningViewModel` | PlanningScreen | Reativo | — |
| `SettingsViewModel` | SettingsScreen, UserProfileScreen | Reativo (prefs) + jobs pontuais | tema/nome/moeda/idioma, lembretes, limpar tudo, exportar/importar |
| `ManageBudgetsViewModel` | ManageBudgetsScreen | Reativo + SharedFlow de resultado | updateBudget, excluir/criar/atualizar categoria |
| `EditTransactionViewModel` | **nenhuma (órfão)** | Reativo | atualizar/excluir transação |
| `MonthlyHistoryViewModel` | MonthlyHistoryScreen | Pontual (init) | — |
| `FinancialFlowViewModel` | FinancialFlowScreen, SheetScreen, FullScreenChart (financeiro), PanelScreen | Reativo | exportar CSV |
| `CategoryTrendsViewModel` | CategoryTrendsScreen, CategoryTrendsSheetScreen, FullScreenChart (categoria) | Pontual (init) | exportar CSV |
| `FinancialSummaryViewModel` | FinancialSummaryScreen, PanelScreen | Reativo | — |
| `ExpenseAnalysisViewModel` | ExpenseAnalysisScreen | Pontual (init) | — |
| `PatternsAnalysisViewModel` | **nenhuma (placeholder em branco)** | — | — |
| `AddTransactionViewModel` | AddTransactionSheet | Reativo (listas de categorias) | saveTransaction |

Convenções compartilhadas:
- Estado de UI = `data class` imutável dentro de um `StateFlow` com padrões seguros (assim `collectAsState()` renderiza imediatamente).
- Agregações calculadas no ViewModel a partir dos `Flow` dos repositórios; room emite → combine → recalcula.
- Mutações de despesa enfileiram um `NotificationWorker` pontual (`TYPE_BUDGET_CHECK`) para atualizar alertas de orçamento.

---

## 1. DashboardViewModel
- **Injeta:** `TransactionRepository`, `UserPreferencesRepository`, `CategoryRepository` (injetado mas não usado).
- **Estado:** `uiState: StateFlow<DashboardUiState>` onde `DashboardUiState(totalBalance, monthlyIncome, monthlyExpense, recentTransactions: List<TransactionWithCategory>, currency)`; além de `chartData: StateFlow<List<CategorySummary>>` (pizza de despesas do mês atual).
- **Lógica:** combina o `YearMonth` atual, o fluxo completo de transações e os dados do usuário:
  - `monthlyIncome/Expense` = somas filtradas por `TransactionType` dentro do mês.
  - `totalBalance` = receitas − despesas de todos os tempos.
  - `recentTransactions` = primeiras 5 (o DAO já retorna por data DESC).
  - `chartData` = `flatMapLatest { getCategorySummaryByTypeAndDateRange(EXPENSE, ...) }`.
  - Ambos os flows com `WhileSubscribed(5000)`.

## 2. TransactionsViewModel
- **Injeta:** `TransactionRepository`, `UserPreferencesRepository`.
- **Estado:** `TransactionsUiState(currentMonth: YearMonth, searchQuery, transactionsByDate: Map<LocalDate, List<TransactionWithCategory>>, currency)`.
- **Funções:** `nextMonth()`, `previousMonth()`, `onSearchQueryChange()`, `deleteTransaction()`.
- **Lógica:** combina mês + busca + fluxo de transações + prefs; filtra pelo mês selecionado, correspondência opcional sem diferenciar maiúsculas em `description` ou `category.name`, agrupa por data, ordena datas decrescentes.

## 3. PlanningViewModel
- **Injeta:** `TransactionRepository`, `UserPreferencesRepository`.
- **Estado:** `PlanningUiState(categorySpendings: List<CategorySummary>, currency)`.
- **Lógica:** totais de despesa do mês atual por categoria (`getCategorySummaryByTypeAndDateRange(EXPENSE)`), fixo em `YearMonth.now()`. Somente leitura.

## 4. SettingsViewModel (o maior)
- **Injeta:** `TransactionRepository`, `CategoryRepository`, `UserPreferencesRepository`, `@ApplicationContext Context`.
- **Estado:** `userData: StateFlow<UserData>`.
- **Funções:**
  - `updateTheme(isDark)`, `updateUserName(name)`, `updateCurrency(currency)`, `updateLanguage(language)` — persiste + aplica locale via `AppCompatDelegate.setApplicationLocales`.
  - `updateReminderSettings(hour, minute, intervalDays, isEnabled)` — agenda/cancela o trabalho periódico único `"WeeklyReminder"`.
  - `sendTestNotification()` — `NotificationWorker` pontual `TYPE_TEST`.
  - `clearAllData()` — exclui todas as transações + categorias customizadas (mantém as padrão).
  - `exportDataToCsv(onResult)` — gera um **Excel 2003 SpreadsheetML (.xls)** com 5 planilhas: Transacoes, Categorias, Fluxo Financeiro, Tendencia por Categoria, Resumo Financeiro.
  - `exportBackup(password, onResult)` — Gson → arquivo `.flowbackup` criptografado com AES.
  - `importBackup(uri, password, onResult)` — valida o JSON, descriptografa, apaga e restaura transações/categorias/prefs.
  - `getBackupMetadata(uri, onResult)` — lê apenas o cabeçalho `BackupMetadata` para o diálogo de importação.

## 5. ManageBudgetsViewModel
- **Injeta:** `CategoryRepository`, `@ApplicationContext Context`.
- **Estado:** `categories: StateFlow<List<Category>>`; `deleteResult: SharedFlow<DeleteResult>` (`Success`/`Failure`).
- **Funções:**
  - `updateBudget(category, newBudget)` — persiste; enfileira `TYPE_BUDGET_CHECK` quando `newBudget > 0`.
  - `deleteCategory(category)` — bloqueia categorias padrão; mensagem amigável em falhas de constraint FK.
  - `createNewCategory(name, color, iconName)` — categoria não-padrão.
  - `updateCategoryDetails(category, name, color, iconName)`.

## 6. EditTransactionViewModel — órfão
- Mesmas dependências/padrão do AddTransactionViewModel; `updateTransaction(...)` e `deleteTransaction(...)`. **Não é referenciado por nenhuma tela** (a UX de edição vive no `AddTransactionSheet`). Candidato a remoção até que a UI de edição volte.

## 7. MonthlyHistoryViewModel
- **Injeta:** `TransactionRepository`, `UserPreferencesRepository`.
- **Estado:** `MonthlyHistoryUiState(isLoading, currency, year, monthlyData: List<MonthSummary>)`; `MonthSummary(monthName, summary: SummaryData)`.
- **Lógica:** snapshot pontual em `init`; itera os 12 `Month.values()`, calcula receitas/despesas/restante por mês, preenche meses vazios com zeros, nomes de mês localizados em pt-BR.

## 8. FinancialFlowViewModel
- **Injeta:** `TransactionRepository`, `UserPreferencesRepository`, `@ApplicationContext Context`.
- **Estado:** `FinancialFlowUiState(monthlyData: List<MonthlyFinancialData>, currency, isLoading)` onde `MonthlyFinancialData(yearMonth, salary, monthlyYield, accumulatedYield, accumulatedBalance, totalWealth, expensesByCategory: Map<String, Double>)`.
- **Funções:** `exportSheetToCsv(onResult)` — CSV separado por ponto-e-vírgula com rótulos `MMM yyyy`.
- **Lógica (reativa):** percorre mês a mês da primeira transação até agora:
  - `salary` = INCOME excluindo a categoria "Rendimentos"; `monthlyYield` = INCOME onde categoria == "Rendimentos".
  - Acumulados: rendimento acumulado, saldo acumulado (receitas−despesas), patrimônio total (soma das receitas).
  - `expensesByCategory` para composições empilhadas.

## 9. CategoryTrendsViewModel
- **Injeta:** `TransactionRepository`, `CategoryRepository`, `UserPreferencesRepository`, `@ApplicationContext Context`.
- **Estado:** `CategoryTrendsUiState(monthlyData: List<MonthlyExpenseData>, currency, isLoading, categories: List<Category>, totalExpensesByCategory: List<CategorySummary>)`; `MonthlyExpenseData(yearMonth, expensesByCategory, totalExpenses, categorySummaries)`.
- **Funções:** `exportSheetToCsv(onResult)` — dinâmica: cabeçalho `Data,<cat>...`, linhas `sortedByDescending`.
- **Lógica (pontual):** exclui categorias de receita ("Salário", "Investimentos", "Rendimentos") das tendências de despesa; monta a pizza geral + série mensal, semeando cada categoria rastreada com `0.0` para linhas contínuas.

## 10. FinancialSummaryViewModel
- **Injeta:** `TransactionRepository`, `UserPreferencesRepository`.
- **Estado:** `FinancialSummaryUiState(isLoading, currency, currentYear, totalSummary, yearlySummary, monthlySummary)` onde `SummaryData(totalIncome, totalExpense, remaining)`.
- **Lógica (reativa):** três fatias do mesmo fluxo de transações: total geral, `date.year == currentYear`, `YearMonth == now`. Atalho para dados vazios.

## 11. ExpenseAnalysisViewModel
- **Injeta:** `TransactionRepository`, `UserPreferencesRepository`.
- **Estado:** `ExpenseAnalysisUiState(isLoading, currency, averageDaily/Weekly/Monthly, peakDayOfWeek: Pair<DayOfWeek, Double>?, peakDayOfMonth: Int?, categoryMetrics: List<CategoryMetric>, weeklyHeatmap: Map<DayOfWeek, Double>, dailyHistory: List<DailyExpense>)`; `CategoryMetric(...)` e `DailyExpense(date, amount)`.
- **Lógica (pontual, apenas despesas):**
  - Escala de datas: `daysDiff`, `weeksDiff = ceil(days/7)`, `monthsDiff = ceil(days/30)`.
  - Médias = total / cada escala.
  - Heatmap semanal normalizado pela contagem de datas distintas por dia da semana; pico de dia da semana e dia do mês pela maior soma.
  - Recorrência de categoria: `monthlyFrequency = count / monthsDiff`, `isRecurring = a frequência >= 1.0`.

## 12. PatternsAnalysisViewModel — placeholder
- Arquivo em branco (artefato de conflito de build). Funcionalidade movida para CategoryTrends.

## 13. AddTransactionViewModel
- **Injeta:** `TransactionRepository`, `CategoryRepository`, `RecurringTransactionRepository` (WIP: apenas wiring de DI), `@ApplicationContext Context`.
- **Estado:** `expenseCategories: StateFlow<List<Category>>` (tudo menos "Salário"/"Rendimentos"); `incomeCategories: StateFlow<List<Category>>` ("Salário", "Investimentos", "Rendimentos" + custom não-padrão).
- **Funções:** `saveTransaction(description, amount, type, categoryId, date)` — insere; enfileira `TYPE_BUDGET_CHECK` quando type == EXPENSE.

---

## Observações para mantenedores
- A lógica de whitelist/blacklist de categorias está duplicada entre `AddTransactionViewModel` e `EditTransactionViewModel`.
- Reativo vs pontual: atenção que `MonthlyHistory`, `CategoryTrends` e `ExpenseAnalysis` calculam uma vez em `init` e **não** reagem a mudanças de dados com a tela aberta — reabrir a tela recalcula.
- `CategoryRepository` é injetado mas não usado em `DashboardViewModel`.
- As verificações de orçamento em background dependem de `NotificationWorker.TYPE_BUDGET_CHECK`; a feature de transações recorrentes (WIP) terá seu próprio `RecurringTransactionWorker`.