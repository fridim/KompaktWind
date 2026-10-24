package com.kompaktwind.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SpotDao {
    @Query("SELECT * FROM spots ORDER BY sortOrder ASC, createdAt ASC")
    fun observeAll(): Flow<List<SpotEntity>>

    @Query("SELECT * FROM spots ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun all(): List<SpotEntity>

    @Query("SELECT * FROM spots WHERE id = :id")
    suspend fun get(id: String): SpotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(spot: SpotEntity)

    @Query("UPDATE spots SET providerId = :providerId WHERE id = :id")
    suspend fun updateProvider(id: String, providerId: String)

    @Query("DELETE FROM spots WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE spots SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Long)

    @Transaction
    suspend fun move(id: String, offset: Int): Boolean {
        val ordered = all()
        val index = ordered.indexOfFirst { it.id == id }
        val target = index + offset
        if (index < 0 || target !in ordered.indices) return false
        val reordered = ordered.toMutableList().apply { add(target, removeAt(index)) }
        reordered.forEachIndexed { position, spot ->
            if (spot.sortOrder != position.toLong()) {
                updateSortOrder(spot.id, position.toLong())
            }
        }
        return true
    }
}
