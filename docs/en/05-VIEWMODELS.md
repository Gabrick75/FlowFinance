# FlowFinance — ViewModels

Location: `app/src/main/java/com/flowfinance/app/ui/viewmodel/` · All are `@HiltViewModel` with constructor injection.

## Reference table

| ViewModel | Feeds | Loading | Mutations |
|-----------|-------|---------|-----------|
| `DashboardViewModel` | DashboardScreen | Reactive | — |
| `TransactionsViewModel` | TransactionsScreen | Reactive | month nav, search, delete |
| `PlanningViewModel` | PlanningScreen | Reactive | — |
| `SettingsViewModel` | SettingsScreen, UserProfileScreen | Reactive (prefs) + one-shot jobs | theme/name/currency/language, reminders, clear-all, export/import |
| `ManageBudgetsViewModel` | ManageBudgetsScreen | Reactive + SharedFlow result | updateBudget, delete/create/update category |
| `EditTransactionViewModel` | **none (orphaned)** | Reactive | update/delete transaction |
| `MonthlyHistoryViewModel` | MonthlyHistoryScreen | One-shot (init) | — |
| `FinancialFlowViewModel` | FinancialFlowScreen, SheetScreen, FullScreenChart (financial), PanelScreen | Reactive | export CSV |
| `CategoryTrendsViewModel` | CategoryTrendsScreen, CategoryTrendsSheetScreen, FullScreenChart (category) | One-shot (init) | export CSV |
| `FinancialSummaryViewModel` | FinancialSummaryScreen, PanelScreen | Reactive | — |
| `ExpenseAnalysisViewModel` | ExpenseAnalysisScreen | One-shot (init) | — |
| `PatternsAnalysisViewModel` | **none (blank placeholder)** | — | — |
| `AddTransactionViewModel` | AddTransactionSheet | Reactive (category lists) | saveTransaction |

Shared conventions:
- UI state = immutable `data class` inside a `StateFlow` with safe defaults (so `collectAsState()` renders immediately).
- Aggregates computed in the ViewModel from repository `Flow`s; rooms emit → combine → recompute.
- Expense mutations enqueue a one-time `NotificationWorker` (`TYPE_BUDGET_CHECK`) to refresh budget alerts.

---

## 1. DashboardViewModel
- **Injects:** `TransactionRepository`, `UserPreferencesRepository`, `CategoryRepository` (injected but unused).
- **State:** `uiState: StateFlow<DashboardUiState>` where `DashboardUiState(totalBalance, monthlyIncome, monthlyExpense, recentTransactions: List<TransactionWithCategory>, currency)`; plus `chartData: StateFlow<List<CategorySummary>>` (current-month expense pie).
- **Logic:** combines the current `YearMonth`, the full transaction flow and user data:
  - `monthlyIncome/Expense` = sums filtered by `TransactionType` inside the month.
  - `totalBalance` = all-time income − expense.
  - `recentTransactions` = first 5 (DAO is already date-DESC).
  - `chartData` = `flatMapLatest { getCategorySummaryByTypeAndDateRange(EXPENSE, ...) }`.
  - Both flows `WhileSubscribed(5000)`.

## 2. TransactionsViewModel
- **Injects:** `TransactionRepository`, `UserPreferencesRepository`.
- **State:** `TransactionsUiState(currentMonth: YearMonth, searchQuery, transactionsByDate: Map<LocalDate, List<TransactionWithCategory>>, currency)`.
- **Functions:** `nextMonth()`, `previousMonth()`, `onSearchQueryChange()`, `deleteTransaction()`.
- **Logic:** combine month + query + transaction flow + prefs; filters to the selected month, optional case-insensitive match on `description` or `category.name`, groups by date, sorts dates descending.

## 3. PlanningViewModel
- **Injects:** `TransactionRepository`, `UserPreferencesRepository`.
- **State:** `PlanningUiState(categorySpendings: List<CategorySummary>, currency)`.
- **Logic:** current-month expense totals per category (`getCategorySummaryByTypeAndDateRange(EXPENSE)`), fixed to `YearMonth.now()`. Read-only.

## 4. SettingsViewModel (largest)
- **Injects:** `TransactionRepository`, `CategoryRepository`, `UserPreferencesRepository`, `@ApplicationContext Context`.
- **State:** `userData: StateFlow<UserData>`.
- **Functions:**
  - `updateTheme(isDark)`, `updateUserName(name)`, `updateCurrency(currency)`, `updateLanguage(language)` — persists + applies locale via `AppCompatDelegate.setApplicationLocales`.
  - `updateReminderSettings(hour, minute, intervalDays, isEnabled)` — schedules/cancels unique periodic work `"WeeklyReminder"`.
  - `sendTestNotification()` — one-time `NotificationWorker` `TYPE_TEST`.
  - `clearAllData()` — deletes all transactions + custom categories (keeps defaults).
  - `exportDataToCsv(onResult)` — generates an **Excel 2003 SpreadsheetML (.xls)** with 5 worksheets: Transacoes, Categorias, Fluxo Financeiro, Tendencia por Categoria, Resumo Financeiro.
  - `exportBackup(password, onResult)` — Gson → AES-encrypted `.flowbackup` file.
  - `importBackup(uri, password, onResult)` — validates JSON, decrypts, wipes and restores transactions/categories/prefs.
  - `getBackupMetadata(uri, onResult)` — reads only the `BackupMetadata` header for the import dialog.

## 5. ManageBudgetsViewModel
- **Injects:** `CategoryRepository`, `@ApplicationContext Context`.
- **State:** `categories: StateFlow<List<Category>>`; `deleteResult: SharedFlow<DeleteResult>` (`Success`/`Failure`).
- **Functions:**
  - `updateBudget(category, newBudget)` — persists; enqueues `TYPE_BUDGET_CHECK` when `newBudget > 0`.
  - `deleteCategory(category)` — blocks default categories; friendly message on FK constraint failures.
  - `createNewCategory(name, color, iconName)` — non-default category.
  - `updateCategoryDetails(category, name, color, iconName)`.

## 6. EditTransactionViewModel — orphaned
- Same dependencies/pattern as AddTransactionViewModel; `updateTransaction(...)` and `deleteTransaction(...)`. **Not referenced by any screen** (the editing UX lives in `AddTransactionSheet`). Candidates for removal once UI for editing is restored.

## 7. MonthlyHistoryViewModel
- **Injects:** `TransactionRepository`, `UserPreferencesRepository`.
- **State:** `MonthlyHistoryUiState(isLoading, currency, year, monthlyData: List<MonthSummary>)`; `MonthSummary(monthName, summary: SummaryData)`.
- **Logic:** one-shot `init` snapshot; loops all 12 `Month.values()`, computes income/expense/remaining per month, fills empty months with zeros, pt-BR localized month names.

## 8. FinancialFlowViewModel
- **Injects:** `TransactionRepository`, `UserPreferencesRepository`, `@ApplicationContext Context`.
- **State:** `FinancialFlowUiState(monthlyData: List<MonthlyFinancialData>, currency, isLoading)` where `MonthlyFinancialData(yearMonth, salary, monthlyYield, accumulatedYield, accumulatedBalance, totalWealth, expensesByCategory: Map<String, Double>)`.
- **Functions:** `exportSheetToCsv(onResult)` — semicolon-delimited CSV with `MMM yyyy` labels.
- **Logic (reactive):** walks month-by-month from first transaction to now:
  - `salary` = INCOME excluding category "Rendimentos"; `monthlyYield` = INCOME where category == "Rendimentos".
  - Running tallies: accumulated yield, accumulated balance (income−expense), total wealth (sum of income).
  - `expensesByCategory` for stacked compositions.

## 9. CategoryTrendsViewModel
- **Injects:** `TransactionRepository`, `CategoryRepository`, `UserPreferencesRepository`, `@ApplicationContext Context`.
- **State:** `CategoryTrendsUiState(monthlyData: List<MonthlyExpenseData>, currency, isLoading, categories: List<Category>, totalExpensesByCategory: List<CategorySummary>)`; `MonthlyExpenseData(yearMonth, expensesByCategory, totalExpenses, categorySummaries)`.
- **Functions:** `exportSheetToCsv(onResult)` — pivot: header `Data,<cat>...`, rows `sortedByDescending`.
- **Logic (one-shot):** excludes income categories ("Salário", "Investimentos", "Rendimentos") from expense trends; builds all-time pie + per-month series, seeding every tracked category with `0.0` so lines are continuous.

## 10. FinancialSummaryViewModel
- **Injects:** `TransactionRepository`, `UserPreferencesRepository`.
- **State:** `FinancialSummaryUiState(isLoading, currency, currentYear, totalSummary, yearlySummary, monthlySummary)` where `SummaryData(totalIncome, totalExpense, remaining)`.
- **Logic (reactive):** three slices from the same transaction flow: all-time, `date.year == currentYear`, `YearMonth == now`. Empty-data shortcut.

## 11. ExpenseAnalysisViewModel
- **Injects:** `TransactionRepository`, `UserPreferencesRepository`.
- **State:** `ExpenseAnalysisUiState(isLoading, currency, averageDaily/Weekly/Monthly, peakDayOfWeek: Pair<DayOfWeek, Double>?, peakDayOfMonth: Int?, categoryMetrics: List<CategoryMetric>, weeklyHeatmap: Map<DayOfWeek, Double>, dailyHistory: List<DailyExpense>)`; `CategoryMetric(...)` and `DailyExpense(date, amount)`.
- **Logic (one-shot, expense-only):**
  - Dates span: `daysDiff`, `weeksDiff = ceil(days/7)`, `monthsDiff = ceil(days/30)`.
  - Averages = total / each span.
  - Weekly heatmap normalized by the count of distinct dates per weekday; peak day/week and peak day/month by max sum.
  - Category recurrence: `monthlyFrequency = count / monthsDiff`, `isRecurring = frequency >= 1.0`.

## 12. PatternsAnalysisViewModel — placeholder
- Blank file (build-conflict artifact). Feature moved to CategoryTrends.

## 13. AddTransactionViewModel
- **Injects:** `TransactionRepository`, `CategoryRepository`, `RecurringTransactionRepository` (WIP: DI wiring only), `@ApplicationContext Context`.
- **State:** `expenseCategories: StateFlow<List<Category>>` (all minus "Salário"/"Rendimentos"); `incomeCategories: StateFlow<List<Category>>` ("Salário", "Investimentos", "Rendimentos" + non-default custom).
- **Functions:** `saveTransaction(description, amount, type, categoryId, date)` — inserts; enqueues `TYPE_BUDGET_CHECK` when type == EXPENSE.

---

## Notes for maintainers
- Category whitelist/blacklist logic is duplicated between `AddTransactionViewModel` and `EditTransactionViewModel`.
- Reactive vs one-shot: be aware that `MonthlyHistory`, `CategoryTrends`, and `ExpenseAnalysis` compute once at `init` and will **not** react to data changes while the screen stays open — reopening the screen recalculates.
- `CategoryRepository` is injected but unused in `DashboardViewModel`.
- `Screen`/background budget checks depend on `NotificationWorker.TYPE_BUDGET_CHECK`; recurring-transaction feature (WIP) will add its own `RecurringTransactionWorker`.