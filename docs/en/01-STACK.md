# FlowFinance — Technical Stack

**Repo snapshot date:** 2026-09-14 · **App version:** 2.0.1 (`versionCode 5`)

## Summary table

| Layer | Technology | Version / Config | Notes |
|-------|-----------|------------------|-------|
| Language | Kotlin | 2.0.21 | 100% Kotlin, JVM target 11 |
| UI | Jetpack Compose + Material 3 | compose-bom 2024.09.00 | Declarative UI, dark/light + Dynamic Color |
| Android SDK | `compileSdk` 35, `targetSdk` 35, `minSdk` 24 | AGP 8.13.2 | Core library desugaring enabled |
| Build tool | Gradle | 9.5.1 (wrapper) | Kotlin DSL, version catalog (`gradle/libs.versions.toml`) |
| Codegen | KSP | 2.0.21-1.0.27 | Room + Hilt annotation processing |
| DI | Hilt (Dagger) | 2.51.1 (+ hilt-work 1.2.0, hilt-navigation-compose 1.2.0) | Singleton + ViewModel injection |
| Persistence / ORM | Room (SQLite) | 2.6.1 | **No ORM/query builder** — raw SQL in DAOs via annotations |
| Preferences | Jetpack DataStore Preferences | 1.1.1 | File: `settings` |
| Async | Kotlin Coroutines + Flow | (bundled with Kotlin) | Reactive, `StateFlow`/`Flow`-driven UI |
| Background | WorkManager | 2.9.0 | Periodic: weekly backup, weekly reminder, daily recurring check (WIP) |
| Serialization | Gson | 2.10.1 | Backup files, model serialization |
| Crypto | javax.crypto + AndroidKeyStore | — | AES/GCM, PBKDF2-HmacSHA256 (see 07-SECURITY) |
| Charts | Hand-rolled Compose `Canvas` | — | No chart library; custom charts in `ui/components/*Charts.kt` |
| Color picker | `com.github.skydoves:colorpicker-compose` | 1.0.0 | Category color selection |
| Auth | **None** | — | 100% offline app; no authentication, no network layer |
| Testing | JUnit 4, Espresso, Compose UI test | — | Only default sample tests present |
| i18n | Android resources | EN (default), PT-BR, ES | Locale switcher + per-activity locales |
| Release | R8 / ProGuard | `proguard-rules.pro` | Shrinking + resource shrinking enabled |
| Deploy/CI | **None** | — | No Docker, CI, Helm or Terraform files. APK in `app/release/`, distributed via GitHub Releases |

## Language & platform

- **Kotlin 2.0.21**, `kotlinOptions.jvmTarget = 11`, `compileOptions` Java 11.
- Android Gradle Plugin **8.13.2**; Gradle distribution **9.5.1** via wrapper.
- Package / applicationId: `com.flowfinance.app`. Supports legacy (24) through modern (35) Android.

## UI framework

- **Jetpack Compose** with the **Material 3** library and `material-icons-extended`.
- `composeOptions`/`kotlin compose` compiler plugin (Kotlin 2.0 compose plugin).
- Screens, components, theme and navigation are all Compose. `MainActivity` is an `AppCompatActivity` hosting a `NavHost`.
- Two custom chart implementations drawn with `Canvas` (no third-party chart dependency):
  - `FinancialFlowCharts.kt` (multi-line, bars, area, combined, tooltips, pan/zoom).
  - `CategoryTrendsCharts.kt` (pie, horizontal bars, multi-line, stacked area).
  - `PieChart.kt` (animated donut used on the Dashboard).

## Persistence & data

- **Room 2.6.1** database `"flowfinance_db"`:
  - `transactions`, `categories` (v1, committed) and `recurring_transactions` (v2, **WIP/uncommitted**).
  - `LocalDate` stored as ISO string; `TransactionType` and `RecurrenceFrequency` stored as enum names (Room `TypeConverter`s).
- Raw SQL in DAOs used for aggregation (no SQL query builder / ORM relational API):
  - `TransactionDao` — date-range filters, `SUM`, `JOIN` with category grouping for `CategorySummary`.
  - `CategoryDao`, and new `RecurringTransactionDao` (WIP).
- **Jetpack DataStore Preferences** for user settings (`user_name`, `currency`, `is_dark_theme`, reminder config, `language`).

## Dependency injection

- **Hilt 2.51.1**: `@HiltAndroidApp` application, `@AndroidEntryPoint` activity, `@HiltViewModel` ViewModels, `@HiltWorker` + `@AssistedInject` workers.
- `DatabaseModule` (Singleton scope) provides `AppDatabase`, DAOs and seed categories.
- Hilt `WorkerFactory` wired through `Configuration.Provider` in `FlowFinanceApplication`.

## Background processing

WorkManager is initialized via Hilt (`setWorkerFactory`). Workers:

| Work | Key | Schedule | Purpose |
|------|-----|----------|---------|
| `BackupWorker` | `"WeeklyBackup"` | every 7 days, requires charging | Encrypted CSV of transactions (AndroidKeyStore AES) |
| `NotificationWorker` | `"WeeklyReminder"` | Sunday 09:00 weekly | Reminder; plus one-shot budget checks / test notification |
| `RecurringTransactionWorker` (WIP) | `"RecurringTransactionsCheck"` / `"RecurringTransactionsImmediateCheck"` | daily + one immediate run | Generates recurring transactions from rules |

## Authentication

- **None.** There is no login, account, token, or server interaction anywhere in the codebase. The app is fully on-device; the only out-of-process interaction is Android system services (notifications, MediaStore, FileProvider sharing).

## Networking / backend

- **None.** No OkHttp/Retrofit/Ktor, no INTERNET permission, no API layer, no external data source.

## Deploy / CI / containers

- **No Dockerfile, no GitHub Actions / CI config, no Helm, no Terraform**, no `docker-compose`, no server deployment files.
- Distribution: signed **release APK** built locally (`app/release/FlowFinance-v2.0.1.apk`, with baseline profiles in `app/release/baselineProfiles/`).
- Releases published through the GitHub **Releases** page of `Gabrick75/FlowFinance`.

## Tests

- `app/src/test/` — `ExampleUnitTest.kt` (JUnit 4 placeholder).
- `app/src/androidTest/` — `ExampleInstrumentedTest.kt` / Espresso placeholder (runner `androidx.test.runner.AndroidJUnitRunner`).
- Compose UI test libraries declared in dependencies but not exercised.

## Key third-party libraries (exact versions)

| Dependency | Version |
|-----------|---------|
| androidx.core:core-ktx | 1.15.0 |
| androidx.lifecycle:lifecycle-runtime-ktx | 2.8.7 |
| androidx.activity:activity-compose | 1.9.3 |
| androidx.appcompat:appcompat | 1.7.0 |
| androidx.navigation:navigation-compose | 2.8.5 |
| androidx.hilt:hilt-navigation-compose | 1.2.0 |
| androidx.hilt:hilt-work | 1.2.0 |
| androidx.work:work-runtime-ktx | 2.9.0 |
| androidx.room:room-runtime / ktx / compiler (ksp) | 2.6.1 |
| androidx.datastore:datastore-preferences | 1.1.1 |
| com.google.dagger:hilt-android (+ compiler ksp) | 2.51.1 |
| com.google.code.gson:gson | 2.10.1 |
| com.android.tools:desugar_jdk_libs | 2.0.4 |
| com.github.skydoves:colorpicker-compose | 1.0.0 |
| compose-bom | 2024.09.00 |

## Files that would define a stack but do NOT exist

Confirmed absent from the repository root and subfolders: `package.json`, `Dockerfile`, `*.{yml,yaml}` CI definitions, `*.tf` Terraform, Helm charts, `requirements.txt`, backend code.