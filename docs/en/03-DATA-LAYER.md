# FlowFinance — Data Layer

All persistence lives in `app/src/main/java/com/flowfinance/app/data/`. Single on-device SQLite DB (`"flowfinance_db"` via Room) plus a DataStore preferences file.

## 1. Room entities

### `Transaction` — table `transactions`
```kotlin
@Entity(tableName = "transactions",
  foreignKeys = [ForeignKey(Category, parent = ["id"], child = ["categoryId"], onDelete = RESTRICT)],
  indices   = [Index("categoryId")])
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    val amount: Double,
    val date: LocalDate,        // stored as ISO-8601 string
    val type: TransactionType,  // stored as enum name: INCOME | EXPENSE
    val categoryId: Int         // FK -> categories.id (RESTRICT)
)
```

### `Category` — table `categories`
```kotlin
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val color: Int,            // Android color int (ARGB)
    val icon: String? = null,  // icon key (see ManageBudgetsScreen.getCategoryIconMap)
    val budgetLimit: Double? = null, // optional monthly budget
    val isDefault: Boolean = false  // default categories cannot be deleted
)
```

### `RecurringTransaction` — table `recurring_transactions`  *(WIP, migration 1→2)*
```kotlin
@Entity(tableName = "recurring_transactions",
  foreignKeys = [ForeignKey(Category, parent = ["id"], child = ["categoryId"], onDelete = RESTRICT)],
  indices   = [Index("categoryId")])
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Int,
    val frequency: RecurrenceFrequency, // DAILY | WEEKLY | MONTHLY | YEARLY
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val occurrencesGenerated: Int = 0,
    val isActive: Boolean = true
)
```

## 2. Type converters (`data/local/converters/Converters.kt`)

- `LocalDate` ↔ ISO `String` (`LocalDate.parse` / `toString`).
- `TransactionType` ↔ enum name.
- `RecurrenceFrequency` ↔ enum name (WIP).

## 3. DAOs

### `TransactionDao`
| Method | Query / behavior |
|--------|------------------|
| `getAllTransactionsWithCategory()` | `SELECT * FROM transactions ORDER BY date DESC` (transactional) → `Flow<List<TransactionWithCategory>>` |
| `getAllTransactions()` | plain list, date DESC → `Flow` |
| `getTransactionsByDateRange(start, end)` | `WHERE date BETWEEN :start AND :end ... DESC` |
| `getTotalAmountByTypeAndDateRange(type, start, end)` | `SELECT SUM(amount) ... WHERE type = :type AND date BETWEEN ...` → `Flow<Double?>` |
| `getCategorySummaryByTypeAndDateRange(type, start, end)` | `SELECT c.*, SUM(t.amount) as totalAmount FROM categories c JOIN transactions t ON c.id = t.categoryId WHERE t.type = :type AND t.date BETWEEN ... GROUP BY c.id` → `Flow<List<CategorySummary>>` |
| `getTransactionCountForCategory(categoryId)` | count used by category-deletion guard |
| `insertTransaction` | `OnConflictStrategy.REPLACE` |
| `updateTransaction` / `deleteTransaction` | standard |
| `deleteAllTransactions()` | used by clear-data / backup import |

### `CategoryDao`
- `getAllCategories()` → `Flow<List<Category>>`
- `getCategoryById(id)` → suspend single
- `insertCategory` (REPLACE), `updateCategory`, `deleteCategory`
- `deleteAllCustomCategories()` — `WHERE isDefault = 0`
- `deleteAllCategories()` — used by backup restore

### `RecurringTransactionDao` *(WIP)*
- `getAllRecurringTransactions()` → `ORDER BY isActive DESC, description ASC`
- `getActiveRecurringTransactions()` → `WHERE isActive = 1` (suspend, list)
- `insert` (REPLACE) / `update` / `delete`

## 4. Database (`data/local/AppDatabase.kt`)

- `@Database(entities = [Transaction, Category, RecurringTransaction], version = 2, exportSchema = false)`.
  - Version **1** (committed): `Transaction` + `Category`.
  - Version **2** (WIP, uncommitted): adds `RecurringTransaction` + **explicit `MIGRATION_1_2`** in `DatabaseModule`.
- No schema export (`exportSchema = false`), no FallbackToDestructiveMigration.
- Type converters registered at `@TypeConverters(Converters::class)`.

### Database creation & seed (`di/DatabaseModule.kt`)
`provideAppDatabase()` builds Room with:
- `.addMigrations(MIGRATION_1_2)` (WIP).
- A `RoomDatabase.Callback` that, on `onCreate`/`onOpen`, seeds **8 default categories** when the `categories` table is empty (raw `INSERT OR REPLACE`):

| Name | Color | Icon | Seed? |
|------|-------|------|-------|
| Alimentação | `#EF5350` | `restaurant` | default |
| Lazer | `#42A5F5` | `attractions` | default |
| Transporte | `#FFA726` | `commute` | default |
| Saúde | `#66BB6A` | `health_and_safety` | default |
| Educação | `#AB47BC` | `school` | default |
| Salário | `#26A69A` | `payments` | default |
| Investimentos | `#7E57C2` | `trending_up` | default |
| Rendimentos | `#4CAF50` | `trending_up` | default |

The module also provides `transactionDao()`, `categoryDao()` and (WIP) `recurringTransactionDao()`.

> Note: `BackupMetadata.dbVersion` currently writes `3` while `AppDatabase` is on version 2 (an inconsistency to converge in the recurring-transactions PR).

## 5. Repositories

Thin `@Singleton` wrappers over the DAOs, exposing `Flow` for reads and `suspend` for writes:

- **`TransactionRepository`** — mirrors all `TransactionDao` operations plus `getTransactionsByDateRange`, type/date aggregates and `CategorySummary`s.
- **`CategoryRepository`** — mirrors `CategoryDao` (list, by-id, insert/update/delete, delete all custom/all).
- **`RecurringTransactionRepository`** *(WIP)* — mirrors `RecurringTransactionDao`.

## 6. Relation / projection models

- **`TransactionWithCategory`** — `@Embedded transaction` + `@Relation(categoryId ↔ id)` `category`. Result of the polymorphic query, used by Dashboard/Transactions/analytics.
- **`CategorySummary`** — `@Embedded category` + `totalAmount: Double` (from `SUM`). Powers every pie/ranking chart.

## 7. User preferences (DataStore)

**`UserPreferencesRepository`** (`data/preferences/`), file name `"settings"`, keys:

| Key | Type | Default |
|-----|------|---------|
| `user_name` | string | `"Usuário"` |
| `currency` | string | `"BRL"` |
| `is_dark_theme` | boolean | `false` |
| `reminder_hour` | int | `9` |
| `reminder_minute` | int | `0` |
| `reminder_interval_days` | int | `7` |
| `reminder_enabled` | boolean | `true` |
| `language` | string | `""` (system default) |

Exposes `userData: Flow<UserData>` and `set*` suspend setters. `UserData` is a plain data class consumed by ViewModels/UI and serialized into backups.

## 8. Backup models (`data/local/model/BackupData.kt`)

- `BackupFile { metadata: BackupMetadata, encryptedData: String }` — wrapper written to `.flowbackup`.
- `BackupMetadata { appVersion: String, dbVersion: Int, createdAt: String }`.
- `BackupPayload { transactions: List<Transaction>, categories: List<Category>, userData: UserData? }` — the JSON payload encrypted by `CryptoUtils` (see 07-SECURITY).

## 9. Data flow example (Dashboard)

```
DB commit → TransactionDao.getAllTransactionsWithCategory() Flow emits
  → DashboardViewModel.combine(flow, userData) recomputes
     monthlyIncome/Expense, totalBalance, recent 5, chartData (EXPENSE pies)
  → collectAsState() re-renders the screen
```

No repository cache, no refresh mechanism — reactivity comes entirely from Room + Flow + DataStore.