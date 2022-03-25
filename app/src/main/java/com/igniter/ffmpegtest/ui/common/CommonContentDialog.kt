package com.igniter.ffmpegtest.ui.common

import android.app.Dialog
import android.content.Context
import android.widget.TextView
import com.igniter.ffmpeg.R

/**
 * 展示标题和文案的弹窗
 */
class CommonContentDialog(context: Context) : Dialog(context) {

    private var titleView: TextView
    private var contentView: TextView

    init {
        setContentView(R.layout.dialog_video_extract_info)
        titleView = findViewById(R.id.tv_dialog_title)
        contentView = findViewById(R.id.tv_dialog_content)

        setCanceledOnTouchOutside(true)
    }

    fun setTitleText(title: CharSequence) {
        titleView.text = title
    }

    fun setContentText(title: CharSequence) {
        contentView.text = title
    }

}