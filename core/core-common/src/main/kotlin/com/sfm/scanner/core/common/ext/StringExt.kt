package com.sfm.scanner.core.common.ext

private const val LOG_TAG_MAX_LENGTH = 23

fun String.asLogTag(): String =
    if (length <= LOG_TAG_MAX_LENGTH) this else substring(0, LOG_TAG_MAX_LENGTH)

fun String.isAlphanumeric(): Boolean = matches(Regex("[A-Za-z0-9]+"))

fun String.truncate(maxLength: Int): String =
    if (length <= maxLength) this else substring(0, maxLength)
