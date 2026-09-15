# FlowFinance — UI, Navigation & Theme

UI source root: `app/src/main/java/com/flowfinance/app/ui/`

## 1. Navigation overview

- **Single activity**: `MainActivity` (`@AndroidEntryPoint`, extends `AppCompatActivity`), requests `POST_NOTIFICATIONS` on Android 13+, calls `enableEdgeToEdge()`, then sets a Compose content that builds the whole UI.
- **Graceful navigation**: `NavHost` defined inline in `MainActivity` (the file `ui/navigation/NavigationGraph.kt` is a dead stub — do not use it).
- **Routes** (`ui/navigation/Screen.kt`), a `sealed class Screen(route, titleRes, icon)`:

| Route | Screen | In bottom bar | Notes |
|-------|--------|---------------|-------|
| `dashboard` | DashboardScreen | yes | FAB opens AddTransactionSheet |
| `transactions` | TransactionsScreen | yes | full history |
| `planning` | PlanningScreen | yes | FAB → ManageBudgets |
| `panel` | PanelScreen | yes | analysis hub |
| `settings` | SettingsScreen | yes | |
| `user_profile` | UserProfileScreen | no | → from Settings |
| `manage_budgets` | ManageBudgetsScreen | no | |
| `financial_flow` | FinancialFlowScreen | no | → Sheet, ChartDetail |
| `sheet` | SheetScreen | no | |
| `category_trends` | CategoryTrendsScreen | no | → CategoryTrendsSheet, ChartDetail |
| `category_trends_sheet` | CategoryTrendsSheetScreen | no | |
| `expense_analysis` | ExpenseAnalysisScreen | no | |
| `financial_summary` | FinancialSummaryScreen | no | → MonthlyHistory |
| `monthly_history` | MonthlyHistoryScreen | no | |
| `chart_detail/{chartType}` | FullScreenChartScreen | no | arg `chartType` (default `"overview"`) |

- **Scaffold behavior**:
  - `NavigationBar` shows only for the 5 top-level routes (`currentRoute` whitelist).
  - Bottom-bar navigation uses `popUpTo(startDestination){saveState=true}`, `launchSingleTop=true`, `restoreState=true`.
  - `FloatingActionButton`: Dashboard → opens global `AddTransactionSheet` modal; Planning → navigates to `ManageBudgets`.
  - The global `AddTransactionSheet` is rendered at activity level (`if (showBottomSheet)`), so it can be reused from any route.

## 2. Screens by feature

### Dashboard (`screens/dashboard/DashboardScreen.kt`)
- **Total Balance** card (all-time income − expense), monthly **Income** / **Expenses** indicators (green/red).
- **Expenses by Category** donut (`PieChart`) + hand-built legend.
- **Recent Transactions** (5) via reusable `TransactionItem`; "See all" → `TransactionsScreen`.
- Uses `DashboardViewModel` (reactive).

### Transactions (`screens/transactions/TransactionsScreen.kt`)
- Month navigation (`previousMonth()`/`nextMonth()`), **search box** (description + category name), list grouped by date with **sticky headers** (date + daily expense sum).
- **Swipe-to-delete** (`SwipeToDismissBox`, EndToStart) inside a red animated background.
- Empty state. Uses `TransactionsViewModel`.

### Planning (`screens/planning/PlanningScreen.kt` + `ManageBudgetsScreen.kt`)
- Planning: per-category progress bars `spent / budget` (`LinearProgressIndicator` colored by category).
- ManageBudgets: CRUD categories with color picker (`HsvColorPicker`), icon picker (`getCategoryIconMap`), inline budget edit, swipe-to-delete (blocked for default categories), long-press to edit. Uses `ManageBudgetsViewModel`.
- `rememberCategoryIcon(iconKey)` lives here and is reused across the app (Dashboard, AddTransactionSheet).

### Panel — analysis hub (`screens/panel/PanelScreen.kt`)
- Hosts a `GeneralOverviewChart` preview + annual summary card, plus 4 buttons → FinancialFlow, CategoryTrends, ExpenseAnalysis, FinancialSummary.

### Panel — Financial Flow (`FinancialFlowScreen.kt`, `SheetScreen.kt`, `FullScreenChartScreen.kt` subset)
- `FinancialFlowScreen`: interactive chart cards (overview multi-line, salary bars, yield area, combined), "Show sheet" button.
- `SheetScreen`: spreadsheet-style table (date, salary, monthly/acc. yield, acc. balance, wealth) + CSV export/share.
- Charts support **pan, zoom (1–5x)** and tap **tooltips**.

### Panel — Category Trends (`CategoryTrendsScreen.kt`, `CategoryTrendsSheetScreen.kt`)
- Pie carousel (`HorizontalPager`: grand total / current month), ranking `TabRow` (total/monthly, top 5 horizontal bars), monthly multi-line chart, stacked-area composition chart, detailed sheet (pivot table) + CSV export.

### Panel — Expense Analysis (`ExpenseAnalysisScreen.kt`)
- Averages (daily/weekly/monthly cards), peaks (day-of-week and day-of-month), **weekly heatmap** bars, recurring/occasional category tabs (recurrence rule: `count / monthsDiff >= 1.0`).

### Panel — Financial Summary & Monthly History
- `FinancialSummaryScreen`: three `SummaryTableCard`s (all-time, current year, current month) with Total/Spent/Remaining rows and a legend popup; "View Monthly History".
- `MonthlyHistoryScreen`: 12 `SummaryTableCard`s, one per month of the current year.

### Full-Screen Chart (`FullScreenChartScreen.kt`)
- Dispatches by `chartType`: `category_trends_line` / `category_trends_stacked` → CategoryTrends branch, otherwise FinancialFlow branch.
- Renders the chart at full size, with legend, info popup, and **save as PNG** (`saveBitmapToFile` → MediaStore, on `saveBitmapToFile`).

### Settings (`screens/settings/SettingsScreen.kt`, `UserProfileScreen.kt`)
- Settings hub grouped in sections: Profile (→UserProfile), Preferences (dark theme switch; notification dialog with `TimePickerDialog`, interval stepper 1–30 days, test-notification button), Data (export spreadsheet .xls, clear data with typed-confirmation), Backup (export/import encrypted backups with password dialogs), About (version, GitHub, docs).
- `shareFile(context, path, isBackup)` top-level helper (FileProvider + ACTION_SEND), reused by Sheet/CategoryTrendsSheet.
- UserProfile: name, currency radio (BRL/USD/EUR), language radio (default/en/pt-BR/es) → saves via `SettingsViewModel`.

## 3. Reusable components (`ui/components/`)

| Component | Purpose |
|-----------|---------|
| `AddTransactionSheet` | `ModalBottomSheet` for creating income/expense: segmented type selector, amount (decimal), description (35 chars), category row (`CategoryChip` 50dp circles), date picker via press interaction. Saves through `AddTransactionViewModel`. |
| `PieChart` | Animated donut (`drawArc` strokes, 1s tween), sweep per `CategorySummary`. Dashboard pie. |
| `FinancialFlowCharts` | `GeneralOverviewChart` (5-series multi-line), `SalaryBarChart`, `YieldAreaChart` (gradient), `CombinedChart`; shared palette (`ColorSalary`, `ColorYield`, `ColorAccYield`, `ColorBalance`, `ColorWealth`); grid + tooltip + pan/zoom. |
| `CategoryTrendsCharts` | `CategoryPieChart`, `CategoryHorizontalBarChart`, `CategoryTrendsLineChart`, `CategoryStackedAreaChart`; own grid/tooltip copy. |

Shared UI elements across screens: `TransactionItem` (Dashboard→Transactions), `SummaryTableCard` (FinancialSummary→Panel/MonthlyHistory), `ChartCard` (FinancialFlow→Panel), `rememberCategoryIcon` (planning→everywhere), `shareFile` (Settings→Sheet/TrendsSheet), `formatCurrency` (util, everywhere).

## 4. Theme (`ui/theme/`)

**Colors** (`Color.kt`): brand navy blues `NavyBlue80 #B0C4DE / NavyBlue40 #2C3E50 / NavyBlueDark #1A252F`; semantic `GreenIncome #4CAF50`, `RedExpense #EF5350`; Material purple/pink accents for secondary/tertiary; `SurfaceDark #121212`, `SurfaceLight #F5F5F5`.

**Theme** (`Theme.kt`):
- `darkColorScheme(primary=NavyBlue80, ...)` / `lightColorScheme(primary=NavyBlue40, ...)`.
- `FlowFinanceTheme(darkTheme, dynamicColor = true)`: uses `dynamicDark/LightColorScheme` on Android 12+ (SDK ≥ S), else the static schemes. `darkTheme` is wired from `SettingsViewModel.userData.isDarkTheme` (user override beats system).

**Typography** (`Type.kt`): only `bodyLarge` overridden (16.sp/24.sp/0.5.sp); everything else Material defaults. Charts use small hardcoded sizes (9.sp–10.sp axis labels).

## 5. Internationalization

- Resources: `values` (EN default), `values-pt-rBR`, `values-es`.
- ~240 strings in `values/strings.xml` covering navigation, all screens and dialogs.
- Language switch at runtime via `SettingsViewModel.updateLanguage` → `AppCompatDelegate.setApplicationLocales` (activity-scoped, requires per-activity locales in the manifest).
- Known gap: several Panel screens (CategoryTrends, CategoryTrendsSheet, FullScreenChart, MonthlyHistory, ManageBudgets, Sheet) still hardcode Portuguese strings instead of `stringResource`.

## 6. Code-quality notes for UI

- Duplicated private helpers `ChartTooltip`, `drawStandardChartGrid`, `getIndexFromTap` exist in both `FinancialFlowCharts.kt` and `CategoryTrendsCharts.kt`.
- `ui/navigation/NavigationGraph.kt`, `PatternsAnalysisScreen.kt`, `PatternsSheetScreen.kt` are stubs/placeholders and should not be extended.