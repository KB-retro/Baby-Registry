package com.kbretro.babyregistry.widget

import com.kbretro.babyregistry.data.model.BabyEntity
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.data.repository.TrackerRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun repository(): TrackerRepository
}

/** Lo que muestra el widget en un momento dado. */
data class WidgetSnapshot(
    val baby: BabyEntity?,
    val activeSleep: EventEntity? = null,
    val activeBreast: EventEntity? = null,
    val lastFeed: EventEntity? = null,
    val lastDiaper: EventEntity? = null,
    val lastSleep: EventEntity? = null,
)

suspend fun TrackerRepository.widgetSnapshot(): WidgetSnapshot {
    val baby = currentBaby.first() ?: return WidgetSnapshot(baby = null)
    val active = active(baby.id).first()
    return WidgetSnapshot(
        baby = baby,
        activeSleep = active.firstOrNull { it.type == EventType.SLEEP },
        activeBreast = active.firstOrNull { it.type == EventType.BREAST },
        lastFeed = latest(baby.id, EventType.BREAST, EventType.BOTTLE).first(),
        lastDiaper = latest(baby.id, EventType.DIAPER).first(),
        lastSleep = latest(baby.id, EventType.SLEEP).first(),
    )
}
