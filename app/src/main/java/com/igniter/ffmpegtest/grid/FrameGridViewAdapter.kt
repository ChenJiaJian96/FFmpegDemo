package com.igniter.ffmpegtest.grid

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.igniter.ffmpeg.R

import com.igniter.ffmpegtest.bean.FrameInfo

class FrameGridViewAdapter(private val context: Context, private val num: Int) : BaseAdapter() {

    private val frameInfoList: Array<FrameInfo?> = arrayOfNulls(num)

    inner class ViewHolder(contentView: View) {
        private val info: TextView = contentView.findViewById(R.id.image_info)
        private val view: ImageView = contentView.findViewById(R.id.image_view)

        fun bindBitmap(frameInfo: FrameInfo) {
            with(frameInfo) {
                info.text = context.getString(R.string.app_frame_info, index, timestamp)
                view.setImageBitmap(bitmap)
            }
        }
    }

    fun onBitmapUpdated(frameInfo: FrameInfo) {
        frameInfoList[frameInfo.index] = frameInfo
        notifyDataSetChanged()
    }

    override fun getCount(): Int = num

    override fun getItem(position: Int): FrameInfo? = frameInfoList[position]

    override fun getItemId(position: Int): Long = 0

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val frameInfo = getItem(position)

        val contentView = if (convertView == null) {
            val layoutInflater = LayoutInflater.from(context)
            val rootView = layoutInflater.inflate(R.layout.item_frame_layout, parent, false)
            val viewHolder = ViewHolder(rootView)
            rootView.tag = viewHolder
            rootView
        } else {
            convertView
        }

        val viewHolder = contentView.tag as ViewHolder
        frameInfo?.let {
            viewHolder.bindBitmap(it)
        }

        return contentView
    }
}