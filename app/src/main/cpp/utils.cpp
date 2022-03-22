#include "utils.h"

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
