package com.kbretro.babyregistry.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Registro de un borrado local, para propagarlo a los demás móviles de la familia al sincronizar. */
@Entity(tableName = "sync_deletions")
data class SyncDeletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** "baby", "event", "growth", "appointment" o "milestone". */
    val entity: String,
    val uuid: String,
    val deletedAt: Long = System.currentTimeMillis(),
)
