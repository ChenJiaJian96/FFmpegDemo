#include <jni.h>
#include "utils.h"

/**
 * 播放视频流
 * R# 代表申请内存 需要释放或关闭
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_igniter_ffmpegtest_data_data_1source_FFmpegSolution_playVideo(JNIEnv *env,
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
                                format_context
                                    ->streams[video_stream_index]->codecpar);
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
  av_image_fill_arrays(rgba_frame
                           ->data, rgba_frame->linesize, out_buffer,
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
      if (result < 0 && result !=
          AVERROR(EAGAIN)
          && result != AVERROR_EOF) {
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
        for (
            int h = 0;
            h < videoHeight;
            h++) {
          memcpy(bits
                     +
                         h * window_buffer
                             .stride * 4,
                 out_buffer +
                     h * rgba_frame
                         ->linesize[0],
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