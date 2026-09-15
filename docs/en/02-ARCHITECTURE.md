# FlowFinance — Architecture

## 1. Architectural pattern

**MVVM (Model-View-ViewModel)** with a unidirectional, reactive data flow driven by Kotlin `Flow`.

```
┌─────────────────────────────────────────────────────────────┐
│  UI  (Compose)                                               │
│  Screens: Dashboard, Transactions, Planning, Panel, Settings │
│  Components: AddTransactionSheet, Charts, TableCards         │
│        │  collectAsState() on StateFlow / SharedFlow         │
│        ▼                                                     │
├─────────────────────────────────────────────────────────────┤
│  ViewModel  (@HiltViewModel)                                 │
│  Exposes StateFlow<UiState>; converts raw domain data into   │
│  UI models and aggregates (charts, summaries, analytics)     │
│        │  repository calls (suspend / Flow)                  │
│        ▼                                                     │
├─────────────────────────────────────────────────────────────┤
│  Repository  (@Singleton)                                    │
│  TransactionRepository · CategoryRepository                  │
│  RecurringTransactionRepository (WIP) · UserPreferencesRepository│
│        │                                                     │
│        ▼                                                     │
├─────────────────────────────────────────────────────────────┤
│  Local data                                                  │
│  Room AppDatabase (DAO)          DataStore Preferences       │
│  SQLite  "flowfinance_db"        file "settings"             │
└─────────────────────────────────────────────────────────────┘
                 ▲
                 │ injected via Hilt (DatabaseModule)
```

Key characteristics:

- **Reactive by construction.** Room DAOs return `Flow`, which the ViewModels combine into `StateFlow`. Any change to a row is instantly reflected in every open screen (balances, charts, lists) without manual refresh.
- **Single source of truth:** on-device SQLite; all analytics are computed from the same `transactions`/`categories` tables.
- **No network, no auth.** The app never leaves the device except for system services.

## 2. Package layout (`app/src/main/java/com/flowfinance/app`)

| Package | Responsibility |
|---------|----------------|
| `MainActivity.kt` | Single activity; hosts the `NavHost`, bottom bar, FABs, and the global `AddTransactionSheet`. Requests POST_NOTIFICATIONS on Android 13+. |
| `FlowFinanceApplication.kt` | `@HiltAndroidApp`; provides WorkManager `Configuration` via HiltWorkerFactory; schedules backup / reminder / recurring-check jobs on startup. |
| `data/entity` | Room entities (`Transaction`, `Category`, `RecurringTransaction` WIP). |
| `data/dao` | Room DAOs with raw SQL aggregations. |
| `data/local/converters` | Room `TypeConverter`s (LocalDate, enums). |
| `data/local/model` | Relation models (`TransactionWithCategory`, `CategorySummary`) and backup models (`BackupFile`/`BackupPayload`). |
| `data/repository` | Repository layer wrapping DAOs; single `@Singleton` each. |
| `data/preferences` | `UserPreferencesRepository` + `UserData` snapshot over DataStore. |
| `di` | `DatabaseModule`: Room builder, seed categories, DAO providers. |
| `ui/screens` | Feature screens (`dashboard`, `transactions`, `planning`, `panel`, `settings`). |
| `ui/components` | Reusable composables (AddTransactionSheet, FinancialFlowCharts, CategoryTrendsCharts, PieChart). |
| `ui/viewmodel` | 13+ ViewModels. |
| `ui/navigation` | `Screen` (routes) and a stubbed `NavigationGraph` (unused; real graph in `MainActivity`). |
| `ui/theme` | Color palette, color schemes, typography. |
| `worker`, `workers` | WorkManager workers (`BackupWorker`, `RecurringTransactionWorker`; `NotificationWorker`). |
| `util` | `CryptoUtils`, `TransactionType`, `RecurrenceFrequency` (WIP), `NotificationHelper` (WIP), currency/date/save-bitmap extensions. |

## 3. Single-activity navigation

- One activity, one `NavHost` (defined in `MainActivity`, **not** in `ui/navigation/NavigationGraph.kt`, which is a dead stub).
- Routes are declared in `Screen.kt` (sealed class): 5 bottom-bar destinations (`dashboard`, `transactions`, `planning`, `panel`, `settings`) plus sub-screens and a parameterized route `chart_detail/{chartType}`.
- Bottom `NavigationBar` is hidden for every sub-screen via a route whitelist; a `FloatingActionButton` appears on Dashboard (add transaction sheet) and Planning (manage budgets).

## 4. ViewModel patterns

- All ViewModels are `@HiltViewModel` with constructor injection.
- UI state is a data class wrapped in `StateFlow`, defaulted to safe empty values so the UI renders immediately.
- Two data-loading strategies coexist:
  - **Reactive**: `combine(repositoryFlow, userPreferences)` recomputes state on every DB change (Dashboard, Transactions, Planning, FinancialFlow, FinancialSummary).
  - **One-shot**: `.first()` snapshot read in `init` (MonthlyHistory, CategoryTrends, ExpenseAnalysis) — cheaper, but does not auto-refresh if the user changes data while the screen is up.
- Mutations are simple: `viewModelScope.launch { repository.insert/update/delete(...) }`, letting the reactive flow push the new state.
- After expense mutations, some ViewModels enqueue a one-time `NotificationWorker` (budget check) so budget alerts stay current.

## 5. Background work startup

`FlowFinanceApplication.onCreate()` schedules three unique WorkManager chains:

1. `"WeeklyBackup"` — `BackupWorker`, period 7 days, requires charging, `KEEP` policy.
2. `"WeeklyReminder"` — `NotificationWorker` (`TYPE_WEEKLY_REMINDER`), first run next Sunday 09:00, period 7 days, `UPDATE` policy.
3. (WIP) `"RecurringTransactionsCheck"` (periodic daily, `KEEP`) + `"RecurringTransactionsImmediateCheck"` (one-time, `REPLACE`) — `RecurringTransactionWorker`.

Reminder timeout / frequency can also be changed at runtime from Settings, which reschedules `"WeeklyReminder"` (or cancels it).

## 6. Security model (summary)

- Password-based backups: PBKDF2 (65536 iters, 16-byte salt) → AES-256/GCM, output `salt‖iv‖ciphertext` Base64.
- Automatic/worker backups: AES-256/GCM with a non-exportable key in the AndroidKeyStore.
- Gson + ProGuard keeps for Room entities and backup models.
- No secrets stored in cleartext; no network to exfiltrate.

## 7. Known dead / stale code (documented for maintainers)

| File | Status |
|------|--------|
| `ui/navigation/NavigationGraph.kt` | Stub with empty composables; **not used** (navigation lives in `MainActivity`). |
| `ui/screens/panel/PatternsAnalysisScreen.kt`, `PatternsSheetScreen.kt` | Empty placeholders; feature moved to CategoryTrends screens. |
| `ui/viewmodel/PatternsAnalysisViewModel.kt` | Blank placeholder (build-conflict artifact). |
| `ui/viewmodel/EditTransactionViewModel.kt` | Orphaned — no screen references it (editing UX replaced by AddTransactionSheet flow). |
| `PanelScreen.onNavigateToPatternsAnalysis` | Callback still declared but leads to the CategoryTrends destination, not a Patterns screen. |

## 8. WIP feature (uncommitted, in progress)

**Recurring transactions** — data layer + worker staged but **no UI yet**:

- `RecurringTransaction` entity + `RecurringTransactionDao` + `RecurringTransactionRepository`.
- Room **migration 1→2** creates `recurring_transactions` (+ index) — `AppDatabase` bumped to version 2.
- `RecurringTransactionWorker` generates due recurring transactions every day (and immediately on app start).
- `RecurrenceFrequency` enum + `occurrenceDate()` helper anchored on the rule start date.
- `NotificationHelper` extracted (share notification logic with `NotificationWorker`).
- `AddTransactionViewModel` already injects `RecurringTransactionRepository` (DI wiring prepared for the future UI).

Anything in this section may still change; treat it as "in development".