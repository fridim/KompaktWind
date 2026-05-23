package com.kompaktwind.data

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "forecast_cache",
    primaryKeys = ["spotId", "providerId"],
    foreignKeys = [ForeignKey(
        entity = SpotEntity::class,
        parentColumns = ["id"],
        childColumns = ["spotId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ForecastCacheEntity(
    val spotId: String,
    val providerId: String,
    val fetchedAt: Long,
    val timezone: String,
    val hoursJson: String
)
