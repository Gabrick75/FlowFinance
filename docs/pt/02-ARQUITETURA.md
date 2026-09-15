# FlowFinance — Arquitetura

## 1. Padrão arquitetural

**MVVM (Model-View-ViewModel)** com fluxo de dados reativo e unidirecional, dirigido por `Flow` do Kotlin.

```
┌─────────────────────────────────────────────────────────────┐
│  UI  (Compose)                                               │
│  Telas: Dashboard, Transactions, Planning, Panel, Settings   │
│  Componentes: AddTransactionSheet, Charts, TableCards        │
│        │  collectAsState() sobre StateFlow / SharedFlow      │
│        ▼                                                     │
├─────────────────────────────────────────────────────────────┤
│  ViewModel  (@HiltViewModel)                                 │
│  Expõe StateFlow<UiState>; converte dados brutos em estados  │
│  de UI e agregações (gráficos, resumos, análises)            │
│        │  chamadas de repositório (suspend / Flow)           │
│        ▼                                                     │
├─────────────────────────────────────────────────────────────┤
│  Repository  (@Singleton)                                    │
│  TransactionRepository · CategoryRepository                  │
│  RecurringTransactionRepository (WIP) · UserPreferencesRepository│
│        │                                                     │
│        ▼                                                     │
├─────────────────────────────────────────────────────────────┤
│  Dados locais                                                │
│  Room AppDatabase (DAO)          DataStore Preferences       │
│  SQLite  "flowfinance_db"        arquivo "settings"          │
└─────────────────────────────────────────────────────────────┘
                 ▲
                 │ injetado via Hilt (DatabaseModule)
```

Características-chave:

- **Reativo por construção.** Os DAOs do Room retornam `Flow`, que os ViewModels combinam em `StateFlow`. Qualquer alteração de linha é refletida instantaneamente nas telas abertas (saldos, gráficos, listas) sem atualização manual.
- **Fonte única de verdade:** SQLite local; todas as análises são calculadas a partir das mesmas tabelas `transactions`/`categories`.
- **Sem rede, sem auth.** O app nunca sai do dispositivo exceto por serviços do sistema.

## 2. Organização de pacotes (`app/src/main/java/com/flowfinance/app`)

| Pacote | Responsabilidade |
|--------|------------------|
| `MainActivity.kt` | Activity única; hospeda `NavHost`, barra inferior, FABs e o `AddTransactionSheet` global. Solicita POST_NOTIFICATIONS no Android 13+. |
| `FlowFinanceApplication.kt` | `@HiltAndroidApp`; fornece `Configuration` do WorkManager via HiltWorkerFactory; agenda jobs de backup / lembrete / verificação de recorrentes no start. |
| `data/entity` | Entidades Room (`Transaction`, `Category`, `RecurringTransaction` WIP). |
| `data/dao` | DAOs Room com agregações SQL cruas. |
| `data/local/converters` | `TypeConverter`s do Room (LocalDate, enums). |
| `data/local/model` | Modelos de relação (`TransactionWithCategory`, `CategorySummary`) e modelos de backup (`BackupFile`/`BackupPayload`). |
| `data/repository` | Camada de repositório que embrulha os DAOs; um `@Singleton` por repositório. |
| `data/preferences` | `UserPreferencesRepository` + snapshot `UserData` sobre DataStore. |
| `di` | `DatabaseModule`: builder do Room, categorias padrão iniciais, providers de DAO. |
| `ui/screens` | Telas por feature (`dashboard`, `transactions`, `planning`, `panel`, `settings`). |
| `ui/components` | Composable reutilizáveis (AddTransactionSheet, FinancialFlowCharts, CategoryTrendsCharts, PieChart). |
| `ui/viewmodel` | 13+ ViewModels. |
| `ui/navigation` | `Screen` (rotas) e um `NavigationGraph` stub (não usado; o grafo real está em `MainActivity`). |
| `ui/theme` | Paleta de cores, color schemes, tipografia. |
| `worker`, `workers` | Workers do WorkManager (`BackupWorker`, `RecurringTransactionWorker`; `NotificationWorker`). |
| `util` | `CryptoUtils`, `TransactionType`, `RecurrenceFrequency` (WIP), `NotificationHelper` (WIP), extensões de moeda/data/salvar bitmap. |

## 3. Navegação de activity única

- Uma activity, um `NavHost` (definido em `MainActivity`, **não** em `ui/navigation/NavigationGraph.kt`, que é um stub morto).
- Rotas declaradas em `Screen.kt` (sealed class): 5 destinos da bottom bar (`dashboard`, `transactions`, `planning`, `panel`, `settings`) além de sub-telas e a rota parametrizada `chart_detail/{chartType}`.
- A `NavigationBar` inferior é ocultada nas sub-telas via whitelist de rotas; um `FloatingActionButton` aparece no Dashboard (nova transação) e no Planning (administrar orçamentos).

## 4. Padrões dos ViewModels

- Todos os ViewModels são `@HiltViewModel` com injeção por construtor.
- Estado de UI é uma data class dentro de `StateFlow`, com valores-padrão seguros para a UI renderizar imediatamente.
- Coexistem duas estratégias de carregamento:
  - **Reativa**: `combine(flowDoRepositorio, preferenciasDoUsuario)` recalcula o estado a cada mudança no banco (Dashboard, Transactions, Planning, FinancialFlow, FinancialSummary).
  - **Ponteira (`one-shot`)**: leitura via `.first()` em `init` (MonthlyHistory, CategoryTrends, ExpenseAnalysis) — mais barata, mas não auto-atualiza se o usuário alterar dados com a tela aberta.
- Mutações são simples: `viewModelScope.launch { repository.insert/update/delete(...) }`, deixando o fluxo reativo empurrar o novo estado.
- Após mutações de despesa, alguns ViewModels enfileiram um `NotificationWorker` pontual (verificação de orçamento) para manter os alertas em dia.

## 5. Inicialização do trabalho em segundo plano

`FlowFinanceApplication.onCreate()` agenda três cadeias WorkManager únicas:

1. `"WeeklyBackup"` — `BackupWorker`, período de 7 dias, exige carregamento, política `KEEP`.
2. `"WeeklyReminder"` — `NotificationWorker` (`TYPE_WEEKLY_REMINDER`), primeira execução no próximo domingo às 09:00, período de 7 dias, política `UPDATE`.
3. (WIP) `"RecurringTransactionsCheck"` (periódico diário, `KEEP`) + `"RecurringTransactionsImmediateCheck"` (pontual, `REPLACE`) — `RecurringTransactionWorker`.

Hora/frequência do lembrete também podem ser alteradas em tempo de execução pelas Configurações, que reagenda `"WeeklyReminder"` (ou o cancela).

## 6. Modelo de segurança (resumo)

- Backups por senha: PBKDF2 (65536 iterações, salt de 16 bytes) → AES-256/GCM, saída `salt‖iv‖texto_cifrado` em Base64.
- Backups automáticos/workers: AES-256/GCM com chave não-exportável no AndroidKeyStore.
- ProGuard + Gson mantém entidades Room e modelos de backup.
- Sem segredos em texto claro; sem rede para exfiltração.

## 7. Código morto / obsoleto conhecido (documentado para mantenedores)

| Arquivo | Situação |
|---------|----------|
| `ui/navigation/NavigationGraph.kt` | Stub com composables vazios; **não usado** (a navegação está em `MainActivity`). |
| `ui/screens/panel/PatternsAnalysisScreen.kt`, `PatternsSheetScreen.kt` | Placeholders vazios; funcionalidade movida para as telas de CategoryTrends. |
| `ui/viewmodel/PatternsAnalysisViewModel.kt` | Placeholder em branco (artefato de conflito de build). |
| `ui/viewmodel/EditTransactionViewModel.kt` | Órfão — nenhuma tela o referencia (a UX de edição foi substituída pelo fluxo do AddTransactionSheet). |
| `PanelScreen.onNavigateToPatternsAnalysis` | Callback ainda declarado, mas leva ao destino CategoryTrends, não a uma tela Patterns. |

## 8. Feature WIP (não commitada, em andamento)

**Transações recorrentes** — camada de dados + worker prontos, mas **ainda sem UI**:

- Entidade `RecurringTransaction` + `RecurringTransactionDao` + `RecurringTransactionRepository`.
- **Migração Room 1→2** cria `recurring_transactions` (+ índice) — `AppDatabase` subiu para versão 2.
- `RecurringTransactionWorker` gera transações recorrentes vencidas diariamente (e imediatamente na inicialização do app).
- Enum `RecurrenceFrequency` + helper `occurrenceDate()` ancorado na data inicial da regra.
- `NotificationHelper` extraído (lógica de notificação compartilhada com `NotificationWorker`).
- `AddTransactionViewModel` já injeta `RecurringTransactionRepository` (preparação de DI para a futura UI).

Tudo nesta seção ainda pode mudar; trate como "em desenvolvimento".