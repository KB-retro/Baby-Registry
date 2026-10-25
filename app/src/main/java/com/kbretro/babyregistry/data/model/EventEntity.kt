package com.kbretro.babyregistry.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    indices = [Index("babyId"), Index("startTime")],
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
    val notes: String? = null,
) {
    val isOngoing: Boolean get() = endTime == null
}
