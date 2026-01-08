# FlowFinance

FlowFinance is a modern and powerful personal finance management app for Android.  
Designed to go beyond simple expense tracking, it provides **advanced financial analytics**, helping users deeply understand their income, spending patterns, and financial evolution over time.

The app is built using **modern Android development practices**, featuring a **100% Kotlin** codebase and a fully declarative UI powered by **Jetpack Compose**.

[Leia este README em Português](README.pt-BR.md)

---

### Main Screenshot
<p align="center">
  <img src="docs/images/main_screenshot.jpg" alt="App Screenshot" width="300"/>
</p>

---

### More Screenshots
<table>
  <tr>
    <td><img src="docs/images/screenshot1.png" alt="Screenshot 1"></td>
    <td><img src="docs/images/screenshot2.png" alt="Screenshot 2"></td>
  </tr>
  <tr>
    <td><img src="docs/images/screenshot3.png" alt="Screenshot 3"></td>
    <td><img src="docs/images/screenshot4.png" alt="Screenshot 4"></td>
  </tr>
</table>

---

## About The Project

FlowFinance was created to give users **full control and visibility over their personal finances**.  
It allows detailed tracking of income and expenses, categorization of transactions, and automatic generation of meaningful financial insights.

With the introduction of **FlowFinance v2.0.0**, the app evolves into a **complete financial analysis platform**, offering advanced dashboards, trend analysis, spending metrics, and historical summaries.

At its core, FlowFinance relies on a **fully reactive architecture**, where every change in the database is instantly reflected in charts, summaries, and balances — no manual refresh required.

---

## Getting Started: A Quick Tutorial

1. **Add Your First Transaction**  
   Tap the `+` button on the Dashboard or History screen.

2. **Fill in the Details**  
   Enter the amount, description, category, and transaction date.

3. **Explore Your Finances**  
   Instantly see your balance, charts, and analytics update across the Dashboard.

4. **Customize Your Experience**  
   Open the **Settings** tab to configure language, currency, theme, and backup options.

---

## Key Features

- **Advanced Financial Dashboard**
    - Net worth, income, and expense evolution charts.
    - Salary vs. earnings comparison.
    - Monthly and historical financial summaries.

- **Spending Analysis**
    - Daily, weekly, and monthly average spending.
    - Spending peak detection by day of week and month.
    - Weekly heatmap visualization.
    - Separation between recurring and occasional expenses.

- **Category Insights**
    - Expense distribution pie chart.
    - Category spending ranking.
    - Monthly trend evolution per category.
    - Stacked area chart for expense composition.

- **Transaction History**
    - Full chronological transaction list.
    - Sticky date headers for easy navigation.
    - Swipe-to-delete gestures.

- **Export & Backup**
    - Export all financial data to `.XLS` files.
    - Encrypted backup export and import.
    - Automated weekly backups via WorkManager.

- **Internationalization (i18n)**
    - Supports **English**, **Portuguese (Brazil)**, and **Spanish**.
    - Manual or automatic language selection (system-based).

- **User Personalization**
    - Username customization.
    - Currency selection (BRL, USD, EUR) with instant UI updates.
    - Light and Dark theme support.

- **Secure Data Management**
    - Clear data option with confirmation dialog to prevent accidental data loss.

---

## Technical Stack & Architecture

This project showcases modern Android development best practices with a clean, scalable architecture.

### Core
- **Language**: 100% [Kotlin](https://kotlinlang.org/)
- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Asynchronous Programming**: [Kotlin Coroutines & Flow](https://kotlinlang.org/docs/coroutines-guide.html)

### Architecture
- **Pattern**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Navigation**: [Jetpack Navigation for Compose](https://developer.android.com/jetpack/compose/navigation)

### Data
- **Persistence**: [Room Database](https://developer.android.com/training/data-storage/room)
- **User Preferences**: [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- **Background Tasks**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)

---

## Download

You can download the latest installable version of the app from the  
[**Releases**](https://github.com/Gabrick75/FlowFinance/releases) page.

---

## How To Build

To build and run this project, you'll need Android Studio Giraffe (2023.3.1) or newer.

1.  Clone the repository:
    ```sh
    git clone https://github.com/Gabrick75/FlowFinance.git
    ```
2.  Open the project in Android Studio.
3.  Let Gradle sync all the dependencies.
4.  Run the `app` module on an emulator or a physical device.

## Credits

Developed by **Gabrick75.**
