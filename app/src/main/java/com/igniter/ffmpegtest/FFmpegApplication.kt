package com.igniter.ffmpegtest

import android.app.Application
import com.igniter.ffmpegtest.data.data_source.FFmpegSolution

class FFmpegApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        prepareEnv()
    }

    private fun prepareEnv() {
        System.loadLibrary("video")
        FFmpegSolution.prepareCaptureEnv()
    }
}