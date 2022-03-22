# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# 设置CMake的最小版本号，用于构建 Native 库
cmake_minimum_required(VERSION 3.4.1)

# 设置生成的动态库目录
# 生成的 so 在当前项目不需要手动拷贝到 libs 或 jniLibs，生成apk会自动引入，这里生成的可以供其他项目使用
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/libs/${ANDROID_ABI})
# 设置 jni 库目录
set(JNI_LIBS_DIR ${CMAKE_SOURCE_DIR}/../../../libs)

add_library(video SHARED capture_frame.cpp play_video.cpp utils.cpp)

# 查找系统库
find_library(log-lib log)
find_library(android-lib android)

add_library(avcodec SHARED IMPORTED)
set_target_properties(
        avcodec
        PROPERTIES IMPORTED_LOCATION
        ${JNI_LIBS_DIR}/${ANDROID_ABI}/libavcodec.so)

add_library(avfilter SHARED IMPORTED)
set_target_properties(
        avfilter
        PROPERTIES IMPORTED_LOCATION
        ${JNI_LIBS_DIR}/${ANDROID_ABI}/libavfilter.so)

add_library(avformat SHARED IMPORTED)
set_target_properties(
        avformat
        PROPERTIES IMPORTED_LOCATION
        ${JNI_LIBS_DIR}/${ANDROID_ABI}/libavformat.so)

add_library(avutil SHARED IMPORTED)
set_target_properties(
        avutil
        PROPERTIES IMPORTED_LOCATION
        ${JNI_LIBS_DIR}/${ANDROID_ABI}/libavutil.so)

add_library(swresample SHARED IMPORTED)
set_target_properties(
        swresample
        PROPERTIES IMPORTED_LOCATION
        ${JNI_LIBS_DIR}/${ANDROID_ABI}/libswresample.so)

add_library(swscale SHARED IMPORTED)
set_target_properties(
        swscale
        PROPERTIES IMPORTED_LOCATION
        ${JNI_LIBS_DIR}/${ANDROID_ABI}/libswscale.so)

add_library(libyuv SHARED IMPORTED)
set_target_properties(
        libyuv
        PROPERTIES IMPORTED_LOCATION
        ${JNI_LIBS_DIR}/${ANDROID_ABI}/libyuv.so)

# 指定头文件路径
include_directories(${CMAKE_SOURCE_DIR}/include)

# 链接目标库，把依赖库都链接到目标库中
target_link_libraries(
        video
        avcodec
        avfilter
        avformat
        avutil
        swresample
        swscale
        libyuv
        android
        jnigraphics
        ${log-lib})