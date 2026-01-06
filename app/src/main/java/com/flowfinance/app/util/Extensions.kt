package com.flowfinance.app.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

fun formatCurrency(amount: Double, currencyCode: String = "BRL", fractionDigits: Int = 2): String {
    val locale = when (currencyCode) {
        "USD" -> Locale.US
        "EUR" -> Locale.GERMANY
        else -> Locale("pt", "BR")
    }
    val format = NumberFormat.getCurrencyInstance(locale)
    format.maximumFractionDigits = fractionDigits
    format.minimumFractionDigits = fractionDigits
    format.currency = try {
        Currency.getInstance(currencyCode)
    } catch (e: Exception) {
        Currency.getInstance("BRL")
    }
    return format.format(amount)
}

fun saveBitmapToFile(context: Context, bitmap: Bitmap, fileName: String): Boolean {
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }
    }
    
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    
    return if (uri != null) {
        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            true
        } catch (e: Exception) {
            false
        }
    } else {
        false
    }
}
