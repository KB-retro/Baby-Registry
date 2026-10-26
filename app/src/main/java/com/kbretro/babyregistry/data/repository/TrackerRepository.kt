package com.kbretro.babyregistry.data.repository

import com.kbretro.babyregistry.data.db.AppointmentDao
import com.kbretro.babyregistry.data.db.BabyDao
import com.kbretro.babyregistry.data.db.EventDao
import com.kbretro.babyregistry.data.db.GrowthDao
import com.kbretro.babyregistry.data.db.MilestoneDao
import com.kbretro.babyregistry.data.model.AppointmentEntity
import com.kbretro.babyregistry.data.model.BabyEntity
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.data.model.GrowthEntity
import com.kbretro.babyregistry.data.model.MilestoneEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackerRepository @Inject constructor(
    private val babyDao: BabyDao,
    private val eventDao: EventDao,
    private val growthDao: GrowthDao,
    private val appointmentDao: AppointmentDao,
    private val milestoneDao: MilestoneDao,
    private val selectedBaby: SelectedBabyStore,
) {
    val babies: Flow<List<BabyEntity>> = babyDao.observeAll()

    /** Bebé seleccionado; si no hay selección válida, el primero. */
    val currentBaby: Flow<BabyEntity?> = combine(babies, selectedBaby.selectedId) { list, id ->
        list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }.distinctUntilChanged()

    suspend fun currentBabyId(): Long? = currentBaby.first()?.id

    suspend fun addBaby(baby: BabyEntity): Long = babyDao.insert(baby).also { selectedBaby.select(it) }

    suspend fun updateBaby(baby: BabyEntity) = babyDao.update(baby)

    suspend fun deleteBaby(baby: BabyEntity) = babyDao.delete(baby)

    suspend fun selectBaby(id: Long) = selectedBaby.select(id)

    // Eventos

    fun active(babyId: Long): Flow<List<EventEntity>> = eventDao.observeActive(babyId)

    /** Temporizadores en curso de todos los bebés (para la notificación). */
    fun allActive(): Flow<List<EventEntity>> = eventDao.observeAllActive()

    fun overlapping(babyId: Long, from: Long, to: Long): Flow<List<EventEntity>> =
        eventDao.observeOverlapping(babyId, from, to)

    fun all(babyId: Long): Flow<List<EventEntity>> = eventDao.observeAll(babyId)

    fun recent(babyId: Long, limit: Int): Flow<List<EventEntity>> = eventDao.observeRecent(babyId, limit)

    fun latest(babyId: Long, vararg types: EventType): Flow<EventEntity?> =
        eventDao.observeLatest(babyId, types.map { it.name })

    fun byTypes(babyId: Long, types: List<EventType>): Flow<List<EventEntity>> =
        eventDao.observeByTypes(babyId, types.map { it.name })

    suspend fun getEvent(id: Long): EventEntity? = eventDao.getById(id)

    suspend fun insert(event: EventEntity): Long = eventDao.insert(event)

    suspend fun insertAll(events: List<EventEntity>) = eventDao.insertAll(events)

    suspend fun update(event: EventEntity) = eventDao.update(event)

    suspend fun delete(event: EventEntity) = eventDao.delete(event)

    suspend fun pendingEventReminders(now: Long): List<EventEntity> = eventDao.pendingReminders(now)

    // Crecimiento

    fun growth(babyId: Long): Flow<List<GrowthEntity>> = growthDao.observeAll(babyId)

    suspend fun saveGrowth(entry: GrowthEntity) {
        if (entry.id == 0L) growthDao.insert(entry) else growthDao.update(entry)
    }

    suspend fun deleteGrowth(entry: GrowthEntity) = growthDao.delete(entry)

    // Hitos

    fun milestones(babyId: Long): Flow<List<MilestoneEntity>> = milestoneDao.observeAll(babyId)

    suspend fun saveMilestone(milestone: MilestoneEntity) {
        if (milestone.id == 0L) milestoneDao.insert(milestone) else milestoneDao.update(milestone)
    }

    suspend fun deleteMilestone(milestone: MilestoneEntity) = milestoneDao.delete(milestone)

    // Citas

    fun appointments(babyId: Long): Flow<List<AppointmentEntity>> = appointmentDao.observeAll(babyId)

    suspend fun getAppointment(id: Long): AppointmentEntity? = appointmentDao.getById(id)

    suspend fun insertAppointment(appointment: AppointmentEntity): Long = appointmentDao.insert(appointment)

    suspend fun updateAppointment(appointment: AppointmentEntity) = appointmentDao.update(appointment)

    suspend fun deleteAppointment(appointment: AppointmentEntity) = appointmentDao.delete(appointment)

    suspend fun pendingAppointmentReminders(now: Long): List<AppointmentEntity> =
        appointmentDao.pendingReminders(now)
}
