package com.igniter.ffmpegtest.ui

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.igniter.ffmpeg.R

class HomeActivity : AppCompatActivity() {

    private lateinit var playVideoView: Button
    private lateinit var captureFrameView: Button

    // 视频选择
    private lateinit var radioGroup: RadioGroup
    private lateinit var firstRadioButton: RadioButton
    private lateinit var secondRadioButton: RadioButton
    private val radioButtonIdList = arrayListOf(R.id.first_radio_button, R.id.second_radio_button)
    private val videoNameList = arrayListOf(firstVideoName, secondVideoName)

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
        radioGroup = findViewById(R.id.radio_group)
        firstRadioButton = findViewById(R.id.first_radio_button)
        firstRadioButton.text = videoNameList[0]
        secondRadioButton = findViewById(R.id.second_radio_button)
        secondRadioButton.text = videoNameList[1]
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.contains(PERMISSION_DENIED)) return
        when (requestCode) {
            REQUEST_PERMISSION_PLAY_VIDEO -> startActivity(Intent(this, PlayVideoActivity::class.java).apply {
                putVideoPath()
            })
            REQUEST_PERMISSION_CAPTURE_FRAME -> startActivity(Intent(this, CaptureToolActivity::class.java).apply {
                putVideoPath()
            })
        }
    }

    private fun Intent.putVideoPath() {
        putExtra(BUNDLE_KEY_VIDEO_PATH, getCurrentVideoPath())
    }

    private fun getCurrentVideoPath(): String {
        val videoName = videoNameList[radioButtonIdList.indexOf(radioGroup.checkedRadioButtonId)]
        return Environment.getExternalStorageDirectory().path + "/download/$videoName"
    }

    companion object {
        const val BUNDLE_KEY_VIDEO_PATH = "bundle_key_video_path"
        private const val firstVideoName = "kandian_test_1.mp4"
        private const val secondVideoName = "kandian_test_2.mp4"

        private const val REQUEST_PERMISSION_PLAY_VIDEO = 1
        private const val REQUEST_PERMISSION_CAPTURE_FRAME = 2
    }
}