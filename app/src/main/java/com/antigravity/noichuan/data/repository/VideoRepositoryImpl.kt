package com.antigravity.noichuan.data.repository

import com.antigravity.noichuan.data.local.RecordedVideoDao
import com.antigravity.noichuan.data.local.RecordedVideoEntity
import com.antigravity.noichuan.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import java.io.File

class VideoRepositoryImpl(
    private val videoDao: RecordedVideoDao
) : VideoRepository {
    override fun getAllVideos(): Flow<List<RecordedVideoEntity>> {
        return videoDao.getAllVideos()
    }

    override suspend fun saveVideo(video: RecordedVideoEntity): Long {
        return videoDao.insertVideo(video)
    }

    override suspend fun deleteVideo(video: RecordedVideoEntity) {
        // Xóa file vật lý nếu còn tồn tại
        try {
            val file = File(video.filePath)
            if (file.exists()) {
                file.delete()
            }
            video.thumbnailPath?.let {
                val thumb = File(it)
                if (thumb.exists()) thumb.delete()
            }
        } catch (_: Exception) {}
        videoDao.deleteVideo(video)
    }

    override suspend fun deleteVideoById(id: Long) {
        videoDao.deleteById(id)
    }
}
