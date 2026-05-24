package com.sfm.scanner.core.common

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Failure(val cause: Throwable) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Failure -> this
    is Result.Loading -> this
}

fun <T> Result<T>.getOrNull(): T? = when (this) {
    is Result.Success -> data
    else -> null
}

fun <T> Result<T>.getOrThrow(): T = when (this) {
    is Result.Success -> data
    is Result.Failure -> throw cause
    is Result.Loading -> throw IllegalStateException("Result is in Loading state")
}

fun <T> Result<T>.isSuccess(): Boolean = this is Result.Success
fun <T> Result<T>.isFailure(): Boolean = this is Result.Failure
fun <T> Result<T>.isLoading(): Boolean = this is Result.Loading

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onFailure(action: (Throwable) -> Unit): Result<T> {
    if (this is Result.Failure) action(cause)
    return this
}

inline fun <T, R> Result<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (Throwable) -> R,
    onLoading: () -> R,
): R = when (this) {
    is Result.Success -> onSuccess(data)
    is Result.Failure -> onFailure(cause)
    is Result.Loading -> onLoading()
}
