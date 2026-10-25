package com.kbretro.babyregistry.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kbretro.babyregistry.data.model.BabyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BabyDao {
    @Query("SELECT * FROM babies ORDER BY id LIMIT 1")
    fun observeFirst(): Flow<BabyEntity?>

    @Query("SELECT * FROM babies ORDER BY id")
    fun observeAll(): Flow<List<BabyEntity>>

    @Insert
    suspend fun insert(baby: BabyEntity): Long

    @Update
    suspend fun update(baby: BabyEntity)

    @Delete
    suspend fun delete(baby: BabyEntity)
}
