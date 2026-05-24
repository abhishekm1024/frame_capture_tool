package com.sfm.scanner.core.common

import com.sfm.scanner.core.common.di.AppDispatchersModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AppDispatchersModuleTest {

    @Test
    fun `provideAppDispatchers returns non-null dispatchers`() {
        val dispatchers = AppDispatchersModule.provideAppDispatchers()
        assertNotNull(dispatchers.io)
        assertNotNull(dispatchers.default)
        assertNotNull(dispatchers.main)
    }

    @Test
    fun `provideAppDispatchers io binds to Dispatchers IO`() {
        val dispatchers = AppDispatchersModule.provideAppDispatchers()
        assertEquals(Dispatchers.IO, dispatchers.io)
    }

    @Test
    fun `provideAppDispatchers default binds to Dispatchers Default`() {
        val dispatchers = AppDispatchersModule.provideAppDispatchers()
        assertEquals(Dispatchers.Default, dispatchers.default)
    }

    @Test
    fun `AppDispatchers supports test dispatcher substitution for io`() {
        val testDispatcher = UnconfinedTestDispatcher()
        val dispatchers = AppDispatchers(io = testDispatcher)
        assertEquals(testDispatcher, dispatchers.io)
    }

    @Test
    fun `AppDispatchers supports test dispatcher substitution for default`() {
        val testDispatcher = UnconfinedTestDispatcher()
        val dispatchers = AppDispatchers(default = testDispatcher)
        assertEquals(testDispatcher, dispatchers.default)
    }

    @Test
    fun `AppDispatchers supports test dispatcher substitution for main`() {
        val testDispatcher = UnconfinedTestDispatcher()
        val dispatchers = AppDispatchers(main = testDispatcher)
        assertEquals(testDispatcher, dispatchers.main)
    }

    @Test
    fun `AppDispatchers equality is data class structural equality`() {
        val testDispatcher = UnconfinedTestDispatcher()
        val a = AppDispatchers(io = testDispatcher, default = testDispatcher, main = testDispatcher)
        val b = AppDispatchers(io = testDispatcher, default = testDispatcher, main = testDispatcher)
        assertEquals(a, b)
    }
}
