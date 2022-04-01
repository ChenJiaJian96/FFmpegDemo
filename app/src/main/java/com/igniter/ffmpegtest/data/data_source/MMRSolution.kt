package com.igniter.ffmpegtest.data.data_source

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Log
import android.util.Size
import com.igniter.ffmpegtest.data.utils.VideoUtils
import com.igniter.ffmpegtest.domain.bean.CaptureFrameListener

object MMRSolution {

    private const val TAG = "MMRUtils"

    // Strategy selection:
    // if internal of key frames is greater than what we need,
    // we can use [OPTION_CLOSEST_SYNC] to seek key frames only, and it's much more faster.
    private const val CAPTURE_OPTION = MediaMetadataRetriever.OPTION_CLOSEST

    /**
     * capture frames with [MediaMetadataRetriever]
     * @param scale control the resolution of video
     *
     * @Note: current function can only customize video resolution up to [Build.VERSION_CODES.O_MR1]
     */
    fun captureFrames(
        filePath: String,
        totalNum: Int,
        callback: CaptureFrameListener,
        scale: Int
    ) {
        MediaMetadataRetriever().use { mmr ->
            try {
                mmr.setDataSource(filePath)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "[captureFrames] | video open failed. filePath = $filePath")
                return
            }

            val rawWidth = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val rawHeight = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val durationMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            callback.onVideoInfoRetrieved(rawWidth, rawHeight, durationMs)

            val outputSize = VideoUtils.getExpectedSize(
                target = Size(rawWidth / scale, rawHeight / scale),
                raw = Size(rawWidth, rawHeight)
            )
            Log.d(TAG, "[captureFrames] | outputSize : width = ${outputSize.width}, height: ${outputSize.height}")

            for (index in 0 until totalNum) {
                val positionMs = index * 1000L

                doExtractor(mmr, outputSize, positionMs)?.run {
                    callback.onBitmapCaptured(index, positionMs, this)
                }
            }
        }
    }

    private fun doExtractor(mmr: MediaMetadataRetriever, videoSize: Size, positionMs: Long): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                Log.e(TAG, "[doExtractor] | getScaledFrameAtTime time = $positionMs ms")
                mmr.getScaledFrameAtTime(
                    positionMs * 1000,
                    CAPTURE_OPTION,
                    videoSize.width,
                    videoSize.height
                )
            } else {
                Log.e(TAG, "[doExtractor] | getFrameAtTime time = $positionMs ms")
                mmr.getFrameAtTime(positionMs * 1000, CAPTURE_OPTION)
            }
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}