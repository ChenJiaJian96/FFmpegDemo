#include <jni.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/log.h>

extern "C" {
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libswscale/swscale.h"
#include "libavutil/imgutils.h"
#include "android/bitmap.h"
}

// Android 打印 Log
#define LOGD(FORMAT, ...) __android_log_print(ANDROID_LOG_DEBUG, "video", FORMAT, ##__VA_ARGS__);
#define LOGE(FORMAT, ...) __android_log_print(ANDROID_LOG_ERROR, "video", FORMAT, ##__VA_ARGS__);

// 打开多媒体文件，将数据存入 AVFormatContext 结构中 avformat_open_input
int open_input_file(const char *path, AVFormatContext *&format_context) {
  int result = avformat_open_input(&format_context, path, nullptr, nullptr);
  if (result < 0) {
    LOGE("Player Error : Can not open video file")
  }
  return result;
}

// 检查输入文件中的流信息，并将视频信息填充至 format_context->streams
int
check_input_stream_info(const char *path, AVFormatContext *&format_context) {
  int result = avformat_find_stream_info(format_context, nullptr);
  if (result < 0) {
    LOGE("Invalid File: Can not find stream info.")
  }
  // 日志输出格式信息
  av_dump_format(format_context, 0, path, 0);
  return result;
}

// 获取视频流位置索引
int get_video_stream_index(const AVFormatContext *format_context) {
  int video_stream_index = -1;
  for (int i = 0; i < format_context->nb_streams; i++) {
    if (format_context->streams[i]->codecpar->codec_type
        == AVMEDIA_TYPE_VIDEO) {
      video_stream_index = i;
      break;
    }
  }
  return video_stream_index;
}

// 将 AVFrame 信息保存为 PPM 格式
void SaveFrame(AVFrame *pFrame, int width, int height, int iFrame) {
  FILE *pFile;
  char szFilename[32];
  int y;

  // Open file.
  sprintf(szFilename, "frame%d.ppm", iFrame);
  pFile = fopen(szFilename, "wb");
  if (pFile == nullptr) {
    return;
  }

  // Write header.
  fprintf(pFile, "P6\n%d %d\n255\n", width, height);

  // Write pixel data.
  for (y = 0; y < height; y++) {
    fwrite(pFrame->data[0] + y * pFrame->linesize[0], 1, width * 3, pFile);
  }

  // Close file.
  fclose(pFile);
}

// 创建 Bitmap
jobject createBitmap(JNIEnv *env, int width, int height) {
  jclass bitmapCls = env->FindClass("android/graphics/Bitmap");
  jmethodID createBitmapFunction = env->GetStaticMethodID(bitmapCls,
                                                          "createBitmap",
                                                          "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
  jstring configName = env->NewStringUTF("RGB_565");
  jclass bitmapConfigClass = env->FindClass("android/graphics/Bitmap$Config");
  jmethodID
      valueOfBitmapConfigFunction = env->GetStaticMethodID(bitmapConfigClass,
                                                           "valueOf",
                                                           "(Ljava/lang/String;)Landroid/graphics/Bitmap$Config;");

  jobject bitmapConfig = env->CallStaticObjectMethod(bitmapConfigClass,
                                                     valueOfBitmapConfigFunction,
                                                     configName);

  jobject newBitmap = env->CallStaticObjectMethod(bitmapCls,
                                                  createBitmapFunction,
                                                  width, height,
                                                  bitmapConfig);

  return newBitmap;
}

jobject convert_bitmap_by_frame(JNIEnv *env,
                                int width,
                                int height,
                                AVFrame *pFrame_rgb) {
  LOGD("start converting")
  jobject bitmap = createBitmap(env, width, height);
  void *pixels;
  int result = AndroidBitmap_lockPixels(env, bitmap, &pixels);
  if (result < 0) {
    LOGE("Error| lockPixel error")
    return nullptr;
  }

  AndroidBitmapInfo info;
  result = AndroidBitmap_getInfo(env, bitmap, &info);
  if (result < 0) {
    LOGE("Error | getInfo error")
    return nullptr;
  }

  memcpy(pixels, pFrame_rgb->data, width * height * sizeof(u_int32_t));
  AndroidBitmap_unlockPixels(env, bitmap);
  return bitmap;
}

int invoke_bitmap_callback(JNIEnv *env,
                           jobject on_bitmap_callback_listener,
                           int index,
                           int64_t timestamp,
                           jobject bitmap) {
  jclass javaClass = env->GetObjectClass(on_bitmap_callback_listener);
  if (javaClass == nullptr) {
    LOGE("Unable to find class")
  }
  jmethodID methodId = env->GetMethodID(javaClass,
                                        "onBitmapCaptured",
                                        "(IJLandroid/graphics/Bitmap;)V");
  if (methodId == nullptr) {
    LOGE("Unable to find method: onBitmapCaptured")
    return -1;
  }
  env->CallVoidMethod(on_bitmap_callback_listener,
                      methodId,
                      index,
                      timestamp,
                      bitmap);
  return 0;
}

/**
 * 播放视频流
 * R# 代表申请内存 需要释放或关闭
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_igniter_ffmpeg_VideoManager_playVideo(JNIEnv *env,
                                               jobject thiz,
                                               jstring path_,
                                               jobject surface) {
  // 记录结果
  int result;
  // R1 Java String -> C String
  const char *path = env->GetStringUTFChars(path_, nullptr);
  // R2 初始化 AVFormatContext 上下文
  AVFormatContext *format_context = avformat_alloc_context();

  // region 打开多媒体文件
  result = open_input_file(path, format_context);
  if (result < 0) {
    return;
  }
  // endregion

  // region 检查文件流信息
  result = check_input_stream_info(path, format_context);
  if (result < 0) {
    return;
  }
  // endregion

  // region 查找是否存在视频编码器，并记录 index
  int video_stream_index = get_video_stream_index(format_context);
  // 没找到视频流
  if (video_stream_index == -1) {
    LOGE("Player Error : Can not find video stream")
    return;
  }
  // endregion

  // region 初始化视频解码器上下文及解码器
  // 将 format_context 中视频流信息的 codec_parameter 传入 codec_context
  AVCodecContext *video_codec_context = avcodec_alloc_context3(nullptr);
  avcodec_parameters_to_context(video_codec_context,
                                format_context->streams[video_stream_index]->codecpar);
  // 通过 codec_id 来查找视频解码器
  AVCodec *video_decoder = avcodec_find_decoder(video_codec_context->codec_id);
  if (video_decoder == nullptr) {
    LOGE("Player Error : Can not find video decoder")
    return;
  }
  // endregion

  // region R3 打开视频解码器
  result = avcodec_open2(video_codec_context, video_decoder, nullptr);
  if (result < 0) {
    LOGE("Player Error : Can not find video stream")
    return;
  }
  // 获取视频的宽高
  int videoWidth = video_codec_context->width;
  int videoHeight = video_codec_context->height;
  // endregion

  // region R4 初始化 Native Window 用于播放视频
  ANativeWindow *native_window = ANativeWindow_fromSurface(env, surface);
  if (native_window == nullptr) {
    LOGE("Player Error : Can not create native window")
    return;
  }
  // 通过设置宽高限制缓冲区中的像素数量，而非屏幕的物理显示尺寸。
  // 如果缓冲区与物理屏幕的显示尺寸不相符，则实际显示可能会是拉伸，或者被压缩的图像
  result = ANativeWindow_setBuffersGeometry(native_window,
                                            videoWidth,
                                            videoHeight,
                                            WINDOW_FORMAT_RGBA_8888);
  if (result < 0) {
    LOGE("Player Error : Can not set native window buffer")
    ANativeWindow_release(native_window);
    return;
  }
  // endregion

  // region 开始转换数据 遍历播放
  // 定义绘图缓冲区
  ANativeWindow_Buffer window_buffer;
  // 声明数据容器 有3个
  // R5 解码前数据容器 Packet 编码数据
  AVPacket *packet = av_packet_alloc();
  // R6 解码后数据容器 Frame 像素数据 不能直接播放像素数据 还要转换
  AVFrame *frame = av_frame_alloc();
  // R7 转换后数据容器 这里面的数据可以用于播放
  AVFrame *rgba_frame = av_frame_alloc();
  // 数据格式转换准备
  // 输出 Buffer
  AVPixelFormat fmt = AV_PIX_FMT_RGBA;
  int buffer_size = av_image_get_buffer_size(fmt, videoWidth, videoHeight, 1);
  // R8 申请 Buffer 内存
  auto *out_buffer = (uint8_t *) av_malloc(buffer_size * sizeof(uint8_t));
  av_image_fill_arrays(rgba_frame->data, rgba_frame->linesize, out_buffer,
                       fmt, videoWidth, videoHeight, 1);
  // R9 数据格式转换上下文
  struct SwsContext *data_convert_context = sws_getContext(
      videoWidth, videoHeight, video_codec_context->pix_fmt,
      videoWidth, videoHeight, fmt,
      SWS_BICUBIC, nullptr, nullptr, nullptr);
  // 开始读取帧
  while (av_read_frame(format_context, packet) >= 0) {
    // 匹配视频流
    if (packet->stream_index == video_stream_index) {
      // 将 packet 作为解码器的输入，复制到 codec_context 中
      result = avcodec_send_packet(video_codec_context, packet);
      if (result < 0 && result != AVERROR(EAGAIN) && result != AVERROR_EOF) {
        LOGE("Player Error : codec step 1 fail")
        return;
      }
      // 将解码器的返回结果返回到 frame 中
      result = avcodec_receive_frame(video_codec_context, frame);
      if (result < 0 && result != AVERROR_EOF) {
        LOGE("Player Error : codec step 2 fail")
        continue;
      }
      // 返回结果为 YUV，需要转换为 RGB
      result = sws_scale(
          data_convert_context,
          (const uint8_t *const *) frame->data, frame->linesize,
          0, videoHeight,
          rgba_frame->data, rgba_frame->linesize);
      if (result < 0) {
        LOGE("Player Error : data convert fail")
        return;
      }

      // region 播放 RGB 数据
      result = ANativeWindow_lock(native_window, &window_buffer, nullptr);
      if (result < 0) {
        LOGE("Player Error : Can not lock native window")
      } else {
        // 将图像绘制到界面上
        // 注意: 这里 rgba_frame 一行的像素和 window_buffer 一行的像素长度可能不一致
        // 需要转换好 否则可能花屏
        auto *bits = (uint8_t *) window_buffer.bits;
        for (int h = 0; h < videoHeight; h++) {
          memcpy(bits + h * window_buffer.stride * 4,
                 out_buffer + h * rgba_frame->linesize[0],
                 rgba_frame->linesize[0]);
        }
        ANativeWindow_unlockAndPost(native_window);
      }
      // endregion
    }
    // 释放 packet 引用
    av_packet_unref(packet);
  }
  // endregion

  // region 释放存储空间
  // 释放 R9
  sws_freeContext(data_convert_context);
  // 释放 R8
  av_free(out_buffer);
  // 释放 R7
  av_frame_free(&rgba_frame);
  // 释放 R6
  av_frame_free(&frame);
  // 释放 R5
  av_packet_free(&packet);
  // 释放 R4
  ANativeWindow_release(native_window);
  // 关闭 R3
  avcodec_close(video_codec_context);
  // 释放 R2
  avformat_close_input(&format_context);
  // 释放 R1
  env->ReleaseStringUTFChars(path_, path);
  // endregion
}

extern "C"
JNIEXPORT void JNICALL
Java_com_igniter_ffmpeg_VideoManager_capture(JNIEnv *env,
                                             jobject thiz,
                                             jstring video_path,
                                             jint total_num,
                                             jobject on_bitmap_callback_listener) {
  int result; // 记录校验结果
  const char *path = env->GetStringUTFChars(video_path, nullptr); // 视频路径

  AVFormatContext *format_context = avformat_alloc_context();
  // region 打开文件，读取多媒体数据
  result = open_input_file(path, format_context);
  if (result < 0) {
    return;
  }
  // endregion

  // region 检查文件流信息
  result = check_input_stream_info(path, format_context);
  if (result < 0) {
    return;
  }
  // endregion

  // region 获取视频流
  int video_stream_index = get_video_stream_index(format_context);
  if (video_stream_index == -1) {
    LOGE("Invalid File: Can not find video stream info.")
    return;
  }
  // endregion

  // region 将 format_context 中视频流信息的 codec_parameter 传入 codec_context
  AVCodecContext *video_codec_context = avcodec_alloc_context3(nullptr);
  AVCodecParameters *video_codec_params =
      format_context->streams[video_stream_index]->codecpar;
  avcodec_parameters_to_context(video_codec_context, video_codec_params);
  // 通过 codec_id 来查找视频解码器
  AVCodec *video_decoder = avcodec_find_decoder(video_codec_context->codec_id);
  if (video_decoder == nullptr) {
    LOGE("Player Error : Can not find video decoder")
    return;
  }
  // endregion

  // region 打开视频解码器
  result = avcodec_open2(video_codec_context, video_decoder, nullptr);
  if (result < 0) {
    LOGE("Player Error : Can not find video stream")
    return;
  }
  // endregion

  // region 分配读取视频帧信息的存储空间
  // 分配 frame 空间存储视频帧 YUV 格式
  AVFrame *pFrame = av_frame_alloc();
  AVFrame *pFrame_rgb = av_frame_alloc();
  // 分配空间存储视频帧的原始数据
  AVPixelFormat fmt = AV_PIX_FMT_RGBA;
  int src_width = video_codec_context->width;
  int src_height = video_codec_context->height;
  int dst_width = src_width;
  int dst_height = src_height;
  int numBytes = av_image_get_buffer_size(fmt, dst_width, dst_height, 1);
  auto *buffer = (uint8_t *) av_malloc(numBytes * sizeof(uint8_t));
  av_image_fill_arrays(pFrame->data,
                       pFrame->linesize,
                       buffer, fmt,
                       dst_width, dst_height,
                       1);
  // 数据格式转换上下文
  int swsFlags = SWS_BICUBIC;
  struct SwsContext *sws_context = sws_getContext(
      src_width, src_height, video_codec_context->pix_fmt,
      dst_width, dst_height, fmt,
      swsFlags,
      nullptr, nullptr,
      nullptr);
  // endregion

  AVPacket packet;
  int64_t video_duration = format_context->duration; // 单位：AV_TIME_BASE
  LOGD("video duration %ld", video_duration)
  uint64_t interval_us = 1 * AV_TIME_BASE; // TODO: 逻辑A 间隔固定 1s
//  uint64_t interval_us = video_duration / total_num; // TODO: 逻辑B 间隔平均分配
  LOGD("video capture interval %ld", interval_us)
  for (int index = 0; index < total_num; ++index) {
    uint64_t seek_pos_us = interval_us * index;
    result = av_seek_frame(format_context, -1, seek_pos_us, AVSEEK_FLAG_BYTE);
//    result = av_seek_frame(format_context, -1, seek_pos_us, AVSEEK_FLAG_BACKWARD);
    if (result < 0) {
      LOGE("av_seek_frame() seek to %lu failed!", seek_pos_us)
    } else {
      LOGD("av_seek_frame() seek to %lu succeed!", seek_pos_us)
    }

    // region 使用 packet 开始读取视频
    // packet -> frame
    while (av_read_frame(format_context, &packet) >= 0) {
      // 匹配视频流
      if (packet.stream_index != video_stream_index) {
        av_packet_unref(&packet);
        continue;
      }

      // 将 packet 作为解码器的输入，复制到 codec_context 中
      result = avcodec_send_packet(video_codec_context, &packet);
      if (result == AVERROR(EAGAIN)) {
        // Decoder can't take packets right now. Make sure you are draining it.
        LOGE("avcodec_send_packet failed: EAGAIN")
        av_packet_unref(&packet);
        continue;
      } else if (result < 0) {
        // Failed to send the packet to the decoder
        LOGE("avcodec_send_packet failed: result = %d", result)
        av_packet_unref(&packet);
        continue;
      }
      // 将解码器的返回结果返回到 frame 中
      result = avcodec_receive_frame(video_codec_context, pFrame);
      if (result == AVERROR(EAGAIN)) {
        // The decoder doesn't have enough data to produce a frame
        // Not an error unless we reached the end of the stream
        // Just pass more packets until it has enough to produce a frame
        LOGE("avcodec_receive_frame failed: EAGAIN")
        av_packet_unref(&packet);
        av_frame_unref(pFrame);
        continue;
      } else if (result < 0) {
        // Failed to get a frame from the decoder
        LOGE("avcodec_receive_frame failed: result = %d", result)
        av_packet_unref(&packet);
        av_frame_unref(pFrame);
        continue;
      }
      int time_base_den =
          format_context->streams[video_stream_index]->time_base.den;
      auto decode_time_us = pFrame->pts * AV_TIME_BASE / time_base_den;
      LOGD(
          "decode frame finished. pFrame->decode_time_ms: %ld, seek_pos_us: %ld, time_base_den: %d",
          decode_time_us,
          seek_pos_us,
          time_base_den)
      if (decode_time_us >= seek_pos_us) break;

      av_packet_unref(&packet);
    }
    // endregion

    LOGD("开始转换视频帧数据到图像帧数据")
    sws_scale(sws_context,
              pFrame->data, pFrame->linesize,
              0, src_height,
              pFrame_rgb->data, pFrame_rgb->linesize);

    // region 生成 bitmap 并回调 bitmap 对象
    // 图像数据已经保存至 frame_rgb 中，用于生成 bitmap 数据
    jobject bitmap =
        convert_bitmap_by_frame(env, dst_width, dst_height, pFrame_rgb);
    if (bitmap == nullptr) {
      LOGE("convert to bitmap failed.")
    }

    // 回调 Bitmap 对象
    result = invoke_bitmap_callback(env,
                                    on_bitmap_callback_listener,
                                    index,
                                    seek_pos_us / 1000,
                                    bitmap);
    if (result == 0) {
      LOGD("invoke bitmap callback success. index: %d", index)
    } else {
      LOGE("invoke bitmap callback failed. index: %d", index)
    }
    // endregion
  }

  // region 释放内存
  av_free(buffer);
  av_packet_unref(&packet);
  av_frame_free(&pFrame);
  av_frame_free(&pFrame_rgb);
  sws_freeContext(sws_context);
  avcodec_close(video_codec_context);
  avcodec_parameters_free(&video_codec_params);
  avformat_close_input(&format_context);
  // endregion
}