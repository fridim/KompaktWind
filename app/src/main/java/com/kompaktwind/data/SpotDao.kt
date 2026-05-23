package com.kompaktwind.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpotDao {
    @Query("SELECT * FROM spots ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<SpotEntity>>

    @Query("SELECT * FROM spots WHERE id = :id")
    suspend fun get(id: String): SpotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(spot: SpotEntity)

    @Query("DELETE FROM spots WHERE id = :id")
    suspend fun delete(id: String)
}
