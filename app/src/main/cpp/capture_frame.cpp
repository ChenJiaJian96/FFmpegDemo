#include <jni.h>
#include "utils.h"

// 创建 Bitmap
jobject create_bitmap(JNIEnv *env, int width, int height) {
  jclass bitmapCls = env->FindClass("android/graphics/Bitmap");
  jmethodID createBitmapFunction = env->GetStaticMethodID(bitmapCls,
                                                          "createBitmap",
                                                          "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
  jstring configName = env->NewStringUTF("ARGB_8888");
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
                                const uint8_t *pFrame_rgb) {
  LOGD("start converting")
  jobject bitmap = create_bitmap(env, width, height);
  void *pixels;
  int result = AndroidBitmap_lockPixels(env, bitmap, &pixels);
  if (result < 0) {
    LOGE("Error| lockPixel error")
    return nullptr;
  }

  AndroidBitmapInfo bitmap_info;
  result = AndroidBitmap_getInfo(env, bitmap, &bitmap_info);
  if (result < 0) {
    LOGE("Error | getInfo error")
    return nullptr;
  }

  memcpy(pixels, pFrame_rgb, width * height * sizeof(u_int32_t));
  AndroidBitmap_unlockPixels(env, bitmap);
  return bitmap;
}

int get_seek_flag(jint seek_flag_index) {
  if (seek_flag_index == 0) {
    return AVSEEK_FLAG_BACKWARD;
  } else {
    return AVSEEK_FLAG_BYTE;
  }
}

int invoke_video_info_callback(
    JNIEnv *env,
    jobject on_bitmap_callback_listener,
    int width, int height, int64_t duration_ms
) {
  jclass javaClass = env->GetObjectClass(on_bitmap_callback_listener);
  if (javaClass == nullptr) {
    LOGE("Unable to find class")
  }
  jmethodID method_id = env->GetMethodID(javaClass,
                                         "onVideoInfoRetrieved",
                                         "(IIJ)V");
  if (method_id == nullptr) {
    LOGE("Unable to find method: onVideoInfoRetrieved")
    return -1;
  }
  env->CallVoidMethod(on_bitmap_callback_listener,
                      method_id,
                      width,
                      height,
                      duration_ms);
  return 0;
}

void invoke_step_passed(JNIEnv *env,
                        jobject on_bitmap_callback_listener,
                        int index,
                        int step) {
  jclass javaClass = env->GetObjectClass(on_bitmap_callback_listener);
  if (javaClass == nullptr) {
    LOGE("Unable to find class")
  }
  jmethodID method_id = env->GetMethodID(javaClass,
                                         "onStepPassed",
                                         "(II)V");
  if (method_id == nullptr) {
    LOGE("Unable to find method: onStepPassed")
    return;
  }
  env->CallVoidMethod(on_bitmap_callback_listener,
                      method_id,
                      index,
                      step);
}

int invoke_bitmap_callback(JNIEnv *env,
                           jobject capture_frame_listener,
                           int index,
                           int64_t timestamp_ms,
                           jobject bitmap) {
  jclass javaClass = env->GetObjectClass(capture_frame_listener);
  if (javaClass == nullptr) {
    LOGE("Unable to find class")
  }
  jmethodID method_id = env->GetMethodID(javaClass,
                                         "onBitmapCaptured",
                                         "(IJLandroid/graphics/Bitmap;)V");
  if (method_id == nullptr) {
    LOGE("Unable to find method: onBitmapCaptured")
    return -1;
  }
  env->CallVoidMethod(capture_frame_listener,
                      method_id,
                      index,
                      timestamp_ms,
                      bitmap);
  return 0;
}

/**
 * seek 到指定的时间位置
 * @param format_context 视频格式上下文
 * @param video_stream_index 视频流位置索引
 * @param seek_flag seek 策略标志
 * @param seek_pos_us 当前 seek 时间位置
 * @param last_seek_pos_us 上次 seek 时间位置
 * @param enable_optimize 是否判断 GOP 跳过解码
 */
void seek_to_target_pos(AVFormatContext *format_context,
                        int video_stream_index,
                        int seek_flag,
                        uint64_t seek_pos_us,
                        uint64_t last_seek_pos_us,
                        bool enable_optimize) {
  if (seek_pos_us == 0 || seek_pos_us == last_seek_pos_us) {
    LOGD("av_seek_frame() passed. current_seek_pos: %ld, last_seek_pos: %ld",
         seek_pos_us, last_seek_pos_us)
    return;
  }

  if (enable_optimize) {
    // 通过获取的 I 帧 DTS 值比较是否在同一个 GOP 中；
    AVStream *video_stream = format_context->streams[video_stream_index];
    int time_base_den = video_stream->time_base.den;
    int cur_iframe_index = av_index_search_timestamp(
        format_context->streams[video_stream_index],
        seek_pos_us * time_base_den / AV_TIME_BASE,
        seek_flag);
    int pre_iframe_index = av_index_search_timestamp(
        format_context->streams[video_stream_index],
        last_seek_pos_us * time_base_den / AV_TIME_BASE,
        seek_flag);
    if (cur_iframe_index == pre_iframe_index) {
      LOGD("av_seek_frame() passed. cur_iframe_index: %d, pre_iframe_index: %d",
           cur_iframe_index, pre_iframe_index)
      return;
    }
  }

  int result = av_seek_frame(format_context, -1, seek_pos_us, seek_flag);
  if (result < 0) {
    LOGE("av_seek_frame() seek to %lu failed!", seek_pos_us)
  } else {
    LOGD("av_seek_frame() seek to %lu succeed!", seek_pos_us)
  }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_igniter_ffmpegtest_data_data_1source_FFmpegSolution_capture(JNIEnv *env,
                                                                     jobject thiz,
                                                                     jstring video_path,
                                                                     jint start_time_in_s,
                                                                     jint start_index,
                                                                     jint total_num,
                                                                     jboolean enable_multi_thread,
                                                                     jint strategy_index,
                                                                     jint seek_flag_index,
                                                                     jobject capture_frame_listener) {
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
  if (enable_multi_thread) {
    video_codec_context->
        thread_count = 8;
  }
  AVCodecParameters *video_codec_params =
      format_context->streams[video_stream_index]->codecpar;
  avcodec_parameters_to_context(video_codec_context, video_codec_params
  );
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
  int64_t video_duration = format_context->duration;   // 单位：AV_TIME_BASE
  invoke_video_info_callback(env,
                             capture_frame_listener,
                             src_width,
                             src_height,
                             video_duration
                                 * 1000 / AV_TIME_BASE);
  int dst_width = src_width;
  int dst_height = src_height;
  int numBytes = av_image_get_buffer_size(fmt, dst_width, dst_height, 1);
  auto *buffer = (uint8_t *) av_malloc(numBytes * sizeof(uint8_t));
  av_image_fill_arrays(pFrame_rgb
                           ->data,
                       pFrame_rgb->linesize,
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

  uint64_t interval_us = 1 * AV_TIME_BASE; // TODO: 逻辑A 间隔固定 1s
//  uint64_t interval_us = video_duration / total_num; // TODO: 逻辑B 间隔平均分配
  LOGD("video capture interval %ld", interval_us)

  AVPacket packet;
  int time_base_den =
      format_context->streams[video_stream_index]->time_base.den;
  int seek_flag = get_seek_flag(seek_flag_index);
  invoke_step_passed(env, capture_frame_listener, 0, 0);

  for (int index = 0; index < total_num; ++index) {
    int64_t seek_pos_us = start_time_in_s * AV_TIME_BASE + interval_us * index;
    int64_t last_seek_pos_us = 0;
    if (index > 0) {
      last_seek_pos_us =
          start_time_in_s * AV_TIME_BASE + interval_us * (index - 1);
    }

    seek_to_target_pos(format_context,
                       video_stream_index,
                       seek_flag,
                       seek_pos_us,
                       last_seek_pos_us,
                       strategy_index
                           == 1);

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
      if (result < 0) {
        // 当前解码器无法接收 packet
        LOGE("avcodec_send_packet failed: EAGAIN || result == %d", result)
        av_packet_unref(&packet);
        continue;
      }
      // 将解码器的返回结果返回到 frame 中
      result = avcodec_receive_frame(video_codec_context, pFrame);
      if (result < 0) {
        // 当前解码器数据无法解析出 frame
        LOGE("avcodec_receive_frame failed: EAGAIN || result == %d", result)
        av_packet_unref(&packet);
        av_frame_unref(pFrame);
        continue;
      }

      auto decode_time_us = pFrame->pts * AV_TIME_BASE / time_base_den;
      if (decode_time_us >= seek_pos_us) break;

      av_packet_unref(&packet);
    }
    // endregion
    // decode finished
    invoke_step_passed(env, capture_frame_listener, start_index + index + 1, 1);

    LOGD("开始转换视频帧数据到图像帧数据")
    sws_scale(sws_context,
              pFrame
                  ->data, pFrame->linesize,
              0, src_height,
              pFrame_rgb->data, pFrame_rgb->linesize);

    // region 生成 bitmap 并回调 bitmap 对象
    // 图像数据已经保存至 frame_rgb 中，用于生成 bitmap 数据
    jobject
        bitmap = convert_bitmap_by_frame(env, dst_width, dst_height, buffer);
    if (bitmap == nullptr) {
      LOGE("convert to bitmap failed.")
    }

    // 回调 Bitmap 对象
    result = invoke_bitmap_callback(env,
                                    capture_frame_listener,
                                    index,
                                    seek_pos_us * 1000 / AV_TIME_BASE,
                                    bitmap);
    if (result == 0) {
      LOGD("invoke bitmap callback success. index: %d", index)
    } else {
      LOGE("invoke bitmap callback failed. index: %d", index)
    }
    // output finished
    invoke_step_passed(env, capture_frame_listener, index
        + 1, 2);
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
  // avformat_close_input(&format_context);
  // endregion
}