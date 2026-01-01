package com.flowfinance.app.util

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

fun formatCurrency(amount: Double, currencyCode: String = "BRL"): String {
    val locale = when (currencyCode) {
        "USD" -> Locale.US
        "EUR" -> Locale.GERMANY
        else -> Locale("pt", "BR")
    }
    val format = NumberFormat.getCurrencyInstance(locale)
    format.currency = try {
        Currency.getInstance(currencyCode)
    } catch (e: Exception) {
        Currency.getInstance("BRL")
    }
    return format.format(amount)
}
