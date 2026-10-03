package com.antigravity.noichuan.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recorded_videos")
data class RecordedVideoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectKey: String,
    val subjectTitle: String,
    val difficulty: String,
    val durationSeconds: Int,
    val filePath: String,
    val thumbnailPath: String? = null,
    val wordsCompleted: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
