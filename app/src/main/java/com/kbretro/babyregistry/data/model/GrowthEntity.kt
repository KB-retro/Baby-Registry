package com.kbretro.babyregistry.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID
import java.time.LocalDate

@Entity(
    tableName = "growth",
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
data class GrowthEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val babyId: Long,
    val date: LocalDate,
    val weightKg: Double? = null,
    val lengthCm: Double? = null,
    val headCm: Double? = null,
    val notes: String? = null,
    /** Identificador global para sincronizar entre móviles (los id locales cambian en cada uno). */
    val uuid: String = UUID.randomUUID().toString(),
    /** Última modificación: en un conflicto entre móviles gana la más reciente. */
    val updatedAt: Long = System.currentTimeMillis(),
)
