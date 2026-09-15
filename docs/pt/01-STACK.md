# FlowFinance — Stack Técnica

**Data do snapshot do repositório:** 2026-09-14 · **Versão do app:** 2.0.1 (`versionCode 5`)

## Tabela-resumo

| Camada | Tecnologia | Versão / Configuração | Observações |
|--------|-----------|----------------------|-------------|
| Linguagem | Kotlin | 2.0.21 | 100% Kotlin, alvo JVM 11 |
| UI | Jetpack Compose + Material 3 | compose-bom 2024.09.00 | UI declarativa, tema claro/escuro + Dynamic Color |
| SDK Android | `compileSdk` 35, `targetSdk` 35, `minSdk` 24 | AGP 8.13.2 | Desugaring da biblioteca core habilitado |
| Build | Gradle | 9.5.1 (wrapper) | Kotlin DSL, version catalog (`gradle/libs.versions.toml`) |
| Geração de código | KSP | 2.0.21-1.0.27 | Processamento de anotações Room + Hilt |
| DI | Hilt (Dagger) | 2.51.1 (+ hilt-work 1.2.0, hilt-navigation-compose 1.2.0) | Injeção singleton + ViewModel |
| Persistência / ORM | Room (SQLite) | 2.6.1 | **Sem ORM/query builder** — SQL cru nos DAOs via anotações |
| Preferências | Jetpack DataStore Preferences | 1.1.1 | Arquivo: `settings` |
| Async | Kotlin Coroutines + Flow | (embutido no Kotlin) | Reativo, UI dirigida por `StateFlow`/`Flow` |
| Background | WorkManager | 2.9.0 | Periódicos: backup semanal, lembrete semanal, verificação diária de recorrentes (WIP) |
| Serialização | Gson | 2.10.1 | Arquivos de backup, serialização de modelos |
| Criptografia | javax.crypto + AndroidKeyStore | — | AES/GCM, PBKDF2-HmacSHA256 (ver 07-SEGURANCA) |
| Gráficos | Compose `Canvas` manual | — | Sem biblioteca de gráficos; charts customizados em `ui/components/*Charts.kt` |
| Seletor de cor | `com.github.skydoves:colorpicker-compose` | 1.0.0 | Seleção de cor de categorias |
| Auth | **Nenhuma** | — | App 100% offline; sem autenticação, sem camada de rede |
| Testes | JUnit 4, Espresso, Compose UI test | — | Apenas testes-exemplo padrão |
| i18n | Recursos Android | EN (padrão), PT-BR, ES | Troca de idioma + locales por atividade |
| Release | R8 / ProGuard | `proguard-rules.pro` | Shrinking + shrinking de recursos habilitados |
| Deploy/CI | **Nenhum** | — | Sem Docker, CI, Helm ou Terraform. APK em `app/release/`, distribuído via GitHub Releases |

## Linguagem e plataforma

- **Kotlin 2.0.21**, `kotlinOptions.jvmTarget = 11`, `compileOptions` Java 11.
- Android Gradle Plugin **8.13.2**; distribuição Gradle **9.5.1** via wrapper.
- Pacote / applicationId: `com.flowfinance.app`. Suporte de Android 24 (legado) até 35 (moderno).

## Framework de UI

- **Jetpack Compose** com biblioteca **Material 3** e `material-icons-extended`.
- Plugin Compose do Kotlin 2.0 (compilador de composables).
- Telas, componentes, tema e navegação são todos Compose. `MainActivity` é uma `AppCompatActivity` que hospeda um `NavHost`.
- Dois gráficos customizados desenhados com `Canvas` (sem dependência de terceiros):
  - `FinancialFlowCharts.kt` (multilinha, barras, área, combinado, tooltips, pan/zoom).
  - `CategoryTrendsCharts.kt` (pizza, barras horizontais, multilinha, área empilhada).
  - `PieChart.kt` (donut animado usado no Dashboard).

## Persistência e dados

- **Room 2.6.1**, banco `"flowfinance_db"`:
  - `transactions`, `categories` (v1, commitada) e `recurring_transactions` (v2, **WIP/não commitada**).
  - `LocalDate` armazenado como string ISO; `TransactionType` e `RecurrenceFrequency` armazenados como nomes de enum (via `TypeConverter`s do Room).
- SQL cru nos DAOs para agregação (sem query builder / API relacional de ORM):
  - `TransactionDao` — filtros por intervalo de datas, `SUM`, `JOIN` com agrupamento por categoria para `CategorySummary`.
  - `CategoryDao`, e novo `RecurringTransactionDao` (WIP).
- **Jetpack DataStore Preferences** para configurações do usuário (`user_name`, `currency`, `is_dark_theme`, config de lembrete, `language`).

## Injeção de dependência

- **Hilt 2.51.1**: aplicação `@HiltAndroidApp`, activity `@AndroidEntryPoint`, ViewModels `@HiltViewModel`, workers `@HiltWorker` + `@AssistedInject`.
- `DatabaseModule` (escopo Singleton) fornece `AppDatabase`, DAOs e categorias padrão iniciais.
- `WorkerFactory` do Hilt conectada via `Configuration.Provider` em `FlowFinanceApplication`.

## Processamento em segundo plano

O WorkManager é inicializado via Hilt (`setWorkerFactory`). Workers:

| Trabalho | Chave | Agendamento | Objetivo |
|---------|-------|-------------|----------|
| `BackupWorker` | `"WeeklyBackup"` | a cada 7 dias, exige carregamento | CSV criptografado das transações (AES via AndroidKeyStore) |
| `NotificationWorker` | `"WeeklyReminder"` | Domingo 09:00 semanal | Lembrete; além disso verificações pontuais de orçamento / teste de notificação |
| `RecurringTransactionWorker` (WIP) | `"RecurringTransactionsCheck"` / `"RecurringTransactionsImmediateCheck"` | diário + uma execução imediata | Gera transações recorrentes a partir das regras |

## Autenticação

- **Nenhuma.** Não há login, conta, token ou interação com servidor em lugar algum do código. O app é totalmente local; a única interação externa é com serviços do sistema (notificações, MediaStore, compartilhamento via FileProvider).

## Rede / backend

- **Nenhuma.** Sem OkHttp/Retrofit/Ktor, sem permissão de INTERNET, sem camada de API, sem fonte de dados externa.

## Deploy / CI / containers

- **Sem Dockerfile, sem GitHub Actions / config de CI, sem Helm, sem Terraform**, sem `docker-compose`, sem arquivos de deploy de servidor.
- Distribuição: **APK release** assinado gerado localmente (`app/release/FlowFinance-v2.0.1.apk`, com baseline profiles em `app/release/baselineProfiles/`).
- Releases publicados na página **Releases** do GitHub em `Gabrick75/FlowFinance`.

## Testes

- `app/src/test/` — `ExampleUnitTest.kt` (placeholder JUnit 4).
- `app/src/androidTest/` — `ExampleInstrumentedTest.kt` / placeholder Espresso (runner `androidx.test.runner.AndroidJUnitRunner`).
- Bibliotecas de teste de UI Compose declaradas nas dependências, mas não exercitadas.

## Principais bibliotecas de terceiros (versões exatas)

| Dependência | Versão |
|-------------|--------|
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
| com.google.dagger:hilt-android (+ compiler via ksp) | 2.51.1 |
| com.google.code.gson:gson | 2.10.1 |
| com.android.tools:desugar_jdk_libs | 2.0.4 |
| com.github.skydoves:colorpicker-compose | 1.0.0 |
| compose-bom | 2024.09.00 |

## Arquivos que normalmente definiriam uma stack e NÃO existem

Confirmado ausente na raiz e subpastas: `package.json`, `Dockerfile`, `*.{yml,yaml}` de CI, `*.tf` Terraform, charts Helm, `requirements.txt`, código de backend.