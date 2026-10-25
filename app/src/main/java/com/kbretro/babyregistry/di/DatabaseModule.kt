package com.kbretro.babyregistry.di

import android.content.Context
import androidx.room.Room
import com.kbretro.babyregistry.data.db.AppDatabase
import com.kbretro.babyregistry.data.db.BabyDao
import com.kbretro.babyregistry.data.db.EventDao
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
        Room.databaseBuilder(context, AppDatabase::class.java, "baby-registry.db").build()

    @Provides
    fun provideBabyDao(db: AppDatabase): BabyDao = db.babyDao()

    @Provides
    fun provideEventDao(db: AppDatabase): EventDao = db.eventDao()
}
