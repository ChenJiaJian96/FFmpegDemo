package com.igniter.ffmpegtest.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.igniter.ffmpeg.R
import com.igniter.ffmpegtest.domain.bean.FrameInfo

class FrameListAdapter(private val context: Context, private val num: Int) :
    RecyclerView.Adapter<FrameListAdapter.ViewHolder>() {

    private val frameInfoList: Array<FrameInfo?> = arrayOfNulls(num)

    inner class ViewHolder(contentView: View) : RecyclerView.ViewHolder(contentView) {
        private val view: ImageView = contentView.findViewById(R.id.image_view)

        fun bindBitmap(frameInfo: FrameInfo) {
            with(frameInfo) {
                view.setImageBitmap(bitmap)
                view.setOnClickListener {
                    Toast.makeText(
                        context,
                        context.getString(R.string.app_frame_info, index, timestamp),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun onBitmapUpdated(frameInfo: FrameInfo) {
        frameInfoList[frameInfo.index] = frameInfo
        notifyItemChanged(frameInfo.index)
    }

    override fun getItemId(position: Int): Long = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val rootView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_frame_layout, parent, false)
        return ViewHolder(rootView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val frameInfo = frameInfoList[position]
        frameInfo?.let {
            holder.bindBitmap(it)
        }
    }

    override fun getItemCount(): Int = num
}