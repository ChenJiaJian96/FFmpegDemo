package com.igniter.ffmpegtest.ui

import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
import com.igniter.ffmpegtest.ui.chart.MyAxisValueFormatter
import com.igniter.ffmpegtest.ui.common.SelectRepoDialog
import com.igniter.ffmpegtest.ui.common.SelectStrategyDialog
import com.igniter.ffmpegtest.ui.common.VideoExtractInfoDialog
import com.igniter.ffmpegtest.viewmodel.BarChartViewModel
import com.igniter.ffmpegtest.viewmodel.CaptureViewModel
import com.igniter.ffmpegtest.viewmodel.CaptureViewModel.Companion.FRAME_NUM

class CaptureToolActivity : AppCompatActivity(), OnChartValueSelectedListener {

    private lateinit var progressBar: ProgressBar
    private lateinit var frameListView: RecyclerView
    private lateinit var startCaptureBtn: Button // 视频帧列表容器
    private lateinit var selectRepoBtn: Button
    private lateinit var barChart: BarChart // 抽帧数据展示容器

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
        progressBar = findViewById(R.id.progress_bar)
        progressBar.max = FRAME_NUM
        captureViewModel.cacheFrameUpdatedIndex.observe(this) { _ ->
            progressBar.progress = captureViewModel.frameInfoList.count {
                it != null
            }
        }
    }

    private fun initRecyclerView() {
        frameListView = findViewById(R.id.rv_frame_list)
        with(frameListView) {
            adapter = listAdapter
            layoutManager = LinearLayoutManager(this@CaptureToolActivity).apply {
                orientation = HORIZONTAL
            }
        }
        captureViewModel.cacheFrameUpdatedIndex.observe(this) { index ->
            captureViewModel.frameInfoList[index]?.also { newFrame ->
                listAdapter.onBitmapUpdated(newFrame)
            }
        }
    }

    private fun initViewModel() {
        val testVideoPath = intent.getStringExtra(HomeActivity.BUNDLE_KEY_VIDEO_PATH) ?: ""
        captureViewModel.videoPath = testVideoPath
    }

    private fun initStartCaptureBtn() {
        startCaptureBtn = findViewById(R.id.start_capture)
        startCaptureBtn.setOnClickListener {
            captureViewModel.startCapture(this)
        }
    }

    private fun initSelectRepoBtn() {
        selectRepoBtn = findViewById(R.id.select_repo)
        selectRepoBtn.setOnClickListener {
            SelectRepoDialog(this).also { dialog ->
                dialog.confirmListener = { _, repoType ->
                    captureViewModel.switchCaptureRepository(repoType = repoType)
                }
            }.show()
        }
    }

    private fun initSelectStrategyBtn() {
        findViewById<Button>(R.id.select_strategy).setOnClickListener {
            SelectStrategyDialog(this).also { dialog ->
                dialog.confirmListener = { _, strategy ->
                    captureViewModel.updateFFmpegStrategy(strategy)
                }
            }.show()
        }
    }

    private fun initChart() {
        barChart = findViewById(R.id.bar_chart)
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