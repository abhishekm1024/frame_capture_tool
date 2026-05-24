package com.sfm.scanner.data.firebase

import com.sfm.scanner.core.common.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

class FirebaseRepositoryImplTest {

    private lateinit var mockAuthSource: AnonymousAuthSource
    private lateinit var mockUploader: FirebaseStorageUploader
    private lateinit var repository: FirebaseRepositoryImpl

    @Before
    fun setUp() {
        mockAuthSource = mockk()
        mockUploader = mockk()
        repository = FirebaseRepositoryImpl(mockAuthSource, mockUploader)
    }

    @Test
    fun `signInAnonymously delegates to AnonymousAuthSource`() = runTest {
        coEvery { mockAuthSource.signIn() } returns Result.Success("delegated-uid")

        val result = repository.signInAnonymously()

        assertEquals(Result.Success("delegated-uid"), result)
        coVerify(exactly = 1) { mockAuthSource.signIn() }
    }

    @Test
    fun `uploadZip delegates to FirebaseStorageUploader`() {
        val zipFile = File("/tmp/test.zip")
        val expectedFlow = flowOf(UploadProgress.Success)
        every { mockUploader.upload("uid123", zipFile) } returns expectedFlow

        val result = repository.uploadZip("uid123", zipFile)

        assertEquals(expectedFlow, result)
        verify(exactly = 1) { mockUploader.upload("uid123", zipFile) }
    }
}
