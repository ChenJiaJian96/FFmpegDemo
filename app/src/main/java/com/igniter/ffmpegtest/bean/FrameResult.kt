package com.igniter.ffmpegtest.bean

import android.graphics.Bitmap

/**
 * 抽帧结果
 */
sealed class FrameResult {
    class Success(val bitmap: Bitmap, timeUs: Long) : FrameResult()
    class ReadFileFailed(val errorCode: ErrorCode) : FrameResult()
    class GetFrameFailed(val timeUs: Long) : FrameResult()
}

enum class ErrorCode {
    OPEN_FILE_FAILED,
    VIDEO_TRACK_NOT_FOUND,
    EXTRACT_VIDEO_DURATION_FAILED
}