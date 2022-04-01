package com.igniter.ffmpegtest.data.data_source.mediacodec

import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import com.igniter.ffmpegtest.data.utils.VideoUtils.usToMs
import com.igniter.ffmpegtest.domain.bean.CaptureFrameListener
import com.igniter.ffmpegtest.domain.bean.CaptureFrameListener.Companion.STEP_RETRIEVE
import java.io.IOException

object MediaCodecSolution {

    private const val TAG = "MediaCodecUtils"

    /**
     * capture frames with [MediaCodec] && [ImageReader]
     * @param scale control the resolution of video
     *
     * @link https://www.jianshu.com/p/dfddb85302bd
     */
    fun captureFrames(videoPath: String, totalNum: Int, callback: CaptureFrameListener, scale: Int) {
        val extractor = MediaExtractor().also {
            try {
                it.setDataSource(videoPath)
            } catch (e: IOException) {
                Log.e(TAG, "[captureFrames] | video open failed. filePath = $videoPath")
                callback.onStepPassed(-1, CaptureFrameListener.STEP_FAILED)
                return
            }
        }
        // prepare video track info
        val (index, videoFormat) = getVideoTrackInfo(extractor)
        if (videoFormat == null) {
            callback.onStepPassed(-1, CaptureFrameListener.STEP_FAILED)
            return
        }
        extractor.selectTrack(index)

        val srcWidth = videoFormat.getInteger(MediaFormat.KEY_WIDTH)
        val srcHeight = videoFormat.getInteger(MediaFormat.KEY_HEIGHT)
        val durationMs = usToMs(videoFormat.getLong(MediaFormat.KEY_DURATION))
        callback.onVideoInfoRetrieved(srcWidth, srcHeight, durationMs)
        callback.onStepPassed(0, STEP_RETRIEVE)

        val dstWidth = srcWidth / scale
        val dstHeight = srcHeight / scale
        videoFormat.setInteger(MediaFormat.KEY_WIDTH, dstWidth)
        videoFormat.setInteger(MediaFormat.KEY_HEIGHT, dstHeight)

        val decoder = createDecoder(videoFormat)
        if (decoder == null) {
            Log.e(TAG, "[captureFrames] | createDecoder is null")
            callback.onStepPassed(-1, CaptureFrameListener.STEP_FAILED)
            return
        }


    }

    private fun createDecoder(videoFormat: MediaFormat): MediaCodec? {
        val mimeType = videoFormat.getString(MediaFormat.KEY_MIME)
        if (mimeType == null) {
            Log.e(TAG, "[createDecoder] | mimeType == null")
            return null
        }
        return MediaCodec.createDecoderByType(mimeType)
    }

    /**
     * 获取视频轨道信息
     */
    private fun getVideoTrackInfo(extractor: MediaExtractor): Pair<Int, MediaFormat?> {
        for (index in 0 until extractor.trackCount) {
            val trackFormat = extractor.getTrackFormat(index)
            if (trackFormat.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) {
                return Pair(index, trackFormat)
            }
        }
        return Pair(-1, null)
    }
}