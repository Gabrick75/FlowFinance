# FlowFinance — Background Work (WorkManager)

WorkManager 2.9.0. The app is `Application, Configuration.Provider`; the **Hilt worker factory** is injected and used for every worker (`@HiltWorker` + `@AssistedInject`), so all workers can use repositories.

```kotlin
// FlowFinanceApplication.kt
override val workManagerConfiguration: Configuration
    get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
```

The manifest disables the default WorkManager initializer so the Hilt-aware one is used.

## Scheduling on app start (`FlowFinanceApplication.onCreate()`)

| Unique work name | Worker | Constraints / period | Policy | Purpose |
|------------------|--------|----------------------|--------|---------|
| `"WeeklyBackup"` | `BackupWorker` | 7 days, **requires charging** | `KEEP` | Encrypted CSV backup of transactions |
| `"WeeklyReminder"` | `NotificationWorker` | first run next Sunday 09:00, then every 7 days | `UPDATE` | Weekly expense-recording reminder |
| `"RecurringTransactionsCheck"` *(WIP)* | `RecurringTransactionWorker` | every 1 day | `KEEP` | Generate due recurring transactions |
| `"RecurringTransactionsImmediateCheck"` *(WIP)* | `RecurringTransactionWorker` | one time (REPLACE) | `REPLACE` | Catch-up on app open |

> The weekly reminder is **also** (re)schedulable from Settings (time, interval 1–30 days, enable/disable), which enqueues/cancels `"WeeklyReminder"` via `ExistingPeriodicWorkPolicy.UPDATE`.

## 1. BackupWorker (`worker/`)

- Triggered only by the periodic `"WeeklyBackup"` schedule (7 days, requires charging).
- Reads all transactions `.first()`, builds **CSV** (columns `Id,Description,Amount,Date,Type,CategoryId`).
- **CSV formula injection guard:** fields are quoted and values starting with `=`, `+`, `-`, `@` are neutralized with a leading `'` (see `csvEscape`).
- Writes `backup_transactions_<timestamp>.csv.enc` to `filesDir`, **encrypted with the AndroidKeyStore key** (`CryptoUtils.encryptWithKeystoreKey`) — no user password required.
- Logs success/error; returns `Result.success()/failure()`.

## 2. NotificationWorker (`workers/`)

Dispatches on input `key_notification_type`:

| Type constant | Behavior |
|---------------|----------|
| `TYPE_WEEKLY_REMINDER` | Shows the weekly reminder notification (id `1001`). |
| `TYPE_BUDGET_CHECK` | Reads categories + current month expense summaries; for each category with a budget > 0, posts alerts at **50 / 70 / 90 / 100 %** thresholds (notification id = `category.id`). |
| `TYPE_TEST` | Test notification (id `1002`). |

Encapsulated notification creation: channel `flow_finance_channel` ("Lembretes e Alertas", created on API 26+), permission check for `POST_NOTIFICATIONS` on Android 13+, tap opens `MainActivity` via immutable `PendingIntent`. Since the recurring-transactions PR, the notification plumbing was moved to `util/NotificationHelper.kt`, and `NotificationWorker.showNotification(...)` simply delegates there.

## 3. RecurringTransactionWorker (`worker/`) *(WIP, uncommitted)*

- Periodically (daily) + immediately on app start (one-shot).
- For every **active** `RecurringTransaction`, generates all occurrences that are due (occurrence date ≤ today and ≤ `endDate`):
  - Advances by `occurrenceIndex` from the rule's `startDate` via `RecurrenceFrequency.occurrenceDate(...)` — **anchored to the original start date** to avoid day-of-month drift (e.g. Jan 31 → Feb 28 → Mar 28).
  - Inserts a real `Transaction` per occurrence against the rule's category.
  - Updates `occurrencesGenerated` and deactivates the rule (`isActive = false`) once past `endDate`.
- If any occurrences were generated, shows a summary notification (`id 1003`).

### Notification IDs used
- `1001` weekly reminder · `1002` test · `1003` recurring-generated · `category.id` budget alerts.

## Coordination notes
- All enqueues are **unique** (`enqueueUniquePeriodicWork` / `enqueueUniqueWork`), so only one chain exists per key.
- Workers are `CoroutineWorker` (suspend-friendly) with `@HiltWorker`.
- `BackupWorker` and `RecurringTransactionWorker` both write to/read from internal storage; no user-visible progress UI (result communicated via system log + notifications).