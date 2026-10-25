package com.kbretro.babyregistry.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kbretro.babyregistry.data.model.GrowthEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GrowthDao {
    @Query("SELECT * FROM growth WHERE babyId = :babyId ORDER BY date DESC, id DESC")
    fun observeAll(babyId: Long): Flow<List<GrowthEntity>>

    @Insert
    suspend fun insert(entry: GrowthEntity): Long

    @Update
    suspend fun update(entry: GrowthEntity)

    @Delete
    suspend fun delete(entry: GrowthEntity)
}
