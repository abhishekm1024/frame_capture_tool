package com.sfm.scanner.feature.upload.work

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.sfm.scanner.core.common.Result as DomainResult
import com.sfm.scanner.data.firebase.FirebaseRepository
import com.sfm.scanner.data.firebase.UploadProgress
import com.sfm.scanner.feature.upload.PendingUpload
import com.sfm.scanner.feature.upload.UPLOAD_WORK_INPUT_PENDING_ID
import com.sfm.scanner.feature.upload.UploadQueueRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.io.File

class UploadWorkerTest {

    private lateinit var firebaseRepository: FirebaseRepository
    private lateinit var queue: UploadQueueRepository
    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private lateinit var tempZip: File

    @Before
    fun setUp() {
        firebaseRepository = mockk()
        queue = mockk(relaxUnitFun = true)
        context = mockk(relaxed = true)
        workerParams = mockk()
        tempZip = File.createTempFile("worker-test-", ".zip").apply { writeBytes(byteArrayOf(9)) }
    }

    @After
    fun tearDown() {
        if (tempZip.exists()) tempZip.delete()
    }

    private fun pending(id: String = "pid-1") = PendingUpload(
        id = id,
        sessionUUID = "sess-1",
        zipFilename = tempZip.name,
        absolutePath = tempZip.absolutePath,
        enqueuedAtMs = 1L,
        attemptCount = 0,
    )

    private fun worker(inputData: Data): UploadWorker {
        every { workerParams.inputData } returns inputData
        return UploadWorker(
            appContext = context,
            workerParams = workerParams,
            firebaseRepository = firebaseRepository,
            uploadQueueRepository = queue,
        )
    }

    @Test
    fun `returns failure when pending id missing from input`() = runTest {
        val result = worker(Data.EMPTY).doWork()
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `returns failure when pending record not found`() = runTest {
        coEvery { queue.getById("pid-1") } returns null
        val inputData = Data.Builder().putString(UPLOAD_WORK_INPUT_PENDING_ID, "pid-1").build()

        val result = worker(inputData).doWork()
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `returns failure and clears queue entry when ZIP file is missing`() = runTest {
        val missing = pending().copy(absolutePath = "/nonexistent/missing.zip")
        coEvery { queue.getById(missing.id) } returns missing
        val inputData = Data.Builder().putString(UPLOAD_WORK_INPUT_PENDING_ID, missing.id).build()

        val result = worker(inputData).doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
        coVerify(exactly = 1) { queue.markComplete(missing.id) }
    }

    @Test
    fun `returns retry and increments attempt when auth fails`() = runTest {
        val p = pending()
        coEvery { queue.getById(p.id) } returns p
        coEvery { firebaseRepository.signInAnonymously() } returns DomainResult.Failure(RuntimeException("net"))
        val inputData = Data.Builder().putString(UPLOAD_WORK_INPUT_PENDING_ID, p.id).build()

        val result = worker(inputData).doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
        coVerify(exactly = 1) { queue.incrementAttempt(p.id) }
    }

    @Test
    fun `returns success and clears queue entry on successful upload`() = runTest {
        val p = pending()
        coEvery { queue.getById(p.id) } returns p
        coEvery { firebaseRepository.signInAnonymously() } returns DomainResult.Success("uid-1")
        every { firebaseRepository.uploadZip(any(), any()) } returns flowOf(
            UploadProgress.Uploading(50),
            UploadProgress.Success,
        )
        val inputData = Data.Builder().putString(UPLOAD_WORK_INPUT_PENDING_ID, p.id).build()

        val result = worker(inputData).doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { queue.markComplete(p.id) }
        assertFalse("ZIP file deleted on success", tempZip.exists())
    }

    @Test
    fun `returns retry and increments attempt when upload terminal is Failure`() = runTest {
        val p = pending()
        coEvery { queue.getById(p.id) } returns p
        coEvery { firebaseRepository.signInAnonymously() } returns DomainResult.Success("uid-1")
        every { firebaseRepository.uploadZip(any(), any()) } returns flowOf(
            UploadProgress.Uploading(50),
            UploadProgress.Failure(RuntimeException("storage")),
        )
        val inputData = Data.Builder().putString(UPLOAD_WORK_INPUT_PENDING_ID, p.id).build()

        val result = worker(inputData).doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
        coVerify(exactly = 1) { queue.incrementAttempt(p.id) }
    }
}
