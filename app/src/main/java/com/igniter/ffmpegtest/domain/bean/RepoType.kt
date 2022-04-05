package com.igniter.ffmpegtest.domain.bean

/**
 * Type to select the repo to extract frames
 */
sealed class RepoType {
    object MMR : RepoType()
    object MediaCodec : RepoType()
    object FFmpeg : RepoType()
}
