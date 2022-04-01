package com.igniter.ffmpegtest.data.data_source

import android.view.Surface
import com.igniter.ffmpegtest.domain.bean.CaptureFrameListener

object HardwareSolution {

    /**
     * play video in given surface
     *
     * @param surface given surface
     */
    external fun playVideo(videoPath: String, surface: Surface)

    /**
     * capture frames with ffmpeg
     */
    external fun capture(
        videoPath: String,
        totalNum: Int,
        enableMultiThread: Boolean,
        strategyIndex: Int,
        seekFlagIndex: Int,
        callback: CaptureFrameListener
    )
}