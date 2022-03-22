package com.igniter.ffmpegtest

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis.XAxisPosition
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.igniter.ffmpeg.R
import com.igniter.ffmpegtest.CaptureViewModel.Companion.FRAME_NUM
import com.igniter.ffmpegtest.chart.BarChartViewModel
import com.igniter.ffmpegtest.chart.MyAxisValueFormatter

class CaptureToolActivity : AppCompatActivity(), OnChartValueSelectedListener {

    private lateinit var progressBar: ProgressBar
    private lateinit var captureNumView: TextView
    private lateinit var durationView: TextView
    private lateinit var videoInfoView: TextView
    private lateinit var multiThreadEnabledBox: CheckBox
    private lateinit var strategyRadioGroup: RadioGroup
    private lateinit var seekFlagRadioGroup: RadioGroup
    private lateinit var startCaptureBtn: Button // 视频帧列表容器
    private lateinit var barChart: BarChart // 抽帧数据展示容器

    private val strategyButtonIdList = arrayListOf(R.id.first_strategy_button, R.id.second_strategy_button)
    private val seekFlagButtonIdList = arrayListOf(R.id.first_seek_flag_button, R.id.second_seek_flag_button)

    private val barChartViewModel: BarChartViewModel by viewModels()
    private val captureViewModel: CaptureViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture_tool)

        initStartCaptureBtn()
        initProgressBar()
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
        captureNumView = findViewById(R.id.capture_num)
        videoInfoView = findViewById(R.id.video_info_view)
        durationView = findViewById(R.id.total_duration)
        captureNumView.text = getString(R.string.app_capture_num, FRAME_NUM)

        captureViewModel.videoInfo.observe(this) { infoText ->
            videoInfoView.text = infoText
        }
        captureViewModel.totalDuration.observe(this) { totalDurationText ->
            durationView.text = totalDurationText
        }

        captureViewModel.latestDurationData.observe(this) { durationInfo ->
            barChartViewModel.update(durationInfo)
        }
    }

    private fun initProgressBar() {
        progressBar = findViewById(R.id.progress_bar)
        progressBar.max = FRAME_NUM
        captureViewModel.cacheFrameNum.observe(this) { cacheNum ->
            progressBar.progress = cacheNum
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