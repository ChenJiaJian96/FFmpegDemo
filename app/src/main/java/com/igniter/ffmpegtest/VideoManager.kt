package com.igniter.ffmpegtest

import android.graphics.Bitmap
import android.view.Surface

class VideoManager {
    companion object {
        init {
            System.loadLibrary("video")
        }
    }

    /**
     * 播放视频
     * @param path 视频路径
     * @param surface 播放位置
     */
    external fun playVideo(path: String?, surface: Surface?)

    /**
     * 抽帧接口
     * @param videoPath 视频路径
     * @param totalNum 需要抽帧的数量
     * @param enableMultiThread 是否开启多线程
     * @param strategyIndex 策略选择
     * @param onBitmapCallbackListener 抽帧回调
     */
    external fun capture(
        videoPath: String,
        totalNum: Int,
        enableMultiThread: Boolean,
        strategyIndex: Int,
        seekFlagIndex: Int,
        onBitmapCallbackListener: OnCaptureDataCallbackListener
    )

    interface OnCaptureDataCallbackListener {
        fun onVideoInfoRetrieved(width: Int, height: Int, durationMs: Long)

        fun onBitmapCaptured(index: Int, timestamp: Long, bitmap: Bitmap, isFinished: Boolean)

        fun onStepPassed(index: Int, step: Int)

        companion object {
            const val STEP_RETRIEVE = 0
            const val STEP_SEEK_AND_DECODE = 1
            const val STEP_OUTPUT = 2
        }
    }
}