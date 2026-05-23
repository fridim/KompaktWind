package com.kompaktwind.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ForecastCacheDao {
    @Query("SELECT * FROM forecast_cache WHERE spotId = :spotId AND providerId = :providerId")
    suspend fun get(spotId: String, providerId: String): ForecastCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ForecastCacheEntity)
}
