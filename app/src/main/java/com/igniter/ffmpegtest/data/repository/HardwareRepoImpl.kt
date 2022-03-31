package com.igniter.ffmpegtest.data.repository

import com.igniter.ffmpegtest.data.data_source.HardwareSolution
import com.igniter.ffmpegtest.domain.bean.CaptureFrameListener
import com.igniter.ffmpegtest.domain.bean.CaptureStrategy
import com.igniter.ffmpegtest.domain.repository.CaptureRepository

class HardwareRepoImpl: CaptureRepository {

    private var captureStrategy = CaptureStrategy()

    override fun captureFrames(videoPath: String, totalNum: Int, callback: CaptureFrameListener) {
        HardwareSolution.capture(
            videoPath = videoPath,
            totalNum = totalNum,
            enableMultiThread = captureStrategy.enableMultiThread,
            strategyIndex = captureStrategy.strategyIndex,
            seekFlagIndex = captureStrategy.seekFlagIndex,
            callback = callback
        )
    }

    fun updateCaptureStrategy(strategy: CaptureStrategy) {
        this.captureStrategy = strategy
    }
}