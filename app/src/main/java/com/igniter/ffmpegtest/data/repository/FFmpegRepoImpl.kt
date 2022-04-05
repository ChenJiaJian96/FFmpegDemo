package com.igniter.ffmpegtest.data.repository

import com.igniter.ffmpegtest.data.data_source.FFmpegSolution
import com.igniter.ffmpegtest.data.utils.MsToS
import com.igniter.ffmpegtest.domain.bean.CaptureFrameListener
import com.igniter.ffmpegtest.domain.bean.CaptureStrategy
import com.igniter.ffmpegtest.domain.repository.CaptureRepository

class FFmpegRepoImpl: CaptureRepository {

    private var captureStrategy = CaptureStrategy()

    override fun captureFrames(
        videoPath: String,
        totalNum: Int,
        callback: CaptureFrameListener,
        startPos: Int,
        startTimeInMs: Long
    ) {
        FFmpegSolution.capture(
            videoPath = videoPath,
            startTimeInS = startTimeInMs.MsToS().toInt(),
            startPos = startPos,
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