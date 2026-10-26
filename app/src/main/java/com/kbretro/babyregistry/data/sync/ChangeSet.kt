package com.kbretro.babyregistry.data.sync

import com.kbretro.babyregistry.data.model.AppointmentEntity
import com.kbretro.babyregistry.data.model.BabyEntity
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.GrowthEntity
import com.kbretro.babyregistry.data.model.MilestoneEntity
import com.kbretro.babyregistry.data.model.SyncDeletionEntity

/**
 * Un registro tal como viaja entre móviles: los id locales no significan nada fuera del móvil,
 * así que cada registro lleva el uuid de su bebé.
 */
data class Synced<T>(val babyUuid: String, val item: T)

/** Cambios a enviar o recibidos desde la última sincronización. */
data class ChangeSet(
    val babies: List<BabyEntity> = emptyList(),
    val events: List<Synced<EventEntity>> = emptyList(),
    val growth: List<Synced<GrowthEntity>> = emptyList(),
    val appointments: List<Synced<AppointmentEntity>> = emptyList(),
    val milestones: List<Synced<MilestoneEntity>> = emptyList(),
    val deletions: List<SyncDeletionEntity> = emptyList(),
) {
    val isEmpty: Boolean
        get() = babies.isEmpty() && events.isEmpty() && growth.isEmpty() &&
            appointments.isEmpty() && milestones.isEmpty() && deletions.isEmpty()
}
