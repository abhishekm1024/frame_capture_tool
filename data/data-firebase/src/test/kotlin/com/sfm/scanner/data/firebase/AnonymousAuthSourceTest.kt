package com.sfm.scanner.data.firebase

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.sfm.scanner.core.common.Result
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AnonymousAuthSourceTest {

    private lateinit var mockAuth: FirebaseAuth
    private lateinit var source: AnonymousAuthSource

    @Before
    fun setUp() {
        mockAuth = mockk()
        source = AnonymousAuthSource(mockAuth)
    }

    @Test
    fun `signIn returns existing uid without calling signInAnonymously when currentUser is present`() = runTest {
        val mockUser = mockk<FirebaseUser>()
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns "existing-uid"

        val result = source.signIn()

        assertEquals(Result.Success("existing-uid"), result)
        verify(exactly = 0) { mockAuth.signInAnonymously() }
    }

    @Test
    fun `signIn calls signInAnonymously and returns uid on success when no current user`() = runTest {
        val mockTask = mockk<Task<AuthResult>>()
        val mockAuthResult = mockk<AuthResult>()
        val mockUser = mockk<FirebaseUser>()

        every { mockAuth.currentUser } returns null
        every { mockAuth.signInAnonymously() } returns mockTask

        val successSlot = slot<com.google.android.gms.tasks.OnSuccessListener<AuthResult>>()
        every { mockTask.addOnSuccessListener(capture(successSlot)) } answers {
            successSlot.captured.onSuccess(mockAuthResult)
            mockTask
        }
        every { mockTask.addOnFailureListener(any()) } returns mockTask

        every { mockAuthResult.user } returns mockUser
        every { mockUser.uid } returns "new-uid"

        val result = source.signIn()

        assertEquals(Result.Success("new-uid"), result)
    }

    @Test
    fun `signIn returns Failure when signInAnonymously task fails`() = runTest {
        val mockTask = mockk<Task<AuthResult>>()
        val testException = RuntimeException("Auth failed")

        every { mockAuth.currentUser } returns null
        every { mockAuth.signInAnonymously() } returns mockTask

        every { mockTask.addOnSuccessListener(any()) } returns mockTask
        val failureSlot = slot<com.google.android.gms.tasks.OnFailureListener>()
        every { mockTask.addOnFailureListener(capture(failureSlot)) } answers {
            failureSlot.captured.onFailure(testException)
            mockTask
        }

        val result = source.signIn()

        assertTrue(result is Result.Failure)
        assertEquals(testException, (result as Result.Failure).cause)
    }

    @Test
    fun `signIn returns Failure with IllegalStateException when authResult user is null`() = runTest {
        val mockTask = mockk<Task<AuthResult>>()
        val mockAuthResult = mockk<AuthResult>()

        every { mockAuth.currentUser } returns null
        every { mockAuth.signInAnonymously() } returns mockTask

        val successSlot = slot<com.google.android.gms.tasks.OnSuccessListener<AuthResult>>()
        every { mockTask.addOnSuccessListener(capture(successSlot)) } answers {
            successSlot.captured.onSuccess(mockAuthResult)
            mockTask
        }
        every { mockTask.addOnFailureListener(any()) } returns mockTask
        every { mockAuthResult.user } returns null

        val result = source.signIn()

        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).cause is IllegalStateException)
    }
}
