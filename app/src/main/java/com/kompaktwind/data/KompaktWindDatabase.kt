package com.kompaktwind.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SpotEntity::class, ForecastCacheEntity::class],
    version = 1,
    exportSchema = true
)
abstract class KompaktWindDatabase : RoomDatabase() {
    abstract fun spotDao(): SpotDao
    abstract fun forecastCacheDao(): ForecastCacheDao

    companion object {
        @Volatile private var instance: KompaktWindDatabase? = null

        fun getDatabase(context: Context): KompaktWindDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                KompaktWindDatabase::class.java,
                "kompaktwind.db"
            ).build().also { instance = it }
        }
    }
}
