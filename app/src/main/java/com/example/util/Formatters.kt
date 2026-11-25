package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

private val localeId = Locale.forLanguageTag("id-ID")
private val rupiahFormatter = NumberFormat.getNumberInstance(localeId).apply {
    maximumFractionDigits = 0
}
private val qtyFormatter = NumberFormat.getNumberInstance(localeId).apply {
    maximumFractionDigits = 2
}

fun formatRupiah(amount: Double): String {
    val prefix = if (amount < 0) "-Rp " else "Rp "
    return prefix + rupiahFormatter.format(abs(amount).roundToLong())
}

fun formatQty(qty: Double): String = qtyFormatter.format(qty)

fun formatDate(millis: Long): String {
    if (millis <= 0L) return "-"
    return SimpleDateFormat("dd MMM yyyy", localeId).format(Date(millis))
}

fun formatDateTime(millis: Long): String {
    if (millis <= 0L) return "-"
    return SimpleDateFormat("dd MMM yyyy, HH:mm", localeId).format(Date(millis))
}

fun formatMonthYear(year: Int, monthZeroBased: Int): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, monthZeroBased)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    return SimpleDateFormat("MMMM yyyy", localeId).format(cal.time)
}

fun isSameDay(millisA: Long, millisB: Long): Boolean {
    val calA = Calendar.getInstance().apply { timeInMillis = millisA }
    val calB = Calendar.getInstance().apply { timeInMillis = millisB }
    return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
        calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
}

fun isSameMonth(millis: Long, year: Int, monthZeroBased: Int): Boolean {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == monthZeroBased
}

fun isSameYear(millis: Long, year: Int): Boolean {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return cal.get(Calendar.YEAR) == year
}

fun generateInvoiceNo(prefix: String): String {
    val stamp = SimpleDateFormat("yyMMdd-HHmmss", Locale.US).format(Date())
    return "$prefix-$stamp"
}

fun formatLedgerNumber(amount: Double): String {
    val prefix = if (amount < 0) "-" else ""
    return prefix + rupiahFormatter.format(abs(amount).roundToLong())
}

fun formatDateTimeSlash(millis: Long): String {
    if (millis <= 0L) return "-"
    return SimpleDateFormat("dd/MM/yyyy HH:mm", localeId).format(Date(millis))
}

fun formatDateSlash(millis: Long): String {
    if (millis <= 0L) return "-"
    return SimpleDateFormat("dd/MM/yyyy", localeId).format(Date(millis))
}

