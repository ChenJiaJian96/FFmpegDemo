extern "C" {
#include <android/log.h>
#include <libavformat/avformat.h>
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libswscale/swscale.h"
#include "libavutil/imgutils.h"
#include "android/bitmap.h"
#include <android/native_window.h>
#include <android/native_window_jni.h>
}

#define LOGD(FORMAT, ...) __android_log_print(ANDROID_LOG_DEBUG, "ffmpeg", FORMAT, ##__VA_ARGS__);
#define LOGE(FORMAT, ...) __android_log_print(ANDROID_LOG_ERROR, "ffmpeg", FORMAT, ##__VA_ARGS__);

int open_input_file(const char *path, AVFormatContext *&format_context);

int check_input_stream_info(const char *path, AVFormatContext *&format_context);

int get_video_stream_index(const AVFormatContext *format_context);
