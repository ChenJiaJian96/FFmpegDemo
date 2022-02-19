package com.igniter.ffmpeg

import android.graphics.Bitmap

data class FrameInfo(
    val index: Int,
    val timestamp: Long,
    val bitmap: Bitmap,
    val timeCost: Long
)