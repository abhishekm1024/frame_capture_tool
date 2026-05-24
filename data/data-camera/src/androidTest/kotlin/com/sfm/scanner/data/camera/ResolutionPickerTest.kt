package com.sfm.scanner.data.camera

import android.util.Size
import androidx.test.ext.junit4.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResolutionPickerTest {

    private lateinit var picker: ResolutionPicker

    @Before
    fun setUp() {
        picker = ResolutionPicker()
    }

    @Test
    fun selectFrom_returns_preferred_2560x1440_when_present() {
        val sizes = listOf(Size(2560, 1440), Size(1920, 1080), Size(1280, 720))
        assertEquals(Size(2560, 1440), picker.selectFrom(sizes))
    }

    @Test
    fun selectFrom_falls_back_to_1920x1080_when_preferred_absent() {
        val sizes = listOf(Size(1920, 1080), Size(1280, 720), Size(640, 480))
        assertEquals(Size(1920, 1080), picker.selectFrom(sizes))
    }

    @Test
    fun selectFrom_falls_back_to_1280x720_when_both_preferred_absent() {
        val sizes = listOf(Size(1280, 720), Size(640, 480))
        assertEquals(Size(1280, 720), picker.selectFrom(sizes))
    }

    @Test
    fun selectFrom_returns_largest_when_no_preferred_size_matches() {
        val sizes = listOf(Size(640, 480), Size(1024, 768), Size(800, 600))
        val result = picker.selectFrom(sizes)
        assertNotNull(result)
        assertEquals(Size(1024, 768), result)
    }

    @Test
    fun selectFrom_returns_null_for_empty_list() {
        assertNull(picker.selectFrom(emptyList()))
    }

    @Test
    fun buildCameraXSelector_returns_non_null_selector() {
        assertNotNull(picker.buildCameraXSelector())
    }
}
