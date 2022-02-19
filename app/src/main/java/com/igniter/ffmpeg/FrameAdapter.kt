package com.igniter.ffmpeg

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FrameAdapter(num: Int) : RecyclerView.Adapter<FrameAdapter.FrameHolder>() {

    private val bitmapList: Array<FrameInfo?> = arrayOfNulls(num)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FrameHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_frame_layout, parent, false) as ViewGroup
        return FrameHolder(view)
    }

    override fun onBindViewHolder(holder: FrameHolder, position: Int) {
        bitmapList[position]?.apply {
            holder.bindBitmap(this)
        }
    }

    override fun getItemCount(): Int = bitmapList.size

    fun onBitmapUpdated(frameInfo: FrameInfo) {
        bitmapList[frameInfo.index] = frameInfo
        notifyItemChanged(frameInfo.index)
    }

    inner class FrameHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val info: TextView = itemView.findViewById(R.id.image_info)
        private val view: ImageView = itemView.findViewById(R.id.image_view)

        fun bindBitmap(frameInfo: FrameInfo) {
            with(frameInfo) {
                info.text = "index: $index, timestamp: $timestamp ms, timeCost: $timeCost ms"
                view.setImageBitmap(bitmap)
            }
        }
    }
}