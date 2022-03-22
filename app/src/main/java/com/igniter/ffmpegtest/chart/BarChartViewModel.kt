package com.igniter.ffmpegtest.chart

import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import com.igniter.ffmpegtest.bean.CaptureDuration
import java.util.ArrayList

/**
 * 耗时分析图标数据 ViewModel
 */
class BarChartViewModel : ViewModel() {

    val barData: LiveData<BarData> get() = _barData
    private val _barData: MutableLiveData<BarData> = MutableLiveData()

    private val barEntryList = ArrayList<BarEntry>()
    private val dataSets = ArrayList<IBarDataSet>()

    init {
        for (i in 0 until 21) {
            val val1 = 150f
            val val2 = 150f
            val val3 = 150f
            barEntryList.add(BarEntry(i.toFloat(), floatArrayOf(val1, val2, val3), null))
        }

        val dataSet = BarDataSet(barEntryList, TITLE).also {
            it.setDrawIcons(false)
            it.setColors(*dataSetColors())
            it.stackLabels = arrayOf(RETRIEVE_TAG, SEEK_AND_DECODE_TAG, OUTPUT_TAG)
        }
        dataSets.add(dataSet)

        _barData.value = BarData(dataSets).also {
            it.setValueFormatter(MyAxisValueFormatter())
            it.setValueTextColor(Color.WHITE)
        }
    }

    fun update(durationBean: CaptureDuration) {
        with(durationBean) {
            val barEntry = BarEntry(
                index.toFloat(),
                floatArrayOf(retrieveMs.toFloat(), seekAndDecodeMs.toFloat(), outputMs.toFloat()),
                null
            )
            barEntryList[index] = barEntry

            _barData.value = _barData.value
        }
    }

    private fun dataSetColors(): IntArray {
        val colors = IntArray(3)
        System.arraycopy(ColorTemplate.MATERIAL_COLORS, 0, colors, 0, 3)
        return colors
    }

    companion object {
        private const val TITLE = "「抽帧耗时分析」"

        private const val RETRIEVE_TAG = "retrieve"
        private const val SEEK_AND_DECODE_TAG = "seek_and_decode"
        private const val OUTPUT_TAG = "output"
    }
}