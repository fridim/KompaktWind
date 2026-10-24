package com.kompaktwind.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SpotEntity::class, ForecastCacheEntity::class],
    version = 4,
    exportSchema = true
)
abstract class KompaktWindDatabase : RoomDatabase() {
    abstract fun spotDao(): SpotDao
    abstract fun forecastCacheDao(): ForecastCacheDao

    companion object {
        @Volatile private var instance: KompaktWindDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE spots ADD COLUMN providerId TEXT NOT NULL DEFAULT '${ProviderIds.OPEN_METEO}'"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE spots ADD COLUMN catalogId TEXT")
                db.execSQL("ALTER TABLE spots ADD COLUMN shortName TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE spots ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE spots SET sortOrder = createdAt")
            }
        }

        fun getDatabase(context: Context): KompaktWindDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                KompaktWindDatabase::class.java,
                "kompaktwind.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
        }
    }
}
