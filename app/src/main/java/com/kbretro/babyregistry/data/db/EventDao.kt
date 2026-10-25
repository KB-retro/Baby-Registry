package com.kbretro.babyregistry.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kbretro.babyregistry.data.model.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: EventEntity): Long

    @Update
    suspend fun update(event: EventEntity)

    @Delete
    suspend fun delete(event: EventEntity)

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getById(id: Long): EventEntity?

    @Query("SELECT * FROM events WHERE babyId = :babyId AND endTime IS NULL ORDER BY startTime")
    fun observeActive(babyId: Long): Flow<List<EventEntity>>

    /** Eventos que se solapan con [from, to), incluidos los que siguen en curso. */
    @Query(
        "SELECT * FROM events WHERE babyId = :babyId AND startTime < :to " +
            "AND (endTime IS NULL OR endTime >= :from) ORDER BY startTime DESC",
    )
    fun observeOverlapping(babyId: Long, from: Long, to: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE babyId = :babyId ORDER BY startTime DESC")
    fun observeAll(babyId: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE babyId = :babyId ORDER BY startTime DESC LIMIT :limit")
    fun observeRecent(babyId: Long, limit: Int): Flow<List<EventEntity>>

    @Query(
        "SELECT * FROM events WHERE babyId = :babyId AND type IN (:types) " +
            "ORDER BY startTime DESC LIMIT 1",
    )
    fun observeLatest(babyId: Long, types: List<String>): Flow<EventEntity?>

    @Query("SELECT * FROM events WHERE babyId = :babyId AND type IN (:types) ORDER BY startTime DESC")
    fun observeByTypes(babyId: Long, types: List<String>): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE reminderAt > :now")
    suspend fun pendingReminders(now: Long): List<EventEntity>
}
