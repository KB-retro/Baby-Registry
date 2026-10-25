package com.kbretro.babyregistry.data.repository

import com.kbretro.babyregistry.data.db.BabyDao
import com.kbretro.babyregistry.data.db.EventDao
import com.kbretro.babyregistry.data.model.BabyEntity
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackerRepository @Inject constructor(
    private val babyDao: BabyDao,
    private val eventDao: EventDao,
) {
    val currentBaby: Flow<BabyEntity?> = babyDao.observeFirst()

    suspend fun addBaby(baby: BabyEntity): Long = babyDao.insert(baby)

    fun active(babyId: Long): Flow<List<EventEntity>> = eventDao.observeActive(babyId)

    fun overlapping(babyId: Long, from: Long, to: Long): Flow<List<EventEntity>> =
        eventDao.observeOverlapping(babyId, from, to)

    fun all(babyId: Long): Flow<List<EventEntity>> = eventDao.observeAll(babyId)

    fun recent(babyId: Long, limit: Int): Flow<List<EventEntity>> = eventDao.observeRecent(babyId, limit)

    fun latest(babyId: Long, vararg types: EventType): Flow<EventEntity?> =
        eventDao.observeLatest(babyId, types.map { it.name })

    suspend fun getEvent(id: Long): EventEntity? = eventDao.getById(id)

    suspend fun insert(event: EventEntity): Long = eventDao.insert(event)

    suspend fun update(event: EventEntity) = eventDao.update(event)

    suspend fun delete(event: EventEntity) = eventDao.delete(event)
}
