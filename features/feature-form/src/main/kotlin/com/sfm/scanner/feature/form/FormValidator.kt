package com.sfm.scanner.feature.form

internal object FormValidator {

    private const val DETAIL_MAX_LENGTH = 16
    private val DETAIL_PATTERN = Regex("[A-Za-z0-9]+")

    private const val ERROR_SIZE = "Must be a positive integer"
    private const val ERROR_DETAIL = "Alphanumeric only, max 16 characters"
    private const val ERROR_GT = "Enter comma-separated numbers (e.g. 1.0, 2.5)"

    fun validateSize(input: String): String? {
        if (input.isEmpty()) return ERROR_SIZE
        val parsed = input.toIntOrNull() ?: return ERROR_SIZE
        return if (parsed > 0) null else ERROR_SIZE
    }

    fun validateDetail(input: String): String? {
        if (input.isEmpty()) return ERROR_DETAIL
        if (input.length > DETAIL_MAX_LENGTH) return ERROR_DETAIL
        if (!DETAIL_PATTERN.matches(input)) return ERROR_DETAIL
        return null
    }

    fun validateGt(input: String): String? {
        if (input.isBlank()) return null
        val tokens = input.split(",").map { it.trim() }
        return if (tokens.all { it.toDoubleOrNull() != null }) null else ERROR_GT
    }

    fun parseGt(input: String): List<Double>? {
        if (input.isBlank()) return null
        return input.split(",").map { it.trim().toDouble() }
    }

    fun isSizeValid(input: String) = validateSize(input) == null
    fun isDetailValid(input: String) = validateDetail(input) == null
    fun isGtValid(input: String) = validateGt(input) == null
}
