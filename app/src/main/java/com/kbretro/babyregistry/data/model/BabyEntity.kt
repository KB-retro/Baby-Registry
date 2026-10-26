package com.kbretro.babyregistry.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "babies")
data class BabyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val birthDate: LocalDate,
    val sex: Sex = Sex.UNSPECIFIED,
    val createdAt: Long = System.currentTimeMillis(),
    val feedingType: FeedingType? = null,
    /** Tipos de registro ocultos, separados por comas (p. ej. "BOTTLE,SOLID"). */
    val hiddenTypes: String? = null,
) {
    val hidden: Set<EventType>
        get() = hiddenTypes.orEmpty().split(',')
            .mapNotNull { name -> EventType.entries.firstOrNull { it.name == name } }
            .toSet()

    fun withHidden(types: Set<EventType>): BabyEntity =
        copy(hiddenTypes = types.sortedBy { it.ordinal }.joinToString(",") { it.name }.ifEmpty { null })

    fun isVisible(type: EventType): Boolean = type !in hidden
}
