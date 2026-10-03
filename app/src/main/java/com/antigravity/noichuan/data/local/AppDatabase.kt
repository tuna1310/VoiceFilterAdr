package com.antigravity.noichuan.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        RecordedVideoEntity::class,
        WordEntity::class,
        TongueTwisterEntity::class,
        ContentVersionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordedVideoDao(): RecordedVideoDao
    abstract fun wordDao(): WordDao
    abstract fun tongueTwisterDao(): TongueTwisterDao
    abstract fun contentVersionDao(): ContentVersionDao
}
