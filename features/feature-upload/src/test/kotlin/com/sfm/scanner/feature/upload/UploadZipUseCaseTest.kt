package com.sfm.scanner.feature.upload

import app.cash.turbine.test
import com.sfm.scanner.core.common.AppDispatchers
import com.sfm.scanner.core.common.Result
import com.sfm.scanner.data.firebase.FirebaseRepository
import com.sfm.scanner.data.firebase.UploadProgress
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class UploadZipUseCaseTest {

    private lateinit var firebaseRepository: FirebaseRepository
    private lateinit var uploadQueueRepository: UploadQueueRepository
    private lateinit var workEnqueuer: UploadWorkEnqueuer
    private lateinit var useCase: UploadZipUseCase
    private lateinit var tempZip: File

    @Before
    fun setUp() {
        firebaseRepository = mockk()
        uploadQueueRepository = mockk(relaxUnitFun = true)
        workEnqueuer = mockk(relaxUnitFun = true)
        useCase = UploadZipUseCase(
            firebaseRepository = firebaseRepository,
            uploadQueueRepository = uploadQueueRepository,
            uploadWorkEnqueuer = workEnqueuer,
            appDispatchers = AppDispatchers(
                io = UnconfinedTestDispatcher(),
                default = UnconfinedTestDispatcher(),
                main = UnconfinedTestDispatcher(),
            ),
        )
        tempZip = File.createTempFile("upload-test-", ".zip").apply { writeBytes(byteArrayOf(1, 2, 3)) }
    }

    @After
    fun tearDown() {
        if (tempZip.exists()) tempZip.delete()
    }

    private fun artifact(): ZipArtifact = ZipArtifact(
        sessionUUID = "uuid-1",
        zipFilename = tempZip.name,
        absolutePath = tempZip.absolutePath,
        fileSizeBytes = tempZip.length(),
    )

    @Test
    fun `emits Authenticating then Uploading progress then Success when upload succeeds`() = runTest {
        coEvery { firebaseRepository.signInAnonymously() } returns Result.Success("uid-123")
        every { firebaseRepository.uploadZip(any(), any()) } returns flowOf(
            UploadProgress.Uploading(25),
            UploadProgress.Uploading(75),
            UploadProgress.Success,
        )

        useCase(artifact()).test {
            assertEquals(UploadStage.Authenticating, awaitItem())
            assertEquals(UploadStage.Uploading(25), awaitItem())
            assertEquals(UploadStage.Uploading(75), awaitItem())
            assertEquals(UploadStage.Success, awaitItem())
            awaitComplete()
        }

        assertFalse("ZIP file deleted on success", tempZip.exists())
    }

    @Test
    fun `emits RetryQueued and enqueues PendingUpload when auth fails`() = runTest {
        coEvery { firebaseRepository.signInAnonymously() } returns Result.Failure(RuntimeException("network"))

        val enqueueSlot = slot<PendingUpload>()
        coEvery { uploadQueueRepository.enqueue(capture(enqueueSlot)) } returns Unit

        useCase(artifact()).test {
            assertEquals(UploadStage.Authenticating, awaitItem())
            assertEquals(UploadStage.RetryQueued, awaitItem())
            awaitComplete()
        }

        coVerify(exactly = 1) { uploadQueueRepository.enqueue(any()) }
        verify(exactly = 1) { workEnqueuer.enqueueUploadWork(any()) }
        assertEquals("uuid-1", enqueueSlot.captured.sessionUUID)
        assertEquals(0, enqueueSlot.captured.attemptCount)
    }

    @Test
    fun `emits RetryQueued and enqueues PendingUpload when upload fails`() = runTest {
        coEvery { firebaseRepository.signInAnonymously() } returns Result.Success("uid-123")
        every { firebaseRepository.uploadZip(any(), any()) } returns flowOf(
            UploadProgress.Uploading(50),
            UploadProgress.Failure(RuntimeException("storage error")),
        )

        useCase(artifact()).test {
            assertEquals(UploadStage.Authenticating, awaitItem())
            assertEquals(UploadStage.Uploading(50), awaitItem())
            assertEquals(UploadStage.RetryQueued, awaitItem())
            awaitComplete()
        }

        coVerify(exactly = 1) { uploadQueueRepository.enqueue(any()) }
        verify(exactly = 1) { workEnqueuer.enqueueUploadWork(any()) }
        assertTrue("ZIP file preserved after failure", tempZip.exists())
    }

    @Test
    fun `emits Failed when ZIP file does not exist`() = runTest {
        val missingArtifact = ZipArtifact(
            sessionUUID = "uuid-2",
            zipFilename = "missing.zip",
            absolutePath = "/nonexistent/path/missing.zip",
            fileSizeBytes = 0L,
        )

        useCase(missingArtifact).test {
            assertEquals(UploadStage.Authenticating, awaitItem())
            val next = awaitItem()
            assertTrue("expected Failed got $next", next is UploadStage.Failed)
            awaitComplete()
        }

        coVerify(exactly = 0) { uploadQueueRepository.enqueue(any()) }
        verify(exactly = 0) { workEnqueuer.enqueueUploadWork(any()) }
    }

    @Test
    fun `emits RetryQueued when upload flow ends without terminal event`() = runTest {
        coEvery { firebaseRepository.signInAnonymously() } returns Result.Success("uid-123")
        every { firebaseRepository.uploadZip(any(), any()) } returns flowOf(
            UploadProgress.Uploading(50),
        )

        useCase(artifact()).test {
            assertEquals(UploadStage.Authenticating, awaitItem())
            assertEquals(UploadStage.Uploading(50), awaitItem())
            assertEquals(UploadStage.RetryQueued, awaitItem())
            awaitComplete()
        }

        coVerify(exactly = 1) { uploadQueueRepository.enqueue(any()) }
    }
}
