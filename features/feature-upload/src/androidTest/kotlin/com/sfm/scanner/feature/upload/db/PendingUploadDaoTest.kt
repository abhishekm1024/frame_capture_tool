package com.sfm.scanner.feature.upload.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit4.runners.AndroidJUnit4
import com.sfm.scanner.feature.upload.PendingUpload
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PendingUploadDaoTest {

    private lateinit var database: ScanDatabase
    private lateinit var dao: PendingUploadDao

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ScanDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.pendingUploadDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun sample(
        id: String,
        enqueuedAtMs: Long = 1_000L,
        attemptCount: Int = 0,
    ) = PendingUpload(
        id = id,
        sessionUUID = "sess-$id",
        zipFilename = "$id.zip",
        absolutePath = "/data/uploads/$id.zip",
        enqueuedAtMs = enqueuedAtMs,
        attemptCount = attemptCount,
    ).toEntity()

    @Test
    fun insert_then_getAll_returns_inserted_row() = runTest {
        dao.insert(sample("a"))
        val all = dao.getAll().first()
        assertEquals(1, all.size)
        assertEquals("a", all[0].id)
    }

    @Test
    fun getAll_returns_rows_ordered_by_enqueued_time_ascending() = runTest {
        dao.insert(sample("c", enqueuedAtMs = 3_000L))
        dao.insert(sample("a", enqueuedAtMs = 1_000L))
        dao.insert(sample("b", enqueuedAtMs = 2_000L))

        val all = dao.getAll().first()
        assertEquals(listOf("a", "b", "c"), all.map { it.id })
    }

    @Test
    fun deleteById_removes_only_matching_row() = runTest {
        dao.insert(sample("a"))
        dao.insert(sample("b"))

        dao.deleteById("a")

        val all = dao.getAll().first()
        assertEquals(1, all.size)
        assertEquals("b", all[0].id)
    }

    @Test
    fun getById_returns_inserted_row_when_present() = runTest {
        dao.insert(sample("a"))
        val row = dao.getById("a")
        assertTrue(row != null && row.id == "a")
    }

    @Test
    fun getById_returns_null_when_absent() = runTest {
        assertNull(dao.getById("missing"))
    }

    @Test
    fun incrementAttempt_increases_count_by_one() = runTest {
        dao.insert(sample("a", attemptCount = 0))

        dao.incrementAttempt("a")
        assertEquals(1, dao.getById("a")!!.attemptCount)

        dao.incrementAttempt("a")
        assertEquals(2, dao.getById("a")!!.attemptCount)
    }

    @Test
    fun insert_with_existing_id_replaces_row() = runTest {
        dao.insert(sample("a", enqueuedAtMs = 1L))
        dao.insert(sample("a", enqueuedAtMs = 2L))

        val all = dao.getAll().first()
        assertEquals(1, all.size)
        assertEquals(2L, all[0].enqueuedAtMs)
    }

    @Test
    fun full_lifecycle_enqueue_getAll_markComplete() = runTest {
        // Mimics UploadQueueRepository workflow
        val entity = sample("x")
        dao.insert(entity)
        assertEquals(1, dao.getAll().first().size)

        dao.deleteById("x")
        assertEquals(0, dao.getAll().first().size)
    }
}
