package com.kbretro.babyregistry.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kbretro.babyregistry.data.model.AppointmentEntity
import com.kbretro.babyregistry.data.model.BabyEntity
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.GrowthEntity
import com.kbretro.babyregistry.data.model.MilestoneEntity
import com.kbretro.babyregistry.data.model.SyncDeletionEntity

@Database(
    entities = [
        BabyEntity::class,
        EventEntity::class,
        GrowthEntity::class,
        AppointmentEntity::class,
        MilestoneEntity::class,
        SyncDeletionEntity::class,
    ],
    version = 6,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun babyDao(): BabyDao
    abstract fun eventDao(): EventDao
    abstract fun growthDao(): GrowthDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun milestoneDao(): MilestoneDao
    abstract fun syncDao(): SyncDao
}
