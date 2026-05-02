package com.vibecoding.auth.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val ISO_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'"

fun nowUtcMillis(): Long = System.currentTimeMillis()

fun toIso8601Utc(millis: Long): String {
    val formatter = SimpleDateFormat(ISO_PATTERN, Locale.US)
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(Date(millis))
}

fun parseIso8601Utc(value: String): Long? {
    return runCatching {
        val formatter = SimpleDateFormat(ISO_PATTERN, Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        formatter.parse(value)?.time
    }.getOrNull()
}

