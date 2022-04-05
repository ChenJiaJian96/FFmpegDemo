package com.igniter.ffmpegtest.data.repository

import com.igniter.ffmpegtest.data.data_source.MMRSolution
import com.igniter.ffmpegtest.domain.bean.CaptureFrameListener
import com.igniter.ffmpegtest.domain.repository.CaptureRepository

class MMRRepoImpl : CaptureRepository {

    private var scale: Int = 1

    override fun captureFrames(
        videoPath: String,
        totalNum: Int,
        callback: CaptureFrameListener,
        startPos: Int,
        startTimeInMs: Long
    ) {
        MMRSolution.captureFrames(videoPath, totalNum, callback, scale)
    }

    fun updateScale(scale: Int) {
        this.scale = scale
    }
}