package com.igniter.ffmpegtest.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.igniter.ffmpeg.R
import com.igniter.ffmpegtest.VideoManager
import com.igniter.ffmpegtest.VideoManager.OnCaptureDataCallbackListener.Companion.STEP_OUTPUT
import com.igniter.ffmpegtest.VideoManager.OnCaptureDataCallbackListener.Companion.STEP_RETRIEVE
import com.igniter.ffmpegtest.VideoManager.OnCaptureDataCallbackListener.Companion.STEP_SEEK_AND_DECODE
import com.igniter.ffmpegtest.bean.CaptureDuration
import com.igniter.ffmpegtest.bean.CaptureStrategy
import com.igniter.ffmpegtest.bean.FrameInfo
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
    val cacheFrameNum: LiveData<Int> get() = _cacheFrameNum
    private val _cacheFrameNum: MutableLiveData<Int> = MutableLiveData(0)

    /**
     * 最近回调的耗时数据
     */
    val latestDurationData: LiveData<CaptureDuration> get() = _latestDurationData
    private val _latestDurationData: MutableLiveData<CaptureDuration> = MutableLiveData()

    /**
     * 本地缓存帧列表
     */
    private val frameInfoList: Array<FrameInfo?> = arrayOfNulls(FRAME_NUM)

    /**
     * 抽帧策略选择封装
     */
    private val strategy = CaptureStrategy()

    /**
     * 抽帧逻辑实现管理类
     */
    private val videoManager = VideoManager()

    /**
     * 初始化抽帧策略
     */
    fun initStrategy(enableMultiThread: Boolean, strategyIndex: Int, seekFlagIndex: Int) {
        strategy.enableMultiThread = enableMultiThread
        strategy.strategyIndex = strategyIndex
        strategy.seekFlagIndex = seekFlagIndex
    }

    /**
     * 更新抽帧策略——是否允许多线程抽帧
     */
    fun updateEnableMultiThread(enableMultiThread: Boolean) {
        strategy.enableMultiThread = enableMultiThread
    }

    /**
     * 更新抽帧策略——更新抽帧策略选择
     */
    fun updateStrategyIndex(strategyIndex: Int) {
        strategy.strategyIndex = strategyIndex
    }

    /**
     * 更新抽帧策略——更新seek策略索引
     */
    fun updateSeekFlagIndex(seekFlagIndex: Int) {
        strategy.strategyIndex = seekFlagIndex
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
            videoManager.capture(
                videoPath = videoPath,
                totalNum = captureNum,
                enableMultiThread = strategy.enableMultiThread,
                strategyIndex = strategy.strategyIndex,
                seekFlagIndex = strategy.seekFlagIndex,
                onBitmapCallbackListener = object : VideoManager.OnCaptureDataCallbackListener {
                    override fun onVideoInfoRetrieved(width: Int, height: Int, durationMs: Long) {
                        videoInfo = context.getString(
                            R.string.app_video_info,
                            videoPath,
                            width,
                            height,
                            durationMs
                        )
                    }

                    override fun onBitmapCaptured(index: Int, timestamp: Long, bitmap: Bitmap, isFinished: Boolean) {
                        onBitmapUpdated(
                            FrameInfo(
                                index = index,
                                timestamp = timestamp,
                                bitmap = bitmap
                            )
                        )

                        if (isFinished) {
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
                            STEP_RETRIEVE -> {
                                lastCaptureDuration = CaptureDuration()
                                lastCaptureDuration.index = index
                                lastCaptureDuration.retrieveMs = curStepDurationMs
                                _latestDurationData.postValue(lastCaptureDuration)
                            }

                            STEP_SEEK_AND_DECODE -> {
                                lastCaptureDuration = CaptureDuration()
                                lastCaptureDuration.index = index
                                lastCaptureDuration.seekAndDecodeMs = curStepDurationMs
                            }
                            STEP_OUTPUT -> {
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
        _cacheFrameNum.value = 0
    }

    private fun onBitmapUpdated(frameInfo: FrameInfo) {
        frameInfoList[frameInfo.index] = frameInfo
        _cacheFrameNum.postValue(frameInfoList.count { it != null })
    }

    companion object {
        private const val TAG = "CaptureViewModel"
        const val FRAME_NUM = 20
    }
}