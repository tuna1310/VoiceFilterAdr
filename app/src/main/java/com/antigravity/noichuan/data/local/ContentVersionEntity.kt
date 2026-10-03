package com.antigravity.noichuan.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "content_versions")
data class ContentVersionEntity(
    @PrimaryKey val key: String = "content",
    val version: Int,
    val updatedAt: Long = System.currentTimeMillis()
)
