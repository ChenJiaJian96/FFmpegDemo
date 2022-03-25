//package com.igniter.ffmpegtest
//
//import android.graphics.Bitmap
//import android.media.Image
//import android.media.ImageReader
//import android.util.Log
//import com.igniter.ffmpegtest.bean.FrameResult
//import java.nio.ByteBuffer
//
//class CaptureImageAvailableListener(
//    private val callBack: FrameCallback,
//    private val scale: Int,
//    private val timeUs: Long) :
//    ImageReader.OnImageAvailableListener {
//
//    override fun onImageAvailable(reader: ImageReader) {
//        Log.i(TAG, "onImageAvailable")
//        val latestImage = reader.acquireLatestImage()
//        if (latestImage == null) {
//            Log.d(TAG, "get bitmap onFailed. timeUs: $timeUs")
//            callBack.invoke(FrameResult.GetFrameFailed(timeUs))
//            return
//        }
//        latestImage.use {
//            //这里得到的YUV的数据。需要将YUV的数据变成Bitmap
//            val planes: Array<Image.Plane> = it.planes
//            if (planes[0].buffer == null) {
//                Log.d(TAG, "get bitmap onFailed. timeUs: $timeUs")
//                callBack.invoke(FrameResult.GetFrameFailed(timeUs))
//                return
//            }
//
//            val bitmap = getBitmapScale(it, scale)
//            Log.d(TAG, "get bitmap succeed. timeUs: $timeUs")
//            callBack.invoke(FrameResult.Success(bitmap, timeUs))
//        }
//    }
//
//    private fun getBitmapScale(img: Image, scale: Int): Bitmap {
//        val width: Int = img.width / scale
//        val height: Int = img.height / scale
//        val bytesImage = getDataFromYUV420Scale(img, scale)
//        val bitmap: Bitmap = Bitmap.createBitmap(height, width, Bitmap.Config.ARGB_8888)
//        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(bytesImage))
//        return bitmap
//    }
//
//    private fun getDataFromYUV420Scale(image: Image, scale: Int): ByteArray {
//        val width: Int = image.width
//        val height: Int = image.height
//        // Read image data
//        val planes: Array<Image.Plane> = image.planes
//        val argb = ByteArray(width / scale * height / scale * 4)
//
//        // 值得注意的是在 Java 层传入 byte[] 以 RGBA 顺序排列时，libyuv 是用 ABGR 来表示这个排列
//        // libyuv 表示的排列顺序和 Bitmap 的 RGBA 表示的顺序是反向的。
//        // 所以实际要调用libyuv::ABGRToI420才能得到正确的结果。
//        YuvUtils.yuvI420ToABGRWithScale(
//            argb,
//            planes[0].buffer, planes[0].rowStride,
//            planes[1].buffer, planes[1].rowStride,
//            planes[2].buffer, planes[2].rowStride,
//            width, height,
//            scale
//        )
//        return argb
//    }
//
//    companion object {
//        private val TAG = CaptureImageAvailableListener::class.java.simpleName
//    }
//}