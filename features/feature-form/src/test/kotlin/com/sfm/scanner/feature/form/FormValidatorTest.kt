package com.sfm.scanner.feature.form

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FormValidatorTest {

    // ── validateSize ────────────────────────────────────────────────────────

    @Test
    fun `validateSize returns error for empty input`() {
        assertNotNull(FormValidator.validateSize(""))
    }

    @Test
    fun `validateSize returns error for zero`() {
        assertNotNull(FormValidator.validateSize("0"))
    }

    @Test
    fun `validateSize returns error for negative integer`() {
        assertNotNull(FormValidator.validateSize("-1"))
    }

    @Test
    fun `validateSize returns error for non-integer text`() {
        assertNotNull(FormValidator.validateSize("abc"))
    }

    @Test
    fun `validateSize returns error for decimal`() {
        assertNotNull(FormValidator.validateSize("1.5"))
    }

    @Test
    fun `validateSize returns null for positive integer 1`() {
        assertNull(FormValidator.validateSize("1"))
    }

    @Test
    fun `validateSize returns null for large positive integer`() {
        assertNull(FormValidator.validateSize("100"))
    }

    @Test
    fun `validateSize returns null for Int MAX_VALUE`() {
        assertNull(FormValidator.validateSize(Int.MAX_VALUE.toString()))
    }

    @Test
    fun `validateSize returns error for value exceeding Int range`() {
        assertNotNull(FormValidator.validateSize("2147483648"))
    }

    // ── validateDetail ──────────────────────────────────────────────────────

    @Test
    fun `validateDetail returns error for empty input`() {
        assertNotNull(FormValidator.validateDetail(""))
    }

    @Test
    fun `validateDetail returns null for purely alphanumeric input`() {
        assertNull(FormValidator.validateDetail("abc123"))
    }

    @Test
    fun `validateDetail returns null for uppercase alphanumeric`() {
        assertNull(FormValidator.validateDetail("ABC123"))
    }

    @Test
    fun `validateDetail returns error for input with special character`() {
        assertNotNull(FormValidator.validateDetail("abc!"))
    }

    @Test
    fun `validateDetail returns error for input with space`() {
        assertNotNull(FormValidator.validateDetail("abc 1"))
    }

    @Test
    fun `validateDetail returns null for exactly 16 characters`() {
        assertNull(FormValidator.validateDetail("a".repeat(16)))
    }

    @Test
    fun `validateDetail returns error for 17 characters`() {
        assertNotNull(FormValidator.validateDetail("a".repeat(17)))
    }

    // ── validateGt ──────────────────────────────────────────────────────────

    @Test
    fun `validateGt returns null for empty input`() {
        assertNull(FormValidator.validateGt(""))
    }

    @Test
    fun `validateGt returns null for blank input`() {
        assertNull(FormValidator.validateGt("   "))
    }

    @Test
    fun `validateGt returns null for single valid number`() {
        assertNull(FormValidator.validateGt("1.0"))
    }

    @Test
    fun `validateGt returns null for comma-separated valid numbers`() {
        assertNull(FormValidator.validateGt("1.0, 2.5, 3.0"))
    }

    @Test
    fun `validateGt returns null for numbers without spaces`() {
        assertNull(FormValidator.validateGt("1.0,2.5"))
    }

    @Test
    fun `validateGt returns error for non-numeric input`() {
        assertNotNull(FormValidator.validateGt("abc"))
    }

    @Test
    fun `validateGt returns error when any token is non-numeric`() {
        assertNotNull(FormValidator.validateGt("1.0, abc"))
    }

    @Test
    fun `validateGt returns error for trailing comma producing empty token`() {
        assertNotNull(FormValidator.validateGt("1.0,"))
    }

    // ── parseGt ─────────────────────────────────────────────────────────────

    @Test
    fun `parseGt returns null for blank input`() {
        assertNull(FormValidator.parseGt(""))
    }

    @Test
    fun `parseGt returns parsed list for valid input`() {
        val result = FormValidator.parseGt("1.0, 2.5")
        assertEquals(listOf(1.0, 2.5), result)
    }

    @Test
    fun `parseGt trims whitespace around tokens`() {
        val result = FormValidator.parseGt("  1.0  ,  2.5  ")
        assertEquals(listOf(1.0, 2.5), result)
    }
}
