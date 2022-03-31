package com.igniter.ffmpegtest.data.repository

import com.igniter.ffmpegtest.data.data_source.MMRUtils
import com.igniter.ffmpegtest.domain.bean.CaptureFrameListener
import com.igniter.ffmpegtest.domain.repository.CaptureRepository

class MMRRepoImpl : CaptureRepository {

    override fun captureFrames(videoPath: String, totalNum: Int, callback: CaptureFrameListener) {
        MMRUtils.captureFrames(videoPath, totalNum, callback)
    }
}