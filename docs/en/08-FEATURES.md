# FlowFinance — Features Guide

App version 2.0.1. Navigation is single-activity Compose; every screen reacts to Room/DataStore changes automatically.

```
Bottom bar:  Dashboard | Transactions | Planning | Panel | Settings
```

## 1. Dashboard (home)
- **Total Balance** — all-time income minus expenses.
- **Monthly Income / Expenses** indicators (green income `#4CAF50`, red expense `#EF5350`).
- **Expenses by Category** — animated donut for the current month + legend.
- **Recent Transactions** (latest 5) with category icons; **See all** → History.
- FAB `+` opens the New Transaction sheet.

## 2. Transactions (history)
- Browse by **month** (arrow navigation) and **search** (description or category).
- List grouped by day with **sticky date headers** showing the day's expense total.
- **Swipe left to delete** with red confirmation background.

## 3. Planning
- **Planning** — current-month spending per expense category vs `budgetLimit`, progress bars.
- **Manage Budgets** (via FAB):
  - Create custom categories: name (≤25 chars), **HSV color picker**, icon picker.
  - Edit name/color/icon (long-press), edit budget inline.
  - Delete via swipe; **default categories cannot be deleted**.
  - When a budget is set, **budget alerts** fire at 50 / 70 / 90 / 100 % of the goal (NotificationWorker).

## 4. Panel — advanced analytics

### Financial Flow
- Month-by-month evolution from first transaction to today:
  - `Salary` (income excluding the "Rendimentos" category), `Monthly Yield`, `Accumulated Yield`, `Accumulated Balance`, `Total Wealth`.
- Chart cards: General Overview (5-series multi-line), Salary bars, Yield area, Combined. Each opens **full-screen** with zoom, pan, tooltips, legend info, and **PNG export**.
- **Detailed Sheet**: spreadsheet table + **CSV export**.

### Category Trends
- Pie carousel (All-time vs current month), spending ranking (total/monthly tabs, top 5), monthly multi-line trend, stacked-area composition.
- Detailed pivot **sheet** + CSV export. Income categories are excluded from expense trends.

### Expense Analysis
- Daily / weekly / monthly **averages**.
- **Spending peaks**: peak weekday (highest normalized average) and peak day-of-month (highest volume).
- **Weekly heatmap** of expense intensity.
- **Recurring vs occasional** categories (recurring = ≥ 1 transaction/month on average).

### Financial Summary
- Three summary tables — All-time, Current Year, Current Month — each with **Total Value / Total Spent / Total Unspent** (+ %), with an explanation popup.

### Monthly History
- 12 monthly summary cards for the current year (also accessible from Financial Summary).

## 5. Settings

| Section | Feature |
|---------|---------|
| Profile | username, currency (BRL/USD/EUR), language (default/en/pt-BR/es). Instant UI update. |
| Preferences | Dark theme switch (user override of system); **notification config** (time via TimePickerDialog, interval 1–30 days, test notification). |
| Data | Export spreadsheet `.xls` (5 worksheets); Clear data (typed confirmation). |
| Backup | Export `.flowbackup` (password-encrypted); Import (metadata preview + password; replaces all data). |
| About | version, GitHub source, docs links. |

- Automatic weekly encrypted **backup** (WorkManager, requires charging) and **weekly reminder** notification (default Sunday 09:00).

## 6. Transactions creation (New Transaction sheet)
- Type segmented control (Income / Expense), amount (decimal), description (≤35 chars), category chips (list switches per type), date picker. Type-to-category filtering:
  - **Expense categories**: all except "Salário" and "Rendimentos".
  - **Income categories**: "Salário", "Investimentos", "Rendimentos" + custom non-default.

## 7. i18n
- English (default), Brazilian Portuguese, Spanish — manual selection or system default (`AppCompatDelegate.setApplicationLocales`).
- Known gap: some Panel screens still hardcode Portuguese strings.

## 8. Automatic / background features
- **Weekly backup**: encrypted CSV in app storage (AndroidKeyStore).
- **Weekly reminder**: Sunday 09:00 default.
- **Budget alerts**: after expense entry, a background check posts notifications at 50/70/90/100%.
- **Recurring transactions (WIP)**: daily worker will auto-generate due recurring transactions (data layer ready, no UI yet).

## 9. Data export details (.xls)
`exportDataToCsv` produces an Excel 2003 SpreadsheetML file with 5 worksheets:
1. **Transacoes** — id, description, amount, date, type, category.
2. **Categorias** — id, name, monthly budget, standard flag.
3. **Fluxo Financeiro** — per-month salary / monthly yield / accumulated yield / accumulated balance / total wealth (same math as the Financial Flow screen).
4. **Tendencia por Categoria** — pivot months × categories (expenses; income categories excluded).
5. **Resumo Financeiro** — all-time / current year / current month totals.

## 10. Chart/PNG export
Full-screen charts can be saved to Pictures via MediaStore (`saveBitmapToFile`).