package com.smartchat.core.util

import java.text.DateFormat
import java.util.Date

object DateFormatter {
    fun formatDateTime(timestamp: Long): String =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))
}
