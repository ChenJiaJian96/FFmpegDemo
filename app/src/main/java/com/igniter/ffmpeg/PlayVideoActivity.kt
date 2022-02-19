package com.igniter.ffmpeg

import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.igniter.ffmpeg.HomeActivity.Companion.TEST_VIDEO_PATH

class PlayVideoActivity : AppCompatActivity() {

    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder
    private lateinit var playVideoView: Button

    private val player = VideoManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play_video)

        surfaceView = findViewById(R.id.surface_view)
        surfaceHolder = surfaceView.holder
        playVideoView = findViewById(R.id.btn_play_video)
        playVideoView.setOnClickListener {
            player.playVideo(TEST_VIDEO_PATH, surfaceHolder.surface)
        }
    }
}