package com.igniter.ffmpegtest.data.utils

import android.util.Size

object VideoUtils {



    /**
     * 通过传入预期尺寸和视频原尺寸获取最终导出
     */
    fun getExpectedSize(target: Size, raw: Size): Size {
        if (target.width <= 0) {
            return raw
        }
        if (target.height <= 0) {
            return Size(target.width, raw.height * target.width / raw.width)
        }
        return Size(target.width, target.height)
    }
}

private const val TIME_UNIT = 1000

fun Long.sToMs(): Long = this * TIME_UNIT

fun Long.msToUs() = this * TIME_UNIT

fun Long.usToMs(): Long = this / TIME_UNIT