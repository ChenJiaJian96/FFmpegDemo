package com.igniter.ffmpegtest.data.data_source

import android.view.Surface
import com.igniter.ffmpegtest.domain.bean.CaptureFrameListener

object HardwareSolution {

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
        onBitmapCallbackListener: CaptureFrameListener
    )
}