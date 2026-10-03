package com.antigravity.noichuan.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ContentVersionDao {

    @Query("SELECT * FROM content_versions WHERE `key` = :key LIMIT 1")
    suspend fun getVersion(key: String = "content"): ContentVersionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setVersion(entity: ContentVersionEntity)
}
