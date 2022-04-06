package com.igniter.ffmpegtest.domain.bean

/**
 * 抽帧策略选择封装类
 */
class FFmpegStrategy {

    /**
     * 是否支持多线程抽帧
     */
    var enableMultiThread: Boolean = false

    /**
     * 自选策略索引
     */
    var strategyIndex: Int = -1

    /**
     * seek 策略索引
     */
    var seekFlagIndex: Int = -1

}