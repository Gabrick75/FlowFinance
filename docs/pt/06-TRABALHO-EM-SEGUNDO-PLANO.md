# FlowFinance — Trabalho em Segundo Plano (WorkManager)

WorkManager 2.9.0. O app é `Application, Configuration.Provider`; a **Hilt worker factory** é injetada e usada para todos os workers (`@HiltWorker` + `@AssistedInject`), de modo que todos os workers podem usar repositórios.

```kotlin
// FlowFinanceApplication.kt
override val workManagerConfiguration: Configuration
    get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
```

O manifest desativa o initializer padrão do WorkManager para usar o ciente de Hilt.

## Agendamento na inicialização (`FlowFinanceApplication.onCreate()`)

| Nome do trabalho único | Worker | Constraints / período | Política | Objetivo |
|------------------------|--------|-----------------------|----------|----------|
| `"WeeklyBackup"` | `BackupWorker` | 7 dias, **exige carregamento** | `KEEP` | Backup CSV criptografado das transações |
| `"WeeklyReminder"` | `NotificationWorker` | primeira execução no próximo domingo 09:00, depois a cada 7 dias | `UPDATE` | Lembrete semanal para lançar gastos |
| `"RecurringTransactionsCheck"` *(WIP)* | `RecurringTransactionWorker` | a cada 1 dia | `KEEP` | Gerar transações recorrentes vencidas |
| `"RecurringTransactionsImmediateCheck"` *(WIP)* | `RecurringTransactionWorker` | pontual (REPLACE) | `REPLACE` | Alcançar o que venceu ao abrir o app |

> O lembrete semanal também é (re)agendável em Configurações (hora, intervalo 1–30 dias, ativar/desativar), que enfileira/cancela `"WeeklyReminder"` via `ExistingPeriodicWorkPolicy.UPDATE`.

## 1. BackupWorker (`worker/`)

- Disparado apenas pelo agendamento periódico `"WeeklyBackup"` (7 dias, exige carregamento).
- Lê todas as transações `.first()`, monta **CSV** (colunas `Id,Description,Amount,Date,Type,CategoryId`).
- **Proteção contra injeção de fórmula em CSV:** campos vêm entre aspas e valores que começam com `=`, `+`, `-`, `@` são neutralizados com um `'` inicial (ver `csvEscape`).
- Grava `backup_transactions_<timestamp>.csv.enc` em `filesDir`, **criptografado com a chave do AndroidKeyStore** (`CryptoUtils.encryptWithKeystoreKey`) — sem senha do usuário.
- Loga sucesso/erro; retorna `Result.success()/failure()`.

## 2. NotificationWorker (`workers/`)

Despacha por `key_notification_type` (input data):

| Constante de tipo | Comportamento |
|--------------------|---------------|
| `TYPE_WEEKLY_REMINDER` | Mostra a notificação de lembrete semanal (id `1001`). |
| `TYPE_BUDGET_CHECK` | Lê categorias + resumos de despesa do mês atual; para cada categoria com orçamento > 0, posta alertas nos limiares **50 / 70 / 90 / 100 %** (id da notificação = `category.id`). |
| `TYPE_TEST` | Notificação de teste (id `1002`). |

Criação encapsulada da notificação: canal `flow_finance_channel` ("Lembretes e Alertas", criado no API 26+), checagem de permissão `POST_NOTIFICATIONS` no Android 13+, toque abre `MainActivity` via `PendingIntent` imutável. Desde o PR de transações recorrentes, o encanamento de notificação foi movido para `util/NotificationHelper.kt`, e `NotificationWorker.showNotification(...)` apenas delega a ele.

## 3. RecurringTransactionWorker (`worker/`) *(WIP, não commitado)*

- Executa periodicamente (diário) + imediatamente na inicialização (pontual).
- Para cada `RecurringTransaction` **ativa**, gera todas as ocorrências vencidas (data da ocorrência ≤ hoje e ≤ `endDate`):
  - Avança por `occurrenceIndex` a partir do `startDate` da regra via `RecurrenceFrequency.occurrenceDate(...)` — **ancorado na data inicial original** para evitar deriva de dia do mês (ex.: 31 jan → 28 fev → 28 mar).
  - Insere um `Transaction` real por ocorrência na categoria da regra.
  - Atualiza `occurrencesGenerated` e desativa a regra (`isActive = false`) depois de passar de `endDate`.
- Se alguma ocorrência foi gerada, mostra uma notificação-resumo (id `1003`).

### IDs de notificação usados
- `1001` lembrete semanal · `1002` teste · `1003` recorrentes geradas · `category.id` alertas de orçamento.

## Observações de coordenação
- Todos os enqueues são **únicos** (`enqueueUniquePeriodicWork` / `enqueueUniqueWork`), então existe apenas uma cadeia por chave.
- Workers são `CoroutineWorker` (suspensíveis) com `@HiltWorker`.
- `BackupWorker` e `RecurringTransactionWorker` leem/gravam no armazenamento interno; sem UI de progresso visível ao usuário (resultado comunicado via log do sistema + notificações).