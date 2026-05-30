package com.example.gemmacontrol.data.reminder

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

object ReminderTimeParser {
    private val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mmXXX"
    )

    fun parseEpochMillis(value: String): Long? {
        val trimmed = value.trim()
        if (trimmed.isBlank()) {
            return null
        }
        return patterns.firstNotNullOfOrNull { pattern ->
            try {
                SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                }.parse(trimmed)?.time
            } catch (e: ParseException) {
                null
            }
        }
    }
}
