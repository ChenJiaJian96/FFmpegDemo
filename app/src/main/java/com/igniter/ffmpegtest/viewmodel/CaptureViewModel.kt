package com.igniter.ffmpegtest.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.igniter.ffmpeg.R
import com.igniter.ffmpegtest.data.repository.FFmpegRepoImpl
import com.igniter.ffmpegtest.data.repository.MMRRepoImpl
import com.igniter.ffmpegtest.data.repository.MediaCodecRepoImpl
import com.igniter.ffmpegtest.domain.bean.CaptureDuration
import com.igniter.ffmpegtest.domain.bean.CaptureFrameListener
import com.igniter.ffmpegtest.domain.bean.FFmpegStrategy
import com.igniter.ffmpegtest.domain.bean.FrameInfo
import com.igniter.ffmpegtest.domain.bean.RepoType
import com.igniter.ffmpegtest.domain.repository.CaptureRepository
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 抽帧逻辑 ViewModel
 * 负责执行抽帧、帧信息管理和回调等职责
 */
class CaptureViewModel : ViewModel() {

    /**
     * 视频路径
     */
    var videoPath: String = ""

    /**
     * 抽帧数量
     */
    var captureCount: Int = FRAME_NUM

    /**
     * 视频信息
     */
    private var videoInfo: String = ""

    /**
     * 抽帧耗时信息
     */
    val resultData: LiveData<Triple<String, String, String>> get() = _resultData
    private val _resultData: MutableLiveData<Triple<String, String, String>> = MutableLiveData()

    /**
     * 本地缓存帧数量
     */
    val cacheFrameUpdatedIndex: LiveData<Int> get() = _cacheFrameUpdatedIndex
    private val _cacheFrameUpdatedIndex: MutableLiveData<Int> = MutableLiveData()

    /**
     * 最近回调的耗时数据
     */
    val latestDurationData: LiveData<CaptureDuration> get() = _latestDurationData
    private val _latestDurationData: MutableLiveData<CaptureDuration> = MutableLiveData()

    /**
     * 本地缓存帧列表
     */
    val frameInfoList: CopyOnWriteArrayList<FrameInfo?> = CopyOnWriteArrayList(arrayOfNulls(FRAME_NUM))

    var captureFrameRepo: CaptureRepository = FFmpegRepoImpl()
        private set

    private var startTimeMs: Long = 0L

    fun switchCaptureRepository(repoType: RepoType) {
        Log.d(TAG, "switchCaptureRepository: switchRepo: ${repoType.javaClass.name}")
        captureFrameRepo = when (repoType) {
            RepoType.FFmpeg -> FFmpegRepoImpl()
            RepoType.MMR -> MMRRepoImpl()
            RepoType.MediaCodec -> MediaCodecRepoImpl()
        }

        clearCache()
    }

    fun updateFFmpegStrategy(ffmpegStrategy: FFmpegStrategy) {
        Log.d(TAG, "updateFFmpegStrategy.")
        (captureFrameRepo as? FFmpegRepoImpl)?.updateStrategy(ffmpegStrategy)

        clearCache()
    }

    /**
     * 执行抽帧
     */
    fun startCapture(context: Context) {
        startTimeMs = System.currentTimeMillis()
        captureFrameRepo.captureFrames(
            videoPath = videoPath,
            frameCount = captureCount,
            callback = object : CaptureFrameListener {
                override fun onVideoInfoRetrieved(width: Int, height: Int, durationMs: Long) {
                    videoInfo = context.getString(R.string.app_video_info, videoPath, width, height, durationMs)
                }

                override fun onBitmapCaptured(index: Int, timestampMs: Long, bitmap: Bitmap) {
                    Log.d(TAG, "$index | $timestampMs | $bitmap")
                    val frameInfo = FrameInfo(index, timestampMs, bitmap)
                    doOnBitmapCaptured(frameInfo, context)
                }
            }
        )
    }

    private fun doOnBitmapCaptured(frameInfo: FrameInfo, context: Context) {
        onBitmapUpdated(frameInfo)

        if (!frameInfoList.contains(null)) {
            val captureInfo = context.getString(R.string.app_capture_num, captureCount)
            val totalDuration = context.getString(
                R.string.app_capture_duration,
                System.currentTimeMillis() - startTimeMs
            )
            _resultData.postValue(Triple(captureInfo, videoInfo, totalDuration))
        }
    }

    private fun clearCache() {
        frameInfoList.fill(null)
    }

    private fun onBitmapUpdated(frameInfo: FrameInfo) {
        frameInfoList[frameInfo.index] = frameInfo
        _cacheFrameUpdatedIndex.postValue(frameInfo.index)
    }

    companion object {
        private const val TAG = "CaptureViewModel"
        const val FRAME_NUM = 20
    }
}