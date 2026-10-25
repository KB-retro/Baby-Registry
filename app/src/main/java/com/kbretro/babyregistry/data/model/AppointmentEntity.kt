package com.kbretro.babyregistry.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Cita médica, vacuna u otro evento futuro con recordatorio opcional. */
@Entity(
    tableName = "appointments",
    foreignKeys = [
        ForeignKey(
            entity = BabyEntity::class,
            parentColumns = ["id"],
            childColumns = ["babyId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("babyId")],
)
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val babyId: Long,
    val time: Long,
    val kind: AppointmentKind,
    val title: String,
    val notes: String? = null,
    val reminderAt: Long? = null,
    val done: Boolean = false,
)
