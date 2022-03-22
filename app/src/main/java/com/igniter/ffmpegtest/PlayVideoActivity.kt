package com.igniter.ffmpegtest

import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.igniter.ffmpeg.R

class PlayVideoActivity : AppCompatActivity() {

    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder
    private lateinit var playVideoView: Button

    private val player = VideoManager()

    private var testVideoPath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play_video)

        testVideoPath = intent.getStringExtra(HomeActivity.BUNDLE_KEY_VIDEO_PATH) ?: ""

        surfaceView = findViewById(R.id.surface_view)
        surfaceHolder = surfaceView.holder
        playVideoView = findViewById(R.id.btn_play_video)
        playVideoView.setOnClickListener {
            player.playVideo(testVideoPath, surfaceHolder.surface)
        }
    }
}