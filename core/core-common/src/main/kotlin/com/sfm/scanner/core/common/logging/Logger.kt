package com.sfm.scanner.core.common.logging

import android.util.Log
import com.sfm.scanner.core.common.ext.asLogTag
import javax.inject.Inject

interface Logger {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

class AndroidLogger @Inject constructor() : Logger {
    override fun d(tag: String, message: String) {
        Log.d(tag.asLogTag(), message)
    }

    override fun i(tag: String, message: String) {
        Log.i(tag.asLogTag(), message)
    }

    override fun w(tag: String, message: String, throwable: Throwable?) {
        Log.w(tag.asLogTag(), message, throwable)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag.asLogTag(), message, throwable)
    }
}
