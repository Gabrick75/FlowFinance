package com.flowfinance.app.util

import java.text.NumberFormat
import java.util.Locale

fun formatCurrency(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(amount)
}
