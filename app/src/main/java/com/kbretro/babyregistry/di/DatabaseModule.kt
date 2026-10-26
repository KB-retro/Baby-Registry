package com.kbretro.babyregistry.di

import android.content.Context
import androidx.room.Room
import com.kbretro.babyregistry.data.db.AppDatabase
import com.kbretro.babyregistry.data.db.AppointmentDao
import com.kbretro.babyregistry.data.db.BabyDao
import com.kbretro.babyregistry.data.db.EventDao
import com.kbretro.babyregistry.data.db.GrowthDao
import com.kbretro.babyregistry.data.db.MIGRATION_1_2
import com.kbretro.babyregistry.data.db.MIGRATION_2_3
import com.kbretro.babyregistry.data.db.MIGRATION_3_4
import com.kbretro.babyregistry.data.db.MIGRATION_4_5
import com.kbretro.babyregistry.data.db.MIGRATION_5_6
import com.kbretro.babyregistry.data.db.SyncDao
import com.kbretro.babyregistry.data.db.MilestoneDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "baby-registry.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .build()

    @Provides
    fun provideBabyDao(db: AppDatabase): BabyDao = db.babyDao()

    @Provides
    fun provideEventDao(db: AppDatabase): EventDao = db.eventDao()

    @Provides
    fun provideGrowthDao(db: AppDatabase): GrowthDao = db.growthDao()

    @Provides
    fun provideAppointmentDao(db: AppDatabase): AppointmentDao = db.appointmentDao()

    @Provides
    fun provideMilestoneDao(db: AppDatabase): MilestoneDao = db.milestoneDao()

    @Provides
    fun provideSyncDao(db: AppDatabase): SyncDao = db.syncDao()
}
