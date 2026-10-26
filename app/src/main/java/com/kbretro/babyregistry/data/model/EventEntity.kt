package com.kbretro.babyregistry.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Un registro del bebé. Todas las horas son epoch millis.
 *
 * Los eventos puntuales (biberón, sólidos, pañal) tienen `endTime == startTime`.
 * Los temporizadores en curso (pecho, sueño) tienen `endTime == null`.
 */
@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = BabyEntity::class,
            parentColumns = ["id"],
            childColumns = ["babyId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("babyId"), Index("startTime"), Index(value = ["uuid"], unique = true)],
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val babyId: Long,
    val type: EventType,
    val startTime: Long,
    val endTime: Long? = null,
    // Pecho
    val leftMillis: Long = 0,
    val rightMillis: Long = 0,
    val activeSide: BreastSide? = null,
    val activeSideSince: Long? = null,
    val lastSide: BreastSide? = null,
    // Biberón
    val amountMl: Int? = null,
    val bottleContent: BottleContent? = null,
    // Sólidos
    val food: String? = null,
    val isNewFood: Boolean = false,
    val reaction: String? = null,
    // Pañal
    val diaperKind: DiaperKind? = null,
    val stoolColor: StoolColor? = null,
    // Salud
    val temperatureC: Double? = null,
    val medicineName: String? = null,
    val dose: String? = null,
    /** Momento del recordatorio programado (p. ej. próxima dosis), si lo hay. */
    val reminderAt: Long? = null,
    val notes: String? = null,
    /** Identificador global para sincronizar entre móviles (los id locales cambian en cada uno). */
    val uuid: String = UUID.randomUUID().toString(),
    /** Última modificación: en un conflicto entre móviles gana la más reciente. */
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val isOngoing: Boolean get() = endTime == null
}
