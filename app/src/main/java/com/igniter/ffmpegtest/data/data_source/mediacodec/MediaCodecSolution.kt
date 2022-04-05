package com.igniter.ffmpegtest.data.data_source.mediacodec

import android.graphics.Bitmap
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import android.util.Size
import com.igniter.ffmpegtest.data.utils.VideoUtils
import com.igniter.ffmpegtest.data.utils.msToUs
import com.igniter.ffmpegtest.data.utils.usToMs
import com.igniter.ffmpegtest.domain.bean.CaptureFrameListener
import com.igniter.ffmpegtest.domain.bean.CaptureFrameListener.Companion.STEP_RETRIEVE
import java.io.IOException
import java.nio.ByteBuffer

object MediaCodecSolution {

    private const val TAG = "MediaCodecUtils"
    private val DECODER_TIMEOUT_US = 10L.msToUs() // 从解码器申请 buffer 的超时时间

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
        val durationMs = videoFormat.getLong(MediaFormat.KEY_DURATION).usToMs()
        callback.onVideoInfoRetrieved(srcWidth, srcHeight, durationMs)
        callback.onStepPassed(0, STEP_RETRIEVE)

        val targetWidth = srcWidth / scale
        val targetHeight = srcHeight / scale

        val outputSurface = createCodecOutputSurface(
            targetSize = Size(targetWidth, targetHeight),
            rawSize = Size(srcWidth, srcHeight)
        )
        val decoder = createDecoder(videoFormat, outputSurface)
        if (decoder == null) {
            Log.e(TAG, "[captureFrames] | createDecoder is null")
            callback.onStepPassed(-1, CaptureFrameListener.STEP_FAILED)
            return
        }

        decoder.start()
        try {
            doExtract(extractor, decoder, totalNum, outputSurface, callback)
        } catch (e: IOException) {
            Log.e(TAG, "start exception, detail=${e.stackTraceToString()}")
            callback.onStepPassed(-1, CaptureFrameListener.STEP_FAILED)
            return
        } finally {
            outputSurface.release()
            decoder.apply {
                stop()
                release()
            }
            extractor.release()
        }
    }

    private fun createDecoder(videoFormat: MediaFormat, outputSurface: CodecOutputSurface): MediaCodec? {
        val mimeType = videoFormat.getString(MediaFormat.KEY_MIME)
        if (mimeType == null) {
            Log.e(TAG, "[createDecoder] | mimeType == null")
            return null
        }
        return MediaCodec.createDecoderByType(mimeType).apply {
            configure(videoFormat, outputSurface.surface, null, 0)
        }
    }

    private fun createCodecOutputSurface(
        targetSize: Size,
        rawSize: Size
    ): CodecOutputSurface {
        val finalSize = VideoUtils.getExpectedSize(target = targetSize, raw = rawSize)
        Log.i(
            TAG,
            "startExtract, video size is ${finalSize.width} x ${finalSize.height}, " +
                    "rawWidth=${rawSize.width}, rawHeight=${rawSize.height}"
        )
        return CodecOutputSurface(finalSize.width, finalSize.height)
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

    @Throws(IOException::class)
    private fun doExtract(
        extractor: MediaExtractor,
        decoder: MediaCodec,
        totalNum: Int,
        outputSurface: CodecOutputSurface,
        callback: CaptureFrameListener
    ) {
        val intervalMs = 1000L
        val bufferInfo = MediaCodec.BufferInfo()
        var decodeIndex = 0
        var outputDone = false
        var inputDone = false
        var currentExtractTimeUs: Long = (-1L).msToUs() * intervalMs
        val decoderInputBuffers = decoder.inputBuffers
        extractor.seekTo(0L.msToUs(), MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        while (!outputDone) {
            if (!inputDone) {
                inputDone = queueInputBuffer(decoder, extractor, decoderInputBuffers)
            }
            if (!outputDone) {
                val bufferIndex = decoder.dequeueOutputBuffer(bufferInfo, DECODER_TIMEOUT_US)
                // bufferIndex 大于等于0的为缓冲区数据下标
                if (bufferIndex < 0) {
                    continue
                }
                // 输出流读取完成
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    outputDone = true
                }
                val doRender = bufferInfo.size != 0
                /*
                 * 一旦我们调用了 releaseOutputBuffer，buffer 就会通过 SurfaceTexture 转化为 texture,
                 * 但是这个 api 不能保证该 texture 能够同步返回，所以我们需要等 onFrameAvailable 回调后再
                 * 获取 texture
                 */
                decoder.releaseOutputBuffer(bufferIndex, doRender)
                if (!doRender) {
                    continue
                }
                if (bufferInfo.presentationTimeUs - currentExtractTimeUs < intervalMs.msToUs()) {
                    continue
                }
                currentExtractTimeUs = bufferInfo.presentationTimeUs
                if (decodeIndex < totalNum) {
                    captureImage(outputSurface)?.also { outputBitmap ->
                        callback.onBitmapCaptured(decodeIndex, currentExtractTimeUs.usToMs(), outputBitmap)
                    }
                    decodeIndex++
                } else {
                    outputDone = true
                }
            }
        }
    }

    private fun queueInputBuffer(
        decoder: MediaCodec,
        extractor: MediaExtractor,
        decoderInputBuffers: Array<ByteBuffer>
    ): Boolean {
        val inputBufIndex = decoder.dequeueInputBuffer(DECODER_TIMEOUT_US)
        if (inputBufIndex >= 0) {
            val inputBuf = decoderInputBuffers[inputBufIndex]
            val chunkSize = extractor.readSampleData(inputBuf, 0)
            if (chunkSize < 0) {
                decoder.queueInputBuffer(
                    inputBufIndex,
                    0,
                    0,
                    0L,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
                Log.i(TAG, "input EOS")
                return true
            } else {
                val presentationTimeUs = extractor.sampleTime
                decoder.queueInputBuffer(
                    inputBufIndex,
                    0,
                    chunkSize,
                    presentationTimeUs,
                    0
                )
                extractor.advance()
            }
        } else {
            Log.e(TAG, "input buffer not available")
        }
        return false
    }

    private fun captureImage(outputSurface: CodecOutputSurface): Bitmap? {
        outputSurface.awaitNewImage()
        outputSurface.drawImage(true)
        return outputSurface.saveFrame()
    }
}