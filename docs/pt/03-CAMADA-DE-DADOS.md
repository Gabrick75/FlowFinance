# FlowFinance — Camada de Dados

Toda a persistência vive em `app/src/main/java/com/flowfinance/app/data/`. Um único banco SQLite local (`"flowfinance_db"` via Room) mais um arquivo de preferências DataStore.

## 1. Entidades Room

### `Transaction` — tabela `transactions`
```kotlin
@Entity(tableName = "transactions",
  foreignKeys = [ForeignKey(Category, parent = ["id"], child = ["categoryId"], onDelete = RESTRICT)],
  indices   = [Index("categoryId")])
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    val amount: Double,
    val date: LocalDate,        // armazenado como string ISO-8601
    val type: TransactionType,  // armazenado como nome do enum: INCOME | EXPENSE
    val categoryId: Int         // FK -> categories.id (RESTRICT)
)
```

### `Category` — tabela `categories`
```kotlin
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val color: Int,            // cor Android (ARGB)
    val icon: String? = null,  // chave de ícone (ver getCategoryIconMap em ManageBudgetsScreen)
    val budgetLimit: Double? = null, // orçamento mensal opcional
    val isDefault: Boolean = false  // categorias padrão não podem ser excluídas
)
```

### `RecurringTransaction` — tabela `recurring_transactions`  *(WIP, migração 1→2)*
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

- `LocalDate` ↔ `String` ISO (`LocalDate.parse` / `toString`).
- `TransactionType` ↔ nome do enum.
- `RecurrenceFrequency` ↔ nome do enum (WIP).

## 3. DAOs

### `TransactionDao`
| Método | Query / comportamento |
|--------|----------------------|
| `getAllTransactionsWithCategory()` | `SELECT * FROM transactions ORDER BY date DESC` (transacional) → `Flow<List<TransactionWithCategory>>` |
| `getAllTransactions()` | lista simples, date DESC → `Flow` |
| `getTransactionsByDateRange(start, end)` | `WHERE date BETWEEN :start AND :end ... DESC` |
| `getTotalAmountByTypeAndDateRange(type, start, end)` | `SELECT SUM(amount) ... WHERE type = :type AND date BETWEEN ...` → `Flow<Double?>` |
| `getCategorySummaryByTypeAndDateRange(type, start, end)` | `SELECT c.*, SUM(t.amount) as totalAmount FROM categories c JOIN transactions t ON c.id = t.categoryId WHERE t.type = :type AND t.date BETWEEN ... GROUP BY c.id` → `Flow<List<CategorySummary>>` |
| `getTransactionCountForCategory(categoryId)` | contagem usada na proteção de exclusão de categoria |
| `insertTransaction` | `OnConflictStrategy.REPLACE` |
| `updateTransaction` / `deleteTransaction` | padrão |
| `deleteAllTransactions()` | usado por limpar-dados / import de backup |

### `CategoryDao`
- `getAllCategories()` → `Flow<List<Category>>`
- `getCategoryById(id)` → suspend, retorna uma
- `insertCategory` (REPLACE), `updateCategory`, `deleteCategory`
- `deleteAllCustomCategories()` — `WHERE isDefault = 0`
- `deleteAllCategories()` — usado na restauração de backup

### `RecurringTransactionDao` *(WIP)*
- `getAllRecurringTransactions()` → `ORDER BY isActive DESC, description ASC`
- `getActiveRecurringTransactions()` → `WHERE isActive = 1` (suspend, lista)
- `insert` (REPLACE) / `update` / `delete`

## 4. Banco de dados (`data/local/AppDatabase.kt`)

- `@Database(entities = [Transaction, Category, RecurringTransaction], version = 2, exportSchema = false)`.
  - Versão **1** (commitada): `Transaction` + `Category`.
  - Versão **2** (WIP, não commitada): adiciona `RecurringTransaction` + **`MIGRATION_1_2` explícita** em `DatabaseModule`.
- Sem exportação de schema (`exportSchema = false`), sem FallbackToDestructiveMigration.
- Type converters registrados via `@TypeConverters(Converters::class)`.

### Criação do banco e seed (`di/DatabaseModule.kt`)
`provideAppDatabase()` constrói o Room com:
- `.addMigrations(MIGRATION_1_2)` (WIP).
- Um `RoomDatabase.Callback` que, em `onCreate`/`onOpen`, semeia **8 categorias padrão** quando a tabela `categories` está vazia (`INSERT OR REPLACE` em SQL cru):

| Nome | Cor | Ícone | Seed? |
|------|-----|-------|-------|
| Alimentação | `#EF5350` | `restaurant` | padrão |
| Lazer | `#42A5F5` | `attractions` | padrão |
| Transporte | `#FFA726` | `commute` | padrão |
| Saúde | `#66BB6A` | `health_and_safety` | padrão |
| Educação | `#AB47BC` | `school` | padrão |
| Salário | `#26A69A` | `payments` | padrão |
| Investimentos | `#7E57C2` | `trending_up` | padrão |
| Rendimentos | `#4CAF50` | `trending_up` | padrão |

O módulo também fornece `transactionDao()`, `categoryDao()` e (WIP) `recurringTransactionDao()`.

> Observação: `BackupMetadata.dbVersion` hoje grava `3` enquanto `AppDatabase` está na versão 2 (inconsistência a convergir no PR das transações recorrentes).

## 5. Repositórios

Wrappers finos `@Singleton` sobre os DAOs, expondo `Flow` para leitura e `suspend` para escrita:

- **`TransactionRepository`** — espelha todas as operações do `TransactionDao`, além de `getTransactionsByDateRange`, agregações por tipo/data e `CategorySummary`s.
- **`CategoryRepository`** — espelha `CategoryDao` (lista, por id, insert/update/delete, delete custom/todas).
- **`RecurringTransactionRepository`** *(WIP)* — espelha `RecurringTransactionDao`.

## 6. Modelos de relação / projeção

- **`TransactionWithCategory`** — `@Embedded transaction` + `@Relation(categoryId ↔ id)` `category`. Resultado da query polimórfica, usado por Dashboard/Transactions/análises.
- **`CategorySummary`** — `@Embedded category` + `totalAmount: Double` (do `SUM`). Alimenta todos os gráficos de pizza/ranking.

## 7. Preferências do usuário (DataStore)

**`UserPreferencesRepository`** (`data/preferences/`), arquivo `"settings"`, chaves:

| Chave | Tipo | Padrão |
|-------|------|--------|
| `user_name` | string | `"Usuário"` |
| `currency` | string | `"BRL"` |
| `is_dark_theme` | boolean | `false` |
| `reminder_hour` | int | `9` |
| `reminder_minute` | int | `0` |
| `reminder_interval_days` | int | `7` |
| `reminder_enabled` | boolean | `true` |
| `language` | string | `""` (padrão do sistema) |

Expõe `userData: Flow<UserData>` e setters suspend `set*`. `UserData` é uma data class simples consumida por ViewModels/UI e serializada nos backups.

## 8. Modelos de backup (`data/local/model/BackupData.kt`)

- `BackupFile { metadata: BackupMetadata, encryptedData: String }` — wrapper gravado em `.flowbackup`.
- `BackupMetadata { appVersion: String, dbVersion: Int, createdAt: String }`.
- `BackupPayload { transactions: List<Transaction>, categories: List<Category>, userData: UserData? }` — o payload JSON criptografado por `CryptoUtils` (ver 07-SEGURANCA).

## 9. Exemplo de fluxo de dados (Dashboard)

```
Commit no banco → Flow do TransactionDao.getAllTransactionsWithCategory() emite
  → DashboardViewModel.combine(flow, userData) recalcula
     monthlyIncome/Expense, totalBalance, últimos 5, chartData (pizza de despesas)
  → collectAsState() re-renderiza a tela
```

Sem cache de repositório, sem mecanismo de refresh — a reatividade vem inteira de Room + Flow + DataStore.