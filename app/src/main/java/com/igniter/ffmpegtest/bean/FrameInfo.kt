package com.igniter.ffmpegtest.bean

import android.graphics.Bitmap

/**
 * 抽帧结果数据类
 */
data class FrameInfo(
    val index: Int,
    val timestamp: Long,
    val bitmap: Bitmap
)