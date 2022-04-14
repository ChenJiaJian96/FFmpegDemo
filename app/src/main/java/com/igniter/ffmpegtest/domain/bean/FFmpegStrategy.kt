package com.igniter.ffmpegtest.domain.bean

/**
 * 抽帧策略选择封装类
 */
class FFmpegStrategy {

    /**
     * 是否支持多线程抽帧
     */
    var enableMultiThread: Boolean = DEFAULT_ENABLE_MULTI_THREAD

    /**
     * 自选策略索引
     */
    var strategyIndex: Int = 0

    /**
     * seek 策略索引
     */
    var seekFlagIndex: Int = 0

    companion object {
        const val DEFAULT_ENABLE_MULTI_THREAD = false
    }
}