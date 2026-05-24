package com.sfm.scanner.data.firebase

import android.net.Uri
import app.cash.turbine.test
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.OnProgressListener
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class FirebaseStorageUploaderTest {

    private lateinit var mockStorage: FirebaseStorage
    private lateinit var mockRef: StorageReference
    private lateinit var mockTask: UploadTask
    private lateinit var uploader: FirebaseStorageUploader
    private lateinit var mockUri: Uri
    private val zipFile = File("/tmp/test_550e8400_1712345678.zip")

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        mockUri = mockk(relaxed = true)
        every { Uri.fromFile(any()) } returns mockUri

        mockStorage = mockk()
        mockRef = mockk()
        mockTask = mockk(relaxed = true)

        every { mockStorage.reference } returns mockk {
            every { child(any()) } returns mockRef
        }
        every { mockRef.putFile(any<Uri>()) } returns mockTask

        uploader = FirebaseStorageUploader(mockStorage)
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun `upload emits Uploading progress from progress listener`() = runTest {
        val progressSlot = slot<OnProgressListener<UploadTask.TaskSnapshot>>()
        every { mockTask.addOnProgressListener(capture(progressSlot)) } answers {
            val snapshot = mockk<UploadTask.TaskSnapshot> {
                every { bytesTransferred } returns 50L
                every { totalByteCount } returns 100L
            }
            progressSlot.captured.onProgress(snapshot)
            mockTask
        }
        val successSlot = slot<com.google.android.gms.tasks.OnSuccessListener<UploadTask.TaskSnapshot>>()
        every { mockTask.addOnSuccessListener(capture(successSlot)) } answers {
            successSlot.captured.onSuccess(mockk(relaxed = true))
            mockTask
        }
        every { mockTask.addOnFailureListener(any()) } returns mockTask

        uploader.upload("uid123", zipFile).test {
            assertEquals(UploadProgress.Uploading(50), awaitItem())
            assertEquals(UploadProgress.Success, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `upload emits Success on task completion`() = runTest {
        every { mockTask.addOnProgressListener(any<OnProgressListener<UploadTask.TaskSnapshot>>()) } returns mockTask
        val successSlot = slot<com.google.android.gms.tasks.OnSuccessListener<UploadTask.TaskSnapshot>>()
        every { mockTask.addOnSuccessListener(capture(successSlot)) } answers {
            successSlot.captured.onSuccess(mockk(relaxed = true))
            mockTask
        }
        every { mockTask.addOnFailureListener(any()) } returns mockTask

        uploader.upload("uid123", zipFile).test {
            assertEquals(UploadProgress.Success, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `upload emits Failure on task error`() = runTest {
        val testException = RuntimeException("Upload failed")
        every { mockTask.addOnProgressListener(any<OnProgressListener<UploadTask.TaskSnapshot>>()) } returns mockTask
        every { mockTask.addOnSuccessListener(any()) } returns mockTask
        val failureSlot = slot<com.google.android.gms.tasks.OnFailureListener>()
        every { mockTask.addOnFailureListener(capture(failureSlot)) } answers {
            failureSlot.captured.onFailure(testException)
            mockTask
        }

        uploader.upload("uid123", zipFile).test {
            val item = awaitItem()
            assertTrue(item is UploadProgress.Failure)
            assertEquals(testException, (item as UploadProgress.Failure).cause)
            awaitComplete()
        }
    }

    @Test
    fun `upload uses correct storage path scans_uid_filename`() = runTest {
        val refCapture = slot<String>()
        every { mockStorage.reference } returns mockk {
            every { child(capture(refCapture)) } returns mockRef
        }
        val successSlot = slot<com.google.android.gms.tasks.OnSuccessListener<UploadTask.TaskSnapshot>>()
        every { mockTask.addOnProgressListener(any<OnProgressListener<UploadTask.TaskSnapshot>>()) } returns mockTask
        every { mockTask.addOnSuccessListener(capture(successSlot)) } answers {
            successSlot.captured.onSuccess(mockk(relaxed = true))
            mockTask
        }
        every { mockTask.addOnFailureListener(any()) } returns mockTask

        uploader.upload("uid-abc", zipFile).test {
            awaitItem()
            awaitComplete()
        }

        assertEquals("scans/uid-abc/${zipFile.name}", refCapture.captured)
    }

    @Test
    fun `upload cancels UploadTask when flow collection is cancelled`() = runTest {
        every { mockTask.addOnProgressListener(any<OnProgressListener<UploadTask.TaskSnapshot>>()) } returns mockTask
        every { mockTask.addOnSuccessListener(any()) } returns mockTask
        every { mockTask.addOnFailureListener(any()) } returns mockTask

        uploader.upload("uid123", zipFile).test {
            cancel()
        }

        verify { mockTask.cancel() }
    }
}
