package com.igniter.ffmpeg

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
     * @param onBitmapCallbackListener 抽帧回调
     */
    external fun capture(videoPath: String, totalNum: Int, onBitmapCallbackListener: OnBitmapCallbackListener)

    //回调到各个线程
    interface OnBitmapCallbackListener {
        fun onBitmapCaptured(index: Int, timestamp: Long, bitmap: Bitmap?)
    }
}