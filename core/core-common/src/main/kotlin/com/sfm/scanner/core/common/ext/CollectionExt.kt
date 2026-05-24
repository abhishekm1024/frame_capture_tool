package com.sfm.scanner.core.common.ext

fun <T> Collection<T>?.isNotNullOrEmpty(): Boolean = !isNullOrEmpty()

fun <T> List<T>.second(): T = get(1)

fun <T> List<T>.secondOrNull(): T? = getOrNull(1)
