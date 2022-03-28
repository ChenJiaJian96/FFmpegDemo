package com.igniter.ffmpegtest.ui

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.RadioGroup
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
import com.igniter.ffmpegtest.viewmodel.CaptureViewModel
import com.igniter.ffmpegtest.viewmodel.CaptureViewModel.Companion.FRAME_NUM
import com.igniter.ffmpegtest.viewmodel.BarChartViewModel
import com.igniter.ffmpegtest.ui.chart.MyAxisValueFormatter
import com.igniter.ffmpegtest.ui.common.CommonContentDialog
import com.igniter.ffmpegtest.ui.grid.FrameListAdapter

class CaptureToolActivity : AppCompatActivity(), OnChartValueSelectedListener {

    private lateinit var progressBar: ProgressBar
    private lateinit var frameListView: RecyclerView
    private lateinit var multiThreadEnabledBox: CheckBox
    private lateinit var strategyRadioGroup: RadioGroup
    private lateinit var seekFlagRadioGroup: RadioGroup
    private lateinit var startCaptureBtn: Button // 视频帧列表容器
    private lateinit var barChart: BarChart // 抽帧数据展示容器

    private val listAdapter = FrameListAdapter(this, FRAME_NUM)

    private val strategyButtonIdList =
        arrayListOf(R.id.first_strategy_button, R.id.second_strategy_button)
    private val seekFlagButtonIdList =
        arrayListOf(R.id.first_seek_flag_button, R.id.second_seek_flag_button)

    private val barChartViewModel: BarChartViewModel by viewModels()
    private val captureViewModel: CaptureViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture_tool)

        initStartCaptureBtn()
        initProgressBar()
        initRecyclerView()
        initChart()
        initVideoInfoView()
        initStrategySelections()

        initViewModel()
    }

    private fun initStrategySelections() {
        multiThreadEnabledBox = findViewById(R.id.multi_thread_box)
        multiThreadEnabledBox.setOnCheckedChangeListener { _, isChecked ->
            captureViewModel.updateEnableMultiThread(isChecked)
        }

        strategyRadioGroup = findViewById(R.id.strategy_radio_group)
        strategyRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            captureViewModel.updateStrategyIndex(strategyButtonIdList.indexOf(checkedId))
        }

        seekFlagRadioGroup = findViewById(R.id.seek_flag_radio_group)
        seekFlagRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            captureViewModel.updateSeekFlagIndex(seekFlagButtonIdList.indexOf(checkedId))
        }
    }

    private fun initVideoInfoView() {
        captureViewModel.resultData.observe(this) { dataTriple ->
            CommonContentDialog(this).apply {
                setTitleText(getString(R.string.app_capture_succeed))
                setContentText(
                    """
                        ${dataTriple.first}
                        ${dataTriple.second}
                        ${dataTriple.third}
                    """.trimIndent()
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
        // init video path
        val testVideoPath = intent.getStringExtra(HomeActivity.BUNDLE_KEY_VIDEO_PATH) ?: ""
        captureViewModel.videoPath = testVideoPath

        // init capture strategy
        captureViewModel.initStrategy(
            multiThreadEnabledBox.isChecked,
            strategyButtonIdList.indexOf(strategyRadioGroup.checkedRadioButtonId),
            seekFlagButtonIdList.indexOf(seekFlagRadioGroup.checkedRadioButtonId)
        )
    }

    private fun initStartCaptureBtn() {
        startCaptureBtn = findViewById(R.id.start_capture)
        startCaptureBtn.setOnClickListener {
            captureViewModel.startCapture(this)
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