package com.igniter.ffmpeg

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.igniter.ffmpeg.CaptureToolActivity

class HomeActivity : AppCompatActivity() {

    // 视频播放容器
    private lateinit var playVideoView: Button
    private lateinit var captureFrameView: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        playVideoView = findViewById(R.id.test_play_video)
        playVideoView.setOnClickListener {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE),
                REQUEST_PERMISSION_PLAY_VIDEO
            )
        }
        captureFrameView = findViewById(R.id.test_capture_frame)
        captureFrameView.setOnClickListener {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE),
                REQUEST_PERMISSION_CAPTURE_FRAME
            )
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.contains(PERMISSION_DENIED)) return
        when (requestCode) {
            REQUEST_PERMISSION_PLAY_VIDEO -> startActivity(Intent(this, PlayVideoActivity::class.java))
            REQUEST_PERMISSION_CAPTURE_FRAME -> startActivity(Intent(this, CaptureToolActivity::class.java))
        }
    }

    companion object {
        //        const val TEST_VIDEO_PATH = "/storage/emulated/0/kandian_test.mov"
        const val TEST_VIDEO_PATH = "/storage/emulated/0/test.mp4"

        private const val REQUEST_PERMISSION_PLAY_VIDEO = 1
        private const val REQUEST_PERMISSION_CAPTURE_FRAME = 2
    }
}