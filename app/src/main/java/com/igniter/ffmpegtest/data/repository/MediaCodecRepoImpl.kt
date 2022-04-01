package com.igniter.ffmpegtest.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.igniter.ffmpegtest.data.data_source.MediaCodecSolution
import com.igniter.ffmpegtest.domain.bean.CaptureFrameListener
import com.igniter.ffmpegtest.domain.repository.CaptureRepository

class MediaCodecRepoImpl: CaptureRepository {

    private var scale: Int = 1

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun captureFrames(videoPath: String, totalNum: Int, callback: CaptureFrameListener) {
        MediaCodecSolution.captureFrames(videoPath, totalNum, callback, scale)
    }

    fun updateScale(scale: Int) {
        this.scale = scale
    }
}