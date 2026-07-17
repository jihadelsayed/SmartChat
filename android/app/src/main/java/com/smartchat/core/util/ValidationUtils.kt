package com.smartchat.core.util

object ValidationUtils {
    private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun isValidEmail(value: String): Boolean = emailPattern.matches(value.trim())

    fun passwordError(value: String): String? = when {
        value.length < 8 -> "Password must be at least 8 characters."
        value.none(Char::isUpperCase) -> "Password must include an uppercase letter."
        value.none(Char::isLowerCase) -> "Password must include a lowercase letter."
        value.none(Char::isDigit) -> "Password must include a number."
        else -> null
    }
}
