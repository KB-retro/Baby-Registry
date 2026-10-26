package com.kbretro.babyregistry.data.sync

import androidx.room.withTransaction
import com.kbretro.babyregistry.data.db.AppDatabase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parte local de la sincronización: reúne lo que ha cambiado en este móvil y aplica lo que llega de
 * los demás. En un conflicto gana la modificación más reciente (`updatedAt`).
 */
@Singleton
class SyncRepository @Inject constructor(private val db: AppDatabase) {
    private val dao = db.syncDao()

    suspend fun localChanges(since: Long): ChangeSet {
        val babyUuids = dao.allBabies().associate { it.id to it.uuid }
        fun <T> wrap(items: List<T>, babyId: (T) -> Long) =
            items.mapNotNull { item -> babyUuids[babyId(item)]?.let { Synced(it, item) } }
        return ChangeSet(
            babies = dao.babiesSince(since),
            events = wrap(dao.eventsSince(since)) { it.babyId },
            growth = wrap(dao.growthSince(since)) { it.babyId },
            appointments = wrap(dao.appointmentsSince(since)) { it.babyId },
            milestones = wrap(dao.milestonesSince(since)) { it.babyId },
            deletions = dao.deletionsSince(since),
        )
    }

    /**
     * Aplica cambios remotos sin tocar `updatedAt` (para no reenviarlos) ni registrar borrados.
     * Las fotos no viajan aquí: se conserva la foto local, si la hay.
     */
    suspend fun applyRemote(changes: ChangeSet) = db.withTransaction {
        changes.babies.forEach { remote ->
            val local = dao.babyByUuid(remote.uuid)
            when {
                local == null -> db.babyDao().insert(remote.copy(id = 0, photoPath = null))
                remote.updatedAt > local.updatedAt -> db.babyDao().update(remote.copy(id = local.id, photoPath = local.photoPath))
            }
        }
        suspend fun babyId(uuid: String) = dao.babyByUuid(uuid)?.id

        changes.events.forEach { (babyUuid, remote) ->
            val babyId = babyId(babyUuid) ?: return@forEach
            val local = dao.eventByUuid(remote.uuid)
            when {
                local == null -> db.eventDao().insert(remote.copy(id = 0, babyId = babyId))
                remote.updatedAt > local.updatedAt -> db.eventDao().update(remote.copy(id = local.id, babyId = babyId))
            }
        }
        changes.growth.forEach { (babyUuid, remote) ->
            val babyId = babyId(babyUuid) ?: return@forEach
            val local = dao.growthByUuid(remote.uuid)
            when {
                local == null -> db.growthDao().insert(remote.copy(id = 0, babyId = babyId))
                remote.updatedAt > local.updatedAt -> db.growthDao().update(remote.copy(id = local.id, babyId = babyId))
            }
        }
        changes.appointments.forEach { (babyUuid, remote) ->
            val babyId = babyId(babyUuid) ?: return@forEach
            val local = dao.appointmentByUuid(remote.uuid)
            when {
                local == null -> db.appointmentDao().insert(remote.copy(id = 0, babyId = babyId))
                remote.updatedAt > local.updatedAt -> db.appointmentDao().update(remote.copy(id = local.id, babyId = babyId))
            }
        }
        changes.milestones.forEach { (babyUuid, remote) ->
            val babyId = babyId(babyUuid) ?: return@forEach
            val local = dao.milestoneByUuid(remote.uuid)
            when {
                local == null -> db.milestoneDao().insert(remote.copy(id = 0, babyId = babyId, photoPath = null))
                remote.updatedAt > local.updatedAt ->
                    db.milestoneDao().update(remote.copy(id = local.id, babyId = babyId, photoPath = local.photoPath))
            }
        }
        // Un borrado solo gana si es posterior a la última modificación local.
        changes.deletions.forEach { d ->
            when (d.entity) {
                "baby" -> dao.deleteBaby(d.uuid, d.deletedAt)
                "event" -> dao.deleteEvent(d.uuid, d.deletedAt)
                "growth" -> dao.deleteGrowth(d.uuid, d.deletedAt)
                "appointment" -> dao.deleteAppointment(d.uuid, d.deletedAt)
                "milestone" -> dao.deleteMilestone(d.uuid, d.deletedAt)
            }
        }
    }
}
