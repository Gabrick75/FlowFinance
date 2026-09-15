# FlowFinance — Guia de Funcionalidades

Versão do app 2.0.1. Navegação é Compose com activity única; toda tela reage automaticamente a mudanças no Room/DataStore.

```
Bottom bar:  Dashboard | Transactions | Planning | Panel | Settings
```

## 1. Dashboard (início)
- **Saldo Total** — receitas menos despesas de todos os tempos.
- Indicadores mensais **Receitas / Despesas** (verde `#4CAF50`, vermelho `#EF5350`).
- **Despesas por Categoria** — donut animado do mês atual + legenda.
- **Transações Recentes** (5 últimas) com ícones de categoria; **Ver tudo** → Histórico.
- FAB `+` abre a planilha/folha Nova Transação.

## 2. Transactions (histórico)
- Navegar por **mês** (setas) e **buscar** (descrição ou categoria).
- Lista agrupada por dia com **cabeçalhos de data fixos** mostrando o total de gastos do dia.
- **Deslizar para excluir** (para a esquerda) com fundo vermelho de confirmação.

## 3. Planning
- **Planning** — gastos do mês atual por categoria de despesa vs `budgetLimit`, barras de progresso.
- **Manage Budgets** (via FAB):
  - Criar categorias customizadas: nome (≤25 chars), **seletor de cor HSV**, seletor de ícone.
  - Editar nome/cor/ícone (toque longo), editar orçamento inline.
  - Excluir via swipe; **categorias padrão não podem ser excluídas**.
  - Quando um orçamento é definido, **alertas de orçamento** disparam em 50 / 70 / 90 / 100 % da meta (NotificationWorker).

## 4. Panel — análises avançadas

### Financial Flow
- Evolução mês a mês da primeira transação até hoje:
  - `Salário` (receita fora da categoria "Rendimentos"), `Rendimento Mensal`, `Rendimento Acumulado`, `Saldo Acumulado`, `Patrimônio Total`.
- Cards de gráficos: General Overview (multilinha com 5 séries), barras de salário, área de rendimentos, combinado. Cada um abre **tela cheia** com zoom, pan, tooltips, legenda informativa e **exportação PNG**.
- **Planilha detalhada**: tabela estilo planilha + **exportação CSV**.

### Category Trends
- Carrossel de pizzas (Desde sempre vs mês atual), ranking de gastos (abas total/mensal, top 5), tendência multilinha mensal, composição em área empilhada.
- **Planilha** dinâmica detalhada + exportação CSV. Categorias de receita são excluídas das tendências de despesa.

### Expense Analysis
- **Médias** diária / semanal / mensal.
- **Picos de gasto**: pico de dia da semana (maior média normalizada) e dia do mês (maior volume).
- **Heatmap semanal** de intensidade de gastos.
- Categorias **recorrentes vs ocasionais** (recorrente = ≥ 1 transação/mês na média).

### Financial Summary
- Três tabelas-resumo — Total Geral, Ano Atual, Mês Atual — cada uma com **Valor Total / Total Gasto / Total Não Gasto** (+ %), com popup explicativo.

### Monthly History
- 12 cards de resumo mensal do ano atual (também acessível a partir do Financial Summary).

## 5. Configurações

| Seção | Funcionalidade |
|-------|----------------|
| Perfil | nome de usuário, moeda (BRL/USD/EUR), idioma (padrão/en/pt-BR/es). Atualização instantânea da UI. |
| Preferências | switch de tema escuro (sobrepõe o sistema); **configuração de notificações** (hora via TimePickerDialog, intervalo 1–30 dias, notificação de teste). |
| Dados | Exportar planilha `.xls` (5 abas); Limpar dados (confirmação digitada). |
| Backup | Exportar `.flowbackup` (criptografado por senha); Importar (pré-visualização de metadados + senha; substitui todos os dados). |
| Sobre | versão, fonte no GitHub, links de documentação. |

- **Backup** semanal automático criptografado (WorkManager, exige carregamento) e **lembrete** semanal (padrão domingo 09:00).

## 6. Criação de transações (New Transaction sheet)
- Controle segmentado de tipo (Receita / Despesa), valor (decimal), descrição (≤35 chars), chips de categoria (a lista muda por tipo), seletor de data. Filtro por tipo:
  - **Categorias de despesa**: tudo exceto "Salário" e "Rendimentos".
  - **Categorias de receita**: "Salário", "Investimentos", "Rendimentos" + custom não-padrão.

## 7. i18n
- Inglês (padrão), Português (Brasil), Espanhol — seleção manual ou padrão do sistema (`AppCompatDelegate.setApplicationLocales`).
- Lacuna conhecida: algumas telas do Panel ainda fixam strings em português.

## 8. Funcionalidades automáticas / em segundo plano
- **Backup semanal**: CSV criptografado no armazenamento do app (AndroidKeyStore).
- **Lembrete semanal**: domingo 09:00 (padrão).
- **Alertas de orçamento**: após lançar despesa, uma checagem em background posta notificações em 50/70/90/100%.
- **Transações recorrentes (WIP)**: worker diário vai gerar automaticamente transações recorrentes vencidas (camada de dados pronta, ainda sem UI).

## 9. Detalhes da exportação de dados (.xls)
`exportDataToCsv` produz um arquivo Excel 2003 SpreadsheetML com 5 abas:
1. **Transacoes** — id, descrição, valor, data, tipo, categoria.
2. **Categorias** — id, nome, orçamento mensal, flag padrão.
3. **Fluxo Financeiro** — por mês: salário / rendimento mensal / rendimento acumulado / saldo acumulado / patrimônio total (mesma matemática da tela Financial Flow).
4. **Tendencia por Categoria** — dinâmica meses × categorias (despesas; categorias de receita excluídas).
5. **Resumo Financeiro** — totais de todos os tempos / ano atual / mês atual.

## 10. Exportação de gráficos/PNG
Gráficos em tela cheia podem ser salvos em Imagens via MediaStore (`saveBitmapToFile`).