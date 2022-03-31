package com.igniter.ffmpegtest.ui.common

import android.app.Dialog
import android.content.Context
import android.widget.TextView
import com.igniter.ffmpeg.R

/**
 * 视频抽帧结果导出弹窗
 */
class VideoExtractInfoDialog(context: Context) : Dialog(context, R.style.CommonDialog) {

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