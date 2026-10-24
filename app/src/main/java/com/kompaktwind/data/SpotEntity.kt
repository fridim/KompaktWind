package com.kompaktwind.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spots")
data class SpotEntity(
    @PrimaryKey val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val isCoastal: Boolean,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "'open-meteo'")
    val providerId: String = ProviderIds.OPEN_METEO,
    val catalogId: String? = null,
    val shortName: String? = null,
    @ColumnInfo(defaultValue = "0")
    val sortOrder: Long = 0
)
