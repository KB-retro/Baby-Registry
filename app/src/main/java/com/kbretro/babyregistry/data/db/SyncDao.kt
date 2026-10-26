package com.kbretro.babyregistry.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kbretro.babyregistry.data.model.AppointmentEntity
import com.kbretro.babyregistry.data.model.BabyEntity
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.GrowthEntity
import com.kbretro.babyregistry.data.model.MilestoneEntity
import com.kbretro.babyregistry.data.model.SyncDeletionEntity

/** Consultas para la sincronización: cambios desde una fecha y búsqueda por uuid. */
@Dao
interface SyncDao {
    @Insert
    suspend fun logDeletion(deletion: SyncDeletionEntity)

    @Query("SELECT * FROM sync_deletions WHERE deletedAt > :since")
    suspend fun deletionsSince(since: Long): List<SyncDeletionEntity>

    @Query("SELECT * FROM babies WHERE updatedAt > :since")
    suspend fun babiesSince(since: Long): List<BabyEntity>

    @Query("SELECT * FROM events WHERE updatedAt > :since")
    suspend fun eventsSince(since: Long): List<EventEntity>

    @Query("SELECT * FROM growth WHERE updatedAt > :since")
    suspend fun growthSince(since: Long): List<GrowthEntity>

    @Query("SELECT * FROM appointments WHERE updatedAt > :since")
    suspend fun appointmentsSince(since: Long): List<AppointmentEntity>

    @Query("SELECT * FROM milestones WHERE updatedAt > :since")
    suspend fun milestonesSince(since: Long): List<MilestoneEntity>

    @Query("SELECT * FROM babies")
    suspend fun allBabies(): List<BabyEntity>

    @Query("SELECT * FROM babies WHERE uuid = :uuid")
    suspend fun babyByUuid(uuid: String): BabyEntity?

    @Query("SELECT * FROM events WHERE uuid = :uuid")
    suspend fun eventByUuid(uuid: String): EventEntity?

    @Query("SELECT * FROM growth WHERE uuid = :uuid")
    suspend fun growthByUuid(uuid: String): GrowthEntity?

    @Query("SELECT * FROM appointments WHERE uuid = :uuid")
    suspend fun appointmentByUuid(uuid: String): AppointmentEntity?

    @Query("SELECT * FROM milestones WHERE uuid = :uuid")
    suspend fun milestoneByUuid(uuid: String): MilestoneEntity?

    @Query("DELETE FROM babies WHERE uuid = :uuid AND updatedAt <= :deletedAt")
    suspend fun deleteBaby(uuid: String, deletedAt: Long)

    @Query("DELETE FROM events WHERE uuid = :uuid AND updatedAt <= :deletedAt")
    suspend fun deleteEvent(uuid: String, deletedAt: Long)

    @Query("DELETE FROM growth WHERE uuid = :uuid AND updatedAt <= :deletedAt")
    suspend fun deleteGrowth(uuid: String, deletedAt: Long)

    @Query("DELETE FROM appointments WHERE uuid = :uuid AND updatedAt <= :deletedAt")
    suspend fun deleteAppointment(uuid: String, deletedAt: Long)

    @Query("DELETE FROM milestones WHERE uuid = :uuid AND updatedAt <= :deletedAt")
    suspend fun deleteMilestone(uuid: String, deletedAt: Long)
}
