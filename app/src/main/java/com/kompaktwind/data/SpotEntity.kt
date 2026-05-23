package com.kompaktwind.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spots")
data class SpotEntity(
    @PrimaryKey val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val isCoastal: Boolean,
    val createdAt: Long
)
