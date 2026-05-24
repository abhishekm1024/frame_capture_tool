package com.sfm.scanner.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultTest {

    // --- Construction ---

    @Test
    fun `Success holds data`() {
        val result = Result.Success("hello")
        assertEquals("hello", result.data)
    }

    @Test
    fun `Failure holds throwable`() {
        val ex = RuntimeException("boom")
        val result = Result.Failure(ex)
        assertSame(ex, result.cause)
    }

    @Test
    fun `Loading is singleton`() {
        assertSame(Result.Loading, Result.Loading)
    }

    // --- isSuccess / isFailure / isLoading ---

    @Test
    fun `isSuccess true for Success`() {
        assertTrue(Result.Success(1).isSuccess())
    }

    @Test
    fun `isSuccess false for Failure`() {
        assertFalse(Result.Failure(RuntimeException()).isSuccess())
    }

    @Test
    fun `isSuccess false for Loading`() {
        assertFalse(Result.Loading.isSuccess())
    }

    @Test
    fun `isFailure true for Failure`() {
        assertTrue(Result.Failure(RuntimeException()).isFailure())
    }

    @Test
    fun `isFailure false for Success`() {
        assertFalse(Result.Success(1).isFailure())
    }

    @Test
    fun `isLoading true for Loading`() {
        assertTrue(Result.Loading.isLoading())
    }

    @Test
    fun `isLoading false for Success`() {
        assertFalse(Result.Success(1).isLoading())
    }

    // --- map ---

    @Test
    fun `map transforms Success data`() {
        val result = Result.Success(2).map { it * 3 }
        assertEquals(Result.Success(6), result)
    }

    @Test
    fun `map passes Failure through unchanged`() {
        val ex = RuntimeException("err")
        val result: Result<Int> = Result.Failure(ex)
        val mapped = result.map { it * 2 }
        assertTrue(mapped is Result.Failure)
        assertSame(ex, (mapped as Result.Failure).cause)
    }

    @Test
    fun `map passes Loading through unchanged`() {
        val result: Result<Int> = Result.Loading
        val mapped = result.map { it * 2 }
        assertSame(Result.Loading, mapped)
    }

    // --- getOrNull ---

    @Test
    fun `getOrNull returns data for Success`() {
        assertEquals("value", Result.Success("value").getOrNull())
    }

    @Test
    fun `getOrNull returns null for Failure`() {
        assertNull(Result.Failure(RuntimeException()).getOrNull())
    }

    @Test
    fun `getOrNull returns null for Loading`() {
        assertNull(Result.Loading.getOrNull())
    }

    // --- getOrThrow ---

    @Test
    fun `getOrThrow returns data for Success`() {
        assertEquals(42, Result.Success(42).getOrThrow())
    }

    @Test(expected = IllegalStateException::class)
    fun `getOrThrow throws IllegalStateException for Loading`() {
        Result.Loading.getOrThrow()
    }

    @Test
    fun `getOrThrow rethrows cause for Failure`() {
        val ex = IllegalArgumentException("bad arg")
        try {
            Result.Failure(ex).getOrThrow()
        } catch (thrown: IllegalArgumentException) {
            assertSame(ex, thrown)
            return
        }
        throw AssertionError("Expected exception was not thrown")
    }

    // --- onSuccess / onFailure ---

    @Test
    fun `onSuccess callback invoked for Success`() {
        var called = false
        Result.Success("x").onSuccess { called = true }
        assertTrue(called)
    }

    @Test
    fun `onSuccess callback not invoked for Failure`() {
        var called = false
        Result.Failure(RuntimeException()).onSuccess { called = true }
        assertFalse(called)
    }

    @Test
    fun `onSuccess returns same result instance`() {
        val result = Result.Success("abc")
        val returned = result.onSuccess { }
        assertSame(result, returned)
    }

    @Test
    fun `onFailure callback invoked for Failure`() {
        val ex = RuntimeException("fail")
        var captured: Throwable? = null
        Result.Failure(ex).onFailure { captured = it }
        assertSame(ex, captured)
    }

    @Test
    fun `onFailure callback not invoked for Success`() {
        var called = false
        Result.Success(1).onFailure { called = true }
        assertFalse(called)
    }

    // --- fold ---

    @Test
    fun `fold invokes onSuccess for Success`() {
        val out = Result.Success(5).fold(
            onSuccess = { it * 2 },
            onFailure = { -1 },
            onLoading = { 0 },
        )
        assertEquals(10, out)
    }

    @Test
    fun `fold invokes onFailure for Failure`() {
        val out = Result.Failure(RuntimeException()).fold(
            onSuccess = { 1 },
            onFailure = { -1 },
            onLoading = { 0 },
        )
        assertEquals(-1, out)
    }

    @Test
    fun `fold invokes onLoading for Loading`() {
        val out = Result.Loading.fold(
            onSuccess = { 1 },
            onFailure = { -1 },
            onLoading = { 0 },
        )
        assertEquals(0, out)
    }
}
