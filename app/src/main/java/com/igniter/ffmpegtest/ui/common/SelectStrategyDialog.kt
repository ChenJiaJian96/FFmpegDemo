package com.igniter.ffmpegtest.ui.common

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.RadioGroup
import android.widget.TextView
import com.igniter.ffmpeg.R
import com.igniter.ffmpegtest.domain.bean.CaptureStrategy

typealias CaptureStrategyCallbackListener = (View, CaptureStrategy) -> Unit

class SelectStrategyDialog(context: Context) : Dialog(context, R.style.CommonDialog) {

    private lateinit var multiThreadEnabledBox: CheckBox
    private lateinit var strategyRadioGroup: RadioGroup
    private lateinit var seekFlagRadioGroup: RadioGroup

    private lateinit var leftView: TextView
    private lateinit var rightView: TextView

    var leftMessage: String = context.getString(R.string.app_cancel)
    var rightMessage: String = context.getString(R.string.app_confirm)
    var confirmListener: CaptureStrategyCallbackListener? = null

    private val strategyButtonIdList =
        arrayListOf(R.id.first_strategy_button, R.id.second_strategy_button)
    private val seekFlagButtonIdList =
        arrayListOf(R.id.first_seek_flag_button, R.id.second_seek_flag_button)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_select_strategy)

        initView()
        bindButtonView()
    }

    private fun initView() {
        multiThreadEnabledBox = findViewById(R.id.multi_thread_box)
        strategyRadioGroup = findViewById(R.id.strategy_radio_group)
        seekFlagRadioGroup = findViewById(R.id.seek_flag_radio_group)
        leftView = findViewById(R.id.tv_left)
        rightView = findViewById(R.id.tv_right)
    }

    private fun bindButtonView() {
        if (leftMessage.isNotEmpty()) leftView.text = leftMessage
        leftView.setOnClickListener {
            dismiss()
        }

        if (rightMessage.isNotEmpty()) rightView.text = rightMessage
        rightView.setOnClickListener {
            confirmListener?.invoke(it, buildCaptureStrategy())
            dismiss()
        }
    }

    private fun buildCaptureStrategy(): CaptureStrategy {
        return CaptureStrategy().also {
            it.enableMultiThread = multiThreadEnabledBox.isChecked
            it.strategyIndex = strategyButtonIdList.indexOf(strategyRadioGroup.checkedRadioButtonId)
            it.seekFlagIndex = seekFlagButtonIdList.indexOf(seekFlagRadioGroup.checkedRadioButtonId)
        }
    }


}