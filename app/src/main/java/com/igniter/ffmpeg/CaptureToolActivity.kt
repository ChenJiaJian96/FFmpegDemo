package com.igniter.ffmpeg

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.igniter.ffmpeg.HomeActivity.Companion.TEST_VIDEO_PATH
import kotlin.concurrent.thread

class CaptureToolActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var fileNameView: TextView
    private lateinit var captureNumView: TextView
    private lateinit var durationView: TextView

    // 视频帧列表容器
    private lateinit var startCaptureBtn: Button
    private lateinit var recyclerview: RecyclerView
    private val frameAdapter = FrameAdapter(FRAME_NUM)

    private val player = VideoManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture_tool)
        startCaptureBtn = findViewById(R.id.start_capture)
        startCaptureBtn.setOnClickListener {
            captureFirstFrame()
        }

        progressBar = findViewById(R.id.progress_bar)
        progressBar.max = FRAME_NUM

        recyclerview = findViewById(R.id.recycler_view)
        recyclerview.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerview.adapter = frameAdapter

        fileNameView = findViewById(R.id.file_name)
        captureNumView = findViewById(R.id.capture_num)
        durationView = findViewById(R.id.total_duration)
        fileNameView.text = "文件名：$TEST_VIDEO_PATH"
        captureNumView.text = "抽帧数量：$FRAME_NUM"
    }

    private fun captureFirstFrame() {
        val startTimeMs = System.currentTimeMillis()
        var lastFrameTimeMs = System.currentTimeMillis()
        val indexSet = mutableSetOf<Int>()
        thread {
            player.capture(TEST_VIDEO_PATH, FRAME_NUM, object : VideoManager.OnBitmapCallbackListener {
                override fun onBitmapCaptured(index: Int, timestamp: Long, bitmap: Bitmap?) {
                    val currentFrameTimeMs = System.currentTimeMillis()
                    indexSet.add(index)
                    runOnUiThread {
                        progressBar.progress = indexSet.size
                        if (bitmap == null) {
                            Toast.makeText(
                                this@CaptureToolActivity,
                                "native bitmap result == null, index = $index",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@runOnUiThread
                        }

                        frameAdapter.onBitmapUpdated(
                            FrameInfo(
                                index = index,
                                timestamp = timestamp,
                                bitmap = bitmap,
                                timeCost = currentFrameTimeMs - lastFrameTimeMs
                            )
                        )
                        lastFrameTimeMs = currentFrameTimeMs
                    }
                    if (indexSet.size == FRAME_NUM) {
                        runOnUiThread {
                            durationView.text = "总用时：${System.currentTimeMillis() - startTimeMs} ms"
                        }
                    }
                }
            })
        }
    }

    companion object {
        private const val FRAME_NUM = 20
    }
}