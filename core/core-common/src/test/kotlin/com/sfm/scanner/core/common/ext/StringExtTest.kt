package com.sfm.scanner.core.common.ext

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StringExtTest {

    @Test
    fun `asLogTag returns string unchanged when within 23 chars`() {
        val tag = "MyViewModel"
        assertEquals(tag, tag.asLogTag())
    }

    @Test
    fun `asLogTag truncates to 23 chars when longer`() {
        val tag = "ThisIsAVeryLongTagThatExceedsAndroidLimit"
        val result = tag.asLogTag()
        assertEquals(23, result.length)
        assertEquals(tag.substring(0, 23), result)
    }

    @Test
    fun `asLogTag returns string exactly 23 chars unchanged`() {
        val tag = "ExactlyTwentyThreeChars"
        assertEquals(23, tag.length)
        assertEquals(tag, tag.asLogTag())
    }

    @Test
    fun `isAlphanumeric returns true for alphanumeric string`() {
        assertTrue("ABC123".isAlphanumeric())
        assertTrue("hello".isAlphanumeric())
        assertTrue("12345".isAlphanumeric())
    }

    @Test
    fun `isAlphanumeric returns false for string with special chars`() {
        assertFalse("hello world".isAlphanumeric())
        assertFalse("test!".isAlphanumeric())
        assertFalse("".isAlphanumeric())
        assertFalse("foo-bar".isAlphanumeric())
    }

    @Test
    fun `truncate returns unchanged string within maxLength`() {
        assertEquals("hello", "hello".truncate(10))
        assertEquals("hello", "hello".truncate(5))
    }

    @Test
    fun `truncate cuts string at maxLength`() {
        assertEquals("hel", "hello".truncate(3))
    }
}
