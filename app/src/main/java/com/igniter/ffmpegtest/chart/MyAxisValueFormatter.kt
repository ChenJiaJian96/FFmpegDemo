package com.igniter.ffmpegtest.chart

import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.DecimalFormat

class MyAxisValueFormatter : ValueFormatter() {

    private val format: DecimalFormat = DecimalFormat("###,###,###,##0.0")

    override fun getFormattedValue(value: Float, axis: AxisBase): String {
        return format.format(value.toDouble()) + " ms"
    }
}
