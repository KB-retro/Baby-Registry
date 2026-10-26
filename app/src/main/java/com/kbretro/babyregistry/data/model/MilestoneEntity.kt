package com.kbretro.babyregistry.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** Un hito: tipo, cuándo ocurrió y, si aplica, cuál (la palabra, la operación…). */
@Entity(
    tableName = "milestones",
    foreignKeys = [
        ForeignKey(
            entity = BabyEntity::class,
            parentColumns = ["id"],
            childColumns = ["babyId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("babyId"), Index(value = ["uuid"], unique = true)],
)
data class MilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val babyId: Long,
    val time: Long,
    val kind: MilestoneKind,
    val detail: String? = null,
    val notes: String? = null,
    val photoPath: String? = null,
    /** Identificador global para sincronizar entre móviles (los id locales cambian en cada uno). */
    val uuid: String = UUID.randomUUID().toString(),
    /** Última modificación: en un conflicto entre móviles gana la más reciente. */
    val updatedAt: Long = System.currentTimeMillis(),
)
