package com.igniter.ffmpegtest.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.igniter.ffmpeg.R
import com.igniter.ffmpegtest.data.repository.MMRRepoImpl
import com.igniter.ffmpegtest.domain.bean.CaptureDuration
import com.igniter.ffmpegtest.domain.bean.CaptureFrameListener
import com.igniter.ffmpegtest.domain.bean.CaptureFrameListener.Companion.STEP_FAILED
import com.igniter.ffmpegtest.domain.bean.CaptureFrameListener.Companion.STEP_OUTPUT
import com.igniter.ffmpegtest.domain.bean.CaptureFrameListener.Companion.STEP_RETRIEVE
import com.igniter.ffmpegtest.domain.bean.CaptureFrameListener.Companion.STEP_SEEK_AND_DECODE
import com.igniter.ffmpegtest.domain.bean.FrameInfo
import com.igniter.ffmpegtest.domain.repository.CaptureRepository
import kotlin.concurrent.thread

/**
 * 抽帧逻辑 View
 * 负责执行抽帧、帧信息管理和回调等职责
 */
class CaptureViewModel : ViewModel() {

    /**
     * 视频路径
     */
    var videoPath: String = ""

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
    val frameInfoList: Array<FrameInfo?> = arrayOfNulls(FRAME_NUM)

    private var captureFrameRepo: CaptureRepository = MMRRepoImpl()

    fun switchCaptureRepository(repo: CaptureRepository) {
        this.captureFrameRepo = repo
    }

    /**
     * 执行抽帧
     */
    fun startCapture(context: Context) {
        clearCache()

        captureFrames(context, videoPath)
    }

    private fun captureFrames(context: Context, videoPath: String, captureNum: Int = FRAME_NUM) {
        val startTimeMs = System.currentTimeMillis()
        var lastFrameTimeMs = startTimeMs

        thread {
            var lastCaptureDuration = CaptureDuration()
            captureFrameRepo.captureFrames(
                videoPath = videoPath,
                totalNum = captureNum,
                callback = object : CaptureFrameListener {
                    override fun onVideoInfoRetrieved(width: Int, height: Int, durationMs: Long) {
                        videoInfo = context.getString(
                            R.string.app_video_info,
                            videoPath,
                            width,
                            height,
                            durationMs
                        )
                    }

                    override fun onBitmapCaptured(index: Int, timestampMs: Long, bitmap: Bitmap) {
                        onBitmapUpdated(
                            FrameInfo(
                                index = index,
                                timestamp = timestampMs,
                                bitmap = bitmap
                            )
                        )

                        if (!frameInfoList.contains(null)) {
                            val captureInfo = context.getString(R.string.app_capture_num, captureNum)
                            val totalDuration = context.getString(
                                R.string.app_capture_duration,
                                System.currentTimeMillis() - startTimeMs
                            )
                            _resultData.postValue(
                                Triple(captureInfo, videoInfo, totalDuration)
                            )
                        }
                    }

                    override fun onStepPassed(index: Int, step: Int) {
                        val currentFrameTimeMs = System.currentTimeMillis()
                        val curStepDurationMs = currentFrameTimeMs - lastFrameTimeMs
                        Log.d(TAG, "onStepPassed: index: $index, step: $step, curStepDuration: $curStepDurationMs")
                        when (step) {
                            STEP_FAILED -> {
                                Toast.makeText(context, "抽帧失败了，请通过日志查看原因", Toast.LENGTH_LONG).show()
                            }
                            STEP_RETRIEVE -> {
                                lastCaptureDuration = CaptureDuration()
                                lastCaptureDuration.index = index
                                lastCaptureDuration.retrieveMs = curStepDurationMs
                                _latestDurationData.postValue(lastCaptureDuration)
                            }
                            STEP_SEEK_AND_DECODE -> {
                                lastCaptureDuration = CaptureDuration()
                                lastCaptureDuration.seekAndDecodeMs = curStepDurationMs
                            }
                            STEP_OUTPUT -> {
                                lastCaptureDuration.index = index
                                lastCaptureDuration.outputMs = curStepDurationMs
                                _latestDurationData.postValue(lastCaptureDuration)
                            }
                        }

                        lastFrameTimeMs = currentFrameTimeMs
                    }
                })
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