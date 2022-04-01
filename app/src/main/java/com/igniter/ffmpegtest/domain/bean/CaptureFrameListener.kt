package com.igniter.ffmpegtest.domain.bean

import android.graphics.Bitmap

interface CaptureFrameListener {
    fun onVideoInfoRetrieved(width: Int, height: Int, durationMs: Long)

    fun onBitmapCaptured(index: Int, timestampMs: Long, bitmap: Bitmap)

    fun onStepPassed(index: Int, step: Int)

    companion object {
        const val STEP_FAILED = -1
        const val STEP_RETRIEVE = 0
        const val STEP_SEEK_AND_DECODE = 1
        const val STEP_OUTPUT = 2
    }
}