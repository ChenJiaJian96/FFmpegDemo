package com.igniter.ffmpegtest.domain.repository

import com.igniter.ffmpegtest.domain.bean.CaptureFrameListener


interface CaptureRepository {

    /**
     * Capture Frames
     * @param videoPath source file
     * @param frameCount num of capture frames
     * @param callback return frames
     */
    fun captureFrames(
        videoPath: String,
        frameCount: Int,
        callback: CaptureFrameListener
    )

}