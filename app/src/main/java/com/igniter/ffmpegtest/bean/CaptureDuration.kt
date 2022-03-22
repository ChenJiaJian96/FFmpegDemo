package com.igniter.ffmpegtest.bean

/**
 * 记录逐帧耗时结构体
 */
data class CaptureDuration(
    var index: Int = -1,
    var retrieveMs: Long = 0,
    var seekAndDecodeMs: Long = 0,
    var outputMs: Long = 0
)