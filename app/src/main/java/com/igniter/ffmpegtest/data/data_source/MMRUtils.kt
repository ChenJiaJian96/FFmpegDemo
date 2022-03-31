package com.igniter.ffmpegtest.data.data_source

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import com.igniter.ffmpegtest.domain.bean.CaptureFrameListener
import com.igniter.ffmpegtest.viewmodel.CaptureViewModel.Companion.FRAME_NUM

object MMRUtils {

    private const val TAG = "MMRUtils"

    /**
     * capture frames with [MediaMetadataRetriever]
     *
     * @Note: current function can not customize resolution of the video
     */
    fun captureFrames(
        filePath: String,
        totalNum: Int,
        callback: CaptureFrameListener) {
        MediaMetadataRetriever().use { mmr ->
            try {
                mmr.setDataSource(filePath)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "video open failed. filePath = $filePath")
//                callback.invoke(FrameResult.ReadFileFailed(ErrorCode.OPEN_FILE_FAILED))
                return
            }

            val durationMs =
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull()
            Log.d(TAG, "video duration = $durationMs ms")

            if (durationMs == null) {
//                callback.invoke(FrameResult.ReadFileFailed(ErrorCode.EXTRACT_VIDEO_DURATION_FAILED))
                return
            }

            for (index in 0 until totalNum) {
                val currentUs = index * 1000L * 1000L
                Log.d(TAG, "getFrameAtTime time = $currentUs ms")
                val frameAtIndex: Bitmap = mmr.getFrameAtTime(currentUs) ?: return
                val outBitmap = Bitmap.createScaledBitmap(
                    frameAtIndex,
                    frameAtIndex.width / 8,
                    frameAtIndex.height / 8,
                    false
                )
                frameAtIndex.recycle()
                callback.onBitmapCaptured(index, currentUs / 1000L, outBitmap)
            }

        }
    }
}