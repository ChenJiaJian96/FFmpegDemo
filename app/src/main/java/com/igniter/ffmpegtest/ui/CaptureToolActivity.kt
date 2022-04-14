package com.igniter.ffmpegtest.ui

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis.XAxisPosition
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.igniter.ffmpeg.R
import com.igniter.ffmpegtest.data.repository.FFmpegRepoImpl
import com.igniter.ffmpegtest.domain.bean.RepoType
import com.igniter.ffmpegtest.ui.chart.MyAxisValueFormatter
import com.igniter.ffmpegtest.ui.common.SelectRepoDialog
import com.igniter.ffmpegtest.ui.common.SelectStrategyDialog
import com.igniter.ffmpegtest.ui.common.VideoExtractInfoDialog
import com.igniter.ffmpegtest.viewmodel.BarChartViewModel
import com.igniter.ffmpegtest.viewmodel.CaptureViewModel
import com.igniter.ffmpegtest.viewmodel.CaptureViewModel.Companion.FRAME_NUM
import kotlinx.coroutines.launch

class CaptureToolActivity : AppCompatActivity(), OnChartValueSelectedListener {

    companion object {
        private const val TAG = "CaptureToolActivity"
    }

    private val listAdapter = FrameListAdapter(this, FRAME_NUM)

    private val barChartViewModel: BarChartViewModel by viewModels()
    private val captureViewModel: CaptureViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture_tool)

        initStartCaptureBtn()
        initSelectRepoBtn()
        initSelectStrategyBtn()
        initProgressBar()
        initRecyclerView()
        initChart()
        initVideoInfoView()

        initViewModel()
    }

    private fun initVideoInfoView() {
        captureViewModel.resultData.observe(this) { dataTriple ->
            VideoExtractInfoDialog(this).apply {
                setTitleText(getString(R.string.app_capture_succeed))
                setContentText(
                    "${dataTriple.first} | ${dataTriple.second} | ${dataTriple.third}".trimIndent()
                )
                show()
            }
        }

        captureViewModel.latestDurationData.observe(this) { durationInfo ->
            barChartViewModel.update(durationInfo)
        }
    }

    private fun initProgressBar() {
        val progressBar: ProgressBar = findViewById(R.id.progress_bar)
        progressBar.max = FRAME_NUM
        lifecycleScope.launch {
            captureViewModel.cacheFrameUpdatedIndex.observe(this@CaptureToolActivity) { index ->
                Log.d(TAG, "cacheFrameUpdatedIndex: $index update progress")
                progressBar.progress = captureViewModel.frameInfoList.count { it != null }
            }
        }
    }

    private fun initRecyclerView() {
        val frameListView: RecyclerView = findViewById(R.id.rv_frame_list)
        with(frameListView) {
            adapter = listAdapter
            layoutManager = LinearLayoutManager(this@CaptureToolActivity).apply {
                orientation = HORIZONTAL
            }
        }
        lifecycleScope.launch {
            captureViewModel.cacheFrameUpdatedIndex.observe(this@CaptureToolActivity) { index ->
                Log.d(TAG, "cacheFrameUpdatedIndex: $index update frames")
                listAdapter.onBitmapUpdated(index, captureViewModel.frameInfoList[index])
            }
        }
    }

    private fun initViewModel() {
        val testVideoPath = intent.getStringExtra(HomeActivity.BUNDLE_KEY_VIDEO_PATH) ?: ""
        captureViewModel.videoPath = testVideoPath
    }

    private fun initStartCaptureBtn() {
        val startCaptureBtn: Button = findViewById(R.id.start_capture)
        startCaptureBtn.setOnClickListener {
            captureViewModel.startCapture(this)
        }
    }

    private fun initSelectRepoBtn() {
        val selectRepoBtn: Button = findViewById(R.id.select_repo)
        selectRepoBtn.setOnClickListener {
            SelectRepoDialog(this).also { dialog ->
                dialog.initType = captureViewModel.captureFrameRepo.type
                dialog.confirmListener = { _, repoType ->
                    captureViewModel.switchCaptureRepository(repoType = repoType)
                }
            }.show()
        }
    }

    private fun initSelectStrategyBtn() {
        val ffmpegStrategyBtn: Button = findViewById(R.id.select_ffmpeg_strategy)
        ffmpegStrategyBtn.setOnClickListener {
            SelectStrategyDialog(this).also { dialog ->
                dialog.initStrategy = (captureViewModel.captureFrameRepo as FFmpegRepoImpl).captureStrategy
                dialog.confirmListener = { _, strategy ->
                    captureViewModel.updateFFmpegStrategy(strategy)
                }
            }.show()
        }
        captureViewModel.currentRepoType.observe(this) {
            ffmpegStrategyBtn.isEnabled = it == RepoType.FFmpeg
        }
    }

    private fun initChart() {
        val barChart: BarChart = findViewById(R.id.bar_chart)
        with(barChart) {
            setOnChartValueSelectedListener(this@CaptureToolActivity)
            description.isEnabled = false
            setMaxVisibleValueCount(40)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(false)
            isHighlightFullBarEnabled = false
        }

        barChart.axisLeft.apply {
            valueFormatter = MyAxisValueFormatter()
            axisMinimum = 0f
        }
        barChart.axisRight.isEnabled = false
        barChart.xAxis.position = XAxisPosition.TOP

        barChart.legend.apply {
            verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            orientation = Legend.LegendOrientation.HORIZONTAL
            setDrawInside(false)
            formSize = 8f
            formToTextSpace = 4f
            xEntrySpace = 6f
        }

        barChartViewModel.barData.observe(this) {
            barChart.data = it
            barChart.data.notifyDataChanged()
            barChart.notifyDataSetChanged()
        }
    }

    override fun onValueSelected(e: Entry?, h: Highlight?) {}

    override fun onNothingSelected() {}
}