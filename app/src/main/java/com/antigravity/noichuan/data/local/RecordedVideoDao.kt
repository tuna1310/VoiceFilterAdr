package com.antigravity.noichuan.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordedVideoDao {
    @Query("SELECT * FROM recorded_videos ORDER BY createdAt DESC")
    fun getAllVideos(): Flow<List<RecordedVideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: RecordedVideoEntity): Long

    @Delete
    suspend fun deleteVideo(video: RecordedVideoEntity)

    @Query("DELETE FROM recorded_videos WHERE id = :id")
    suspend fun deleteById(id: Long)
}
