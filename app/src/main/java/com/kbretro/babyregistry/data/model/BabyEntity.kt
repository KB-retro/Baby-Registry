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
)
