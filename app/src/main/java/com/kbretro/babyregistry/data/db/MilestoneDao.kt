package com.kbretro.babyregistry.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kbretro.babyregistry.data.model.MilestoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MilestoneDao {
    @Query("SELECT * FROM milestones WHERE babyId = :babyId ORDER BY time DESC")
    fun observeAll(babyId: Long): Flow<List<MilestoneEntity>>

    @Insert
    suspend fun insert(milestone: MilestoneEntity): Long

    @Update
    suspend fun update(milestone: MilestoneEntity)

    @Delete
    suspend fun delete(milestone: MilestoneEntity)
}
