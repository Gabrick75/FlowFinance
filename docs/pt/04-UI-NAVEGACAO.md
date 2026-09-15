# FlowFinance — UI, Navegação & Tema

Raiz do código de UI: `app/src/main/java/com/flowfinance/app/ui/`

## 1. Visão geral da navegação

- **Activity única**: `MainActivity` (`@AndroidEntryPoint`, estende `AppCompatActivity`), solicita `POST_NOTIFICATIONS` no Android 13+, chama `enableEdgeToEdge()` e define o conteúdo Compose que monta toda a UI.
- **Navegação real**: `NavHost` definido inline em `MainActivity` (o arquivo `ui/navigation/NavigationGraph.kt` é um stub morto — não usar).
- **Rotas** (`ui/navigation/Screen.kt`), uma `sealed class Screen(route, titleRes, icon)`:

| Rota | Tela | Na bottom bar | Observações |
|------|------|---------------|-------------|
| `dashboard` | DashboardScreen | sim | FAB abre AddTransactionSheet |
| `transactions` | TransactionsScreen | sim | histórico completo |
| `planning` | PlanningScreen | sim | FAB → ManageBudgets |
| `panel` | PanelScreen | sim | hub de análises |
| `settings` | SettingsScreen | sim | |
| `user_profile` | UserProfileScreen | não | → a partir de Settings |
| `manage_budgets` | ManageBudgetsScreen | não | |
| `financial_flow` | FinancialFlowScreen | não | → Sheet, ChartDetail |
| `sheet` | SheetScreen | não | |
| `category_trends` | CategoryTrendsScreen | não | → CategoryTrendsSheet, ChartDetail |
| `category_trends_sheet` | CategoryTrendsSheetScreen | não | |
| `expense_analysis` | ExpenseAnalysisScreen | não | |
| `financial_summary` | FinancialSummaryScreen | não | → MonthlyHistory |
| `monthly_history` | MonthlyHistoryScreen | não | |
| `chart_detail/{chartType}` | FullScreenChartScreen | não | argumento `chartType` (padrão `"overview"`) |

- **Comportamento do Scaffold**:
  - A `NavigationBar` aparece apenas nas 5 rotas principais (whitelist de `currentRoute`).
  - Navegação da bottom bar usa `popUpTo(startDestination){saveState=true}`, `launchSingleTop=true`, `restoreState=true`.
  - `FloatingActionButton`: Dashboard → abre o `AddTransactionSheet` global; Planning → navega para `ManageBudgets`.
  - O `AddTransactionSheet` global é renderizado no nível da activity (`if (showBottomSheet)`), podendo ser reutilizado em qualquer rota.

## 2. Telas por feature

### Dashboard (`screens/dashboard/DashboardScreen.kt`)
- Card **Saldo Total** (receitas − despesas de todos os tempos), indicadores mensais **Receitas** / **Despesas** (verde/vermelho).
- Donut **Despesas por Categoria** (`PieChart`) + legenda manual.
- **Transações Recentes** (5) via componente reutilizável `TransactionItem`; "Ver tudo" → `TransactionsScreen`.
- Usa `DashboardViewModel` (reativo).

### Transactions (`screens/transactions/TransactionsScreen.kt`)
- Navegação por mês (`previousMonth()`/`nextMonth()`), **caixa de busca** (descrição + nome da categoria), lista agrupada por data com **cabeçalhos fixos** (data + total de gasto do dia).
- **Swipe para excluir** (`SwipeToDismissBox`, EndToStart) sobre fundo vermelho animado.
- Estado vazio. Usa `TransactionsViewModel`.

### Planning (`screens/planning/PlanningScreen.kt` + `ManageBudgetsScreen.kt`)
- Planning: barras de progresso por categoria `gasto / orçamento` (`LinearProgressIndicator` colorida pela categoria).
- ManageBudgets: CRUD de categorias com seletor de cor (`HsvColorPicker`), seletor de ícone (`getCategoryIconMap`), edição inline de orçamento, swipe para excluir (bloqueado para categorias padrão), toque longo para editar. Usa `ManageBudgetsViewModel`.
- `rememberCategoryIcon(iconKey)` vive aqui e é reutilizado em todo o app (Dashboard, AddTransactionSheet).

### Panel — hub de análises (`screens/panel/PanelScreen.kt`)
- Hospeda uma prévia do `GeneralOverviewChart` + card de resumo anual, e 4 botões → FinancialFlow, CategoryTrends, ExpenseAnalysis, FinancialSummary.

### Panel — Financial Flow (`FinancialFlowScreen.kt`, `SheetScreen.kt`, subconjunto de `FullScreenChartScreen.kt`)
- `FinancialFlowScreen`: cards de gráficos interativos (multilinha geral, barras de salário, área de rendimentos, combinado) + botão "Mostrar planilha".
- `SheetScreen`: tabela estilo planilha (data, salário, rendimento mensal/acumulado, saldo acumulado, patrimônio) + exportar CSV/compartilhar.
- Gráficos suportam **pan, zoom (1–5x)** e **tooltips** por toque.

### Panel — Category Trends (`CategoryTrendsScreen.kt`, `CategoryTrendsSheetScreen.kt`)
- Carrossel de pizzas (`HorizontalPager`: total geral / mês atual), ranking com `TabRow` (total/mensal, top 5 barras horizontais), gráfico multilinha mensal, gráfico de área empilhada, planilha detalhada (tabela dinâmica) + exportar CSV.

### Panel — Expense Analysis (`ExpenseAnalysisScreen.kt`)
- Médias (cards diário/semanal/mensal), picos (dia da semana e dia do mês), **heatmap semanal**, abas recorrentes/ocasionais por categoria (regra de recorrência: `count / monthsDiff >= 1.0`).

### Panel — Financial Summary & Monthly History
- `FinancialSummaryScreen`: três `SummaryTableCard`s (total geral, ano atual, mês atual) com linhas Total/Gastos/Restante e popup de legenda; botão "Ver Histórico Mensal".
- `MonthlyHistoryScreen`: 12 `SummaryTableCard`s, um por mês do ano atual.

### Gráfico em tela cheia (`FullScreenChartScreen.kt`)
- Despacha por `chartType`: `category_trends_line` / `category_trends_stacked` → branch CategoryTrends; caso contrário branch FinancialFlow.
- Renderiza o gráfico em tamanho cheio, com legenda, popup informativo e **salvar como PNG** (`saveBitmapToFile` → MediaStore).

### Configurações (`screens/settings/SettingsScreen.kt`, `UserProfileScreen.kt`)
- Hub de configurações em seções: Perfil (→UserProfile), Preferências (switch de tema escuro; diálogo de notificações com `TimePickerDialog`, stepper de intervalo 1–30 dias, botão de teste), Dados (exportar planilha .xls, limpar dados com confirmação digitada), Backup (exportar/importar backups criptografados com diálogos de senha), Sobre (versão, GitHub, docs).
- Helper top-level `shareFile(context, path, isBackup)` (FileProvider + ACTION_SEND), reutilizado por Sheet/CategoryTrendsSheet.
- UserProfile: nome, rádio de moeda (BRL/USD/EUR), rádio de idioma (padrão/en/pt-BR/es) → salva via `SettingsViewModel`.

## 3. Componentes reutilizáveis (`ui/components/`)

| Componente | Finalidade |
|-----------|------------|
| `AddTransactionSheet` | `ModalBottomSheet` para criar receita/despesa: seletor segmentado de tipo, valor (decimal), descrição (35 chars), linha de categorias (`CategoryChip` círculos de 50dp), seletor de data via interação de press. Salva por `AddTransactionViewModel`. |
| `PieChart` | Donut animado (`drawArc` strokes, tween de 1s), fatia por `CategorySummary`. Pizza do Dashboard. |
| `FinancialFlowCharts` | `GeneralOverviewChart` (multilinha com 5 séries), `SalaryBarChart`, `YieldAreaChart` (gradiente), `CombinedChart`; paleta compartilhada (`ColorSalary`, `ColorYield`, `ColorAccYield`, `ColorBalance`, `ColorWealth`); grade + tooltip + pan/zoom. |
| `CategoryTrendsCharts` | `CategoryPieChart`, `CategoryHorizontalBarChart`, `CategoryTrendsLineChart`, `CategoryStackedAreaChart`; cópia própria de grade/tooltip. |

Elementos de UI compartilhados entre telas: `TransactionItem` (Dashboard→Transactions), `SummaryTableCard` (FinancialSummary→Panel/MonthlyHistory), `ChartCard` (FinancialFlow→Panel), `rememberCategoryIcon` (planning→em todo lugar), `shareFile` (Settings→Sheet/TrendsSheet), `formatCurrency` (util, em todo lugar).

## 4. Tema (`ui/theme/`)

**Cores** (`Color.kt`): azul-marinho de marca `NavyBlue80 #B0C4DE / NavyBlue40 #2C3E50 / NavyBlueDark #1A252F`; semânticas `GreenIncome #4CAF50`, `RedExpense #EF5350`; acentos roxo/rosa Material para secundária/terciária; `SurfaceDark #121212`, `SurfaceLight #F5F5F5`.

**Tema** (`Theme.kt`):
- `darkColorScheme(primary=NavyBlue80, ...)` / `lightColorScheme(primary=NavyBlue40, ...)`.
- `FlowFinanceTheme(darkTheme, dynamicColor = true)`: usa `dynamicDark/LightColorScheme` no Android 12+ (SDK ≥ S); caso contrário os schemes estáticos. `darkTheme` vem de `SettingsViewModel.userData.isDarkTheme` (preferência do usuário vence o sistema).

**Tipografia** (`Type.kt`): apenas `bodyLarge` sobrescrito (16.sp/24.sp/0.5.sp); o restante são padrões Material. Gráficos usam tamanhos pequenos fixos (9.sp–10.sp em rótulos de eixo).

## 5. Internacionalização

- Recursos: `values` (EN padrão), `values-pt-rBR`, `values-es`.
- ~240 strings em `values/strings.xml` cobrindo navegação, todas as telas e diálogos.
- Troca de idioma em tempo de execução via `SettingsViewModel.updateLanguage` → `AppCompatDelegate.setApplicationLocales` (escopo por atividade, exige "per-activity locales" no manifest).
- Lacuna conhecida: várias telas do Panel (CategoryTrends, CategoryTrendsSheet, FullScreenChart, MonthlyHistory, ManageBudgets, Sheet) ainda fixam strings em português em vez de `stringResource`.

## 6. Observações de qualidade de código na UI

- Helpers privados duplicados `ChartTooltip`, `drawStandardChartGrid`, `getIndexFromTap` existem em `FinancialFlowCharts.kt` e `CategoryTrendsCharts.kt`.
- `ui/navigation/NavigationGraph.kt`, `PatternsAnalysisScreen.kt`, `PatternsSheetScreen.kt` são stubs/placeholders e não devem ser estendidos.