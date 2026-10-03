package com.antigravity.noichuan.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TongueTwisterDao {

    @Query("SELECT * FROM tongue_twisters")
    suspend fun getAll(): List<TongueTwisterEntity>

    @Query("SELECT * FROM tongue_twisters WHERE LOWER(level) = LOWER(:level)")
    suspend fun getByLevel(level: String): List<TongueTwisterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<TongueTwisterEntity>)

    @Query("SELECT COUNT(*) FROM tongue_twisters")
    suspend fun getCount(): Int
}
