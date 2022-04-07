package com.igniter.ffmpegtest.data.repository

import android.util.Log
import com.igniter.ffmpegtest.data.data_source.FFmpegSolution
import com.igniter.ffmpegtest.data.utils.msToS
import com.igniter.ffmpegtest.domain.bean.CaptureFrameListener
import com.igniter.ffmpegtest.domain.bean.FFmpegStrategy
import com.igniter.ffmpegtest.domain.bean.RepoType
import com.igniter.ffmpegtest.domain.repository.CaptureRepository
import kotlin.concurrent.thread

class FFmpegRepoImpl(override val type: RepoType = RepoType.FFmpeg) : CaptureRepository {

    private var captureStrategy = FFmpegStrategy()

    private var threadCount = 1

    override fun captureFrames(
        videoPath: String,
        frameCount: Int,
        callback: CaptureFrameListener
    ) {
        val perThreadExecuteCount = frameCount / threadCount

        repeat(threadCount) { index ->
            val startPosInCurrentThread = perThreadExecuteCount * index
            val startTimeMs = index * perThreadExecuteCount * 1000.toLong()
            thread(
                name = """
                Thread $index:
                positionInfo: [startPos: $startPosInCurrentThread ~ endPos: ${startPosInCurrentThread + perThreadExecuteCount})
                startTimeMs = $startTimeMs
            """.trimIndent()
            ) {
                Log.e(TAG, Thread.currentThread().name)

                FFmpegSolution.capture(
                    videoPath = videoPath,
                    startTimeInS = startTimeMs.msToS().toInt(),
                    startPos = startPosInCurrentThread,
                    totalNum = perThreadExecuteCount,
                    enableMultiThread = false,
                    strategyIndex = captureStrategy.strategyIndex,
                    seekFlagIndex = captureStrategy.seekFlagIndex,
                    callback = callback
                )
            }
        }
    }

    fun updateStrategy(strategy: FFmpegStrategy) {
        captureStrategy = strategy
    }

    fun updateThreadCount(count: Int) {
        threadCount = count
    }

    companion object {
        private const val TAG = "FFmpegRepoImpl"
    }
}