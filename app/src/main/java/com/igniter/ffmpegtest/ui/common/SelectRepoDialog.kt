package com.igniter.ffmpegtest.ui.common

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import com.igniter.ffmpeg.R
import com.igniter.ffmpegtest.domain.bean.RepoType

typealias CaptureRepoCallbackListener = (View, RepoType) -> Unit

class SelectRepoDialog(context: Context) : Dialog(context, R.style.CommonDialog) {

    private lateinit var repoRadioGroup: RadioGroup
    private lateinit var leftView: TextView
    private lateinit var rightView: TextView

    var leftMessage: String = context.getString(R.string.app_cancel)
    var rightMessage: String = context.getString(R.string.app_confirm)
    var confirmListener: CaptureRepoCallbackListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_select_repo)

        initView()
        bindButtonView()
    }

    private fun initView() {
        repoRadioGroup = findViewById(R.id.repo_radio_group)
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
            confirmListener?.invoke(it, getCurrentSelectedRepoType())
            dismiss()
        }
    }

    private fun getCurrentSelectedRepoType(): RepoType {
        return when(repoRadioGroup.checkedRadioButtonId) {
            R.id.rb_mediacodec -> RepoType.MediaCodec
            R.id.rb_ffmpeg -> RepoType.FFmpeg
            else -> RepoType.MMR
        }
    }
}