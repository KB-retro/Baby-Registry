package com.kbretro.babyregistry.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID
import java.time.LocalDate

@Entity(tableName = "babies", indices = [Index(value = ["uuid"], unique = true)])
data class BabyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val birthDate: LocalDate,
    val sex: Sex = Sex.UNSPECIFIED,
    val createdAt: Long = System.currentTimeMillis(),
    val feedingType: FeedingType? = null,
    /** Tipos de registro ocultos, separados por comas (p. ej. "BOTTLE,SOLID"). */
    val hiddenTypes: String? = null,
    /** Nombre del archivo de la foto dentro de la carpeta privada de fotos, si tiene. */
    val photoPath: String? = null,
    /** Identificador global para sincronizar entre móviles (los id locales cambian en cada uno). */
    val uuid: String = UUID.randomUUID().toString(),
    /** Última modificación: en un conflicto entre móviles gana la más reciente. */
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val hidden: Set<EventType>
        get() = hiddenTypes.orEmpty().split(',')
            .mapNotNull { name -> EventType.entries.firstOrNull { it.name == name } }
            .toSet()

    fun withHidden(types: Set<EventType>): BabyEntity =
        copy(hiddenTypes = types.sortedBy { it.ordinal }.joinToString(",") { it.name }.ifEmpty { null })

    fun isVisible(type: EventType): Boolean = type !in hidden
}
