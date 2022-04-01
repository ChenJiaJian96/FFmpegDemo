package com.igniter.ffmpegtest.data.data_source

import android.graphics.ImageFormat
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import androidx.annotation.RequiresApi
import com.igniter.ffmpegtest.domain.bean.CaptureFrameListener

object MediaCodecSolution {

    private const val TAG = "MediaCodecUtils"

    /**
     * capture frames with [MediaCodec] && [ImageReader]
     * @param scale control the resolution of video
     *
     * @link https://www.jianshu.com/p/dfddb85302bd
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun captureFrames(videoPath: String, totalNum: Int, callback: CaptureFrameListener, scale: Int) {
        // prepare file info
        val extractor = MediaExtractor().also {
            it.setDataSource(videoPath)
        }
        // prepare video track info
        val (index, videoFormat) = getVideoTrackInfo(extractor)
        if (videoFormat == null) {
//            callback.invoke(FrameResult.ReadFileFailed(ErrorCode.VIDEO_TRACK_NOT_FOUND))
            return
        }
        extractor.selectTrack(index)
        //
        val colorFormat = COLOR_FormatYUV420Flexible
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
        val srcWidth = videoFormat.getInteger(MediaFormat.KEY_WIDTH)
        val srcHeight = videoFormat.getInteger(MediaFormat.KEY_HEIGHT)
        val dstWidth = srcWidth / scale
        val dstHeight = srcHeight / scale
        videoFormat.setInteger(MediaFormat.KEY_WIDTH, dstWidth)
        videoFormat.setInteger(MediaFormat.KEY_HEIGHT, dstHeight)
        val duration = videoFormat.getLong(MediaFormat.KEY_DURATION)
        val codec = MediaCodec.createDecoderByType(videoFormat.getString(MediaFormat.KEY_MIME, ""))
        val imageReader = ImageReader.newInstance(
            srcWidth,
            srcHeight,
            ImageFormat.YUV_420_888,
            3
        )
        // TODO(jiajianchen) implement it
//        val readerHandlerThread = ImageReaderHandlerThread()
//        imageReader.setOnImageAvailableListener(
//            CaptureImageAvailableListener(callback,scale),
//            readerHandlerThread.handler
//        )
//        codec.configure(videoFormat, imageReader.surface, null, 0)
//        codec.start()
//        val bufferInfo = MediaCodec.BufferInfo()
//        val timeOut = (5 * 1000).toLong() //10ms
//
//        var inputDone = false
//        var outputDone = false // 输出结束
//        var inputBuffers: Array<ByteBuffer?>? = null
//        //开始进行解码。
//        var count = 1
//        while (!outputDone) {
//            if (requestStop) {
//                return
//            }
//            if (!inputDone) {
//                //feed data
//                val inputBufferIndex = codec.dequeueInputBuffer(timeOut)
//                if (inputBufferIndex >= 0) {
//                    val inputBuffer: ByteBuffer? = codec.getInputBuffer(inputBufferIndex)
//                    val sampleData = extractor.readSampleData(inputBuffer, 0)
//                    if (sampleData > 0) {
//                        val sampleTime = extractor.sampleTime
//                        codec.queueInputBuffer(inputBufferIndex, 0, sampleData, sampleTime, 0)
//                        //继续
//                        if (interval === 0) {
//                            extractor.advance()
//                        } else {
//                            extractor.seekTo(
//                                count * interval * 1000,
//                                MediaExtractor.SEEK_TO_PREVIOUS_SYNC
//                            )
//                            count++
//                            //                                        extractor.advance();
//                        }
//                    } else {
//                        //小于0，说明读完了
//                        codec.queueInputBuffer(
//                            inputBufferIndex,
//                            0,
//                            0,
//                            0L,
//                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
//                        )
//                        inputDone = true
//                        Log.d(TAG, "end of stream")
//                    }
//                }
//            }
//            if (!outputDone) {
//                //get data
//                when (val status = codec.dequeueOutputBuffer(bufferInfo, timeOut)) {
//                    MediaCodec.INFO_TRY_AGAIN_LATER -> {
//                        // 继续
//                    }
//                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
//                        // 开始进行解码
//                    }
//                    MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
//                        // 同样啥都不做
//                    }
//                    else -> {
//                        // 在这里判断，当前编码器的状态
//                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
//                            Log.d(TAG, "output EOS")
//                            outputDone = true
//                        }
//                        var doRender = bufferInfo.size != 0
//                        val presentationTimeUs = bufferInfo.presentationTimeUs
//                        if (lastPresentationTimeUs === 0) {
//                            lastPresentationTimeUs = presentationTimeUs
//                        } else {
//                            val diff: Long = presentationTimeUs - lastPresentationTimeUs
//                            if (interval !== 0) {
//                                if (diff < interval * 1000) {
//                                    doRender = false
//                                } else {
//                                    lastPresentationTimeUs = presentationTimeUs
//                                }
//                                Log.d(
//                                    TAG,
//                                    "diff time in ms =" + diff / 1000
//                                )
//                            }
//                        }
//                        // 有数据了.因为会直接传递给Surface，所以说明都不做好了
//                        Log.d(
//                            TAG, "surface decoder given buffer " + status + " (size=" + bufferInfo.size + ")" + ",doRender = " + doRender + ", presentationTimeUs=" + presentationTimeUs
//                        )
//                        // 直接送显就可以了
//                        codec.releaseOutputBuffer(status, doRender)
//                    }
//                }
//            }
//        }
    }

    /**
     * 获取视频轨道信息
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getVideoTrackInfo(extractor: MediaExtractor): Pair<Int, MediaFormat?> {
        for (index in 0 until extractor.trackCount) {
            val trackFormat = extractor.getTrackFormat(index)
            if (trackFormat.getString(MediaFormat.KEY_MIME, "").contains("video")) {
                return Pair(index, trackFormat)
            }
        }
        return Pair(-1, null)
    }
}