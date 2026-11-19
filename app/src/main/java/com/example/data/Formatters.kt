package com.example.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    private const val KB = 1024.0
    private const val MB = 1024.0 * 1024.0
    private const val GB = 1024.0 * 1024.0 * 1024.0
    private const val TB = 1024.0 * 1024.0 * 1024.0 * 1024.0

    const val BYTES_PER_GB: Double = 1024.0 * 1024.0 * 1024.0

    fun formatBytes(bytes: Long): String {
        val safe = bytes.coerceAtLeast(0L).toDouble()
        return when {
            safe >= TB -> String.format(Locale.US, "%.2f TB", safe / TB)
            safe >= GB -> String.format(Locale.US, "%.2f GB", safe / GB)
            safe >= MB -> String.format(Locale.US, "%.1f MB", safe / MB)
            safe >= KB -> String.format(Locale.US, "%.0f KB", safe / KB)
            else -> "0.0 MB"
        }
    }

    fun formatSpeed(bytesPerSecond: Double): String {
        val safe = bytesPerSecond.coerceAtLeast(0.0)
        return when {
            safe >= MB -> String.format(Locale.US, "%.2f MB/s", safe / MB)
            safe >= KB -> String.format(Locale.US, "%.1f KB/s", safe / KB)
            safe > 0.0 -> String.format(Locale.US, "%.0f B/s", safe)
            else -> "0.0 KB/s"
        }
    }

    fun todayDateKey(nowMillis: Long = System.currentTimeMillis()): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(Date(nowMillis))
    }

    fun formatTrackingDate(epochMillis: Long): String {
        if (epochMillis <= 0L) return "—"
        val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
        return formatter.format(Date(epochMillis))
    }

    fun toBengaliDigits(number: Int): String {
        val bengali = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        return number.toString().map { ch ->
            if (ch in '0'..'9') bengali[ch - '0'] else ch
        }.joinToString("")
    }
}
