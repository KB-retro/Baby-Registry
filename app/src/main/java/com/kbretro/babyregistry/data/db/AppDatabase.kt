package com.kbretro.babyregistry.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kbretro.babyregistry.data.model.AppointmentEntity
import com.kbretro.babyregistry.data.model.BabyEntity
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.GrowthEntity

@Database(
    entities = [BabyEntity::class, EventEntity::class, GrowthEntity::class, AppointmentEntity::class],
    version = 3,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun babyDao(): BabyDao
    abstract fun eventDao(): EventDao
    abstract fun growthDao(): GrowthDao
    abstract fun appointmentDao(): AppointmentDao
}
