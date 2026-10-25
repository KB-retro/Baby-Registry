package com.kbretro.babyregistry.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kbretro.babyregistry.data.model.AppointmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments WHERE babyId = :babyId ORDER BY time")
    fun observeAll(babyId: Long): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE id = :id")
    suspend fun getById(id: Long): AppointmentEntity?

    @Query("SELECT * FROM appointments WHERE reminderAt > :now AND done = 0")
    suspend fun pendingReminders(now: Long): List<AppointmentEntity>

    @Insert
    suspend fun insert(appointment: AppointmentEntity): Long

    @Update
    suspend fun update(appointment: AppointmentEntity)

    @Delete
    suspend fun delete(appointment: AppointmentEntity)
}
