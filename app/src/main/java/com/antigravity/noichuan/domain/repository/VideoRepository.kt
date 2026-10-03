package com.antigravity.noichuan.domain.repository

import com.antigravity.noichuan.data.local.RecordedVideoEntity
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    fun getAllVideos(): Flow<List<RecordedVideoEntity>>
    suspend fun saveVideo(video: RecordedVideoEntity): Long
    suspend fun deleteVideo(video: RecordedVideoEntity)
    suspend fun deleteVideoById(id: Long)
}
