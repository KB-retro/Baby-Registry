package com.kbretro.babyregistry.domain

import com.kbretro.babyregistry.data.model.DiaperKind
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import org.junit.Assert.assertEquals
import org.junit.Test

class DaySummaryTest {
    private val hour = 3_600_000L
    private val dayStart = 100 * 24 * hour
    private val dayEnd = dayStart + 24 * hour

    private fun event(type: EventType, start: Long, end: Long? = start, block: EventEntity.() -> EventEntity = { this }) =
        EventEntity(babyId = 1, type = type, startTime = start, endTime = end).block()

    @Test
    fun `el sueno que cruza la medianoche solo cuenta la parte del dia`() {
        val events = listOf(event(EventType.SLEEP, dayStart - 2 * hour, dayStart + 6 * hour))
        val summary = summarize(events, dayStart, dayEnd, now = dayStart + 10 * hour)

        assertEquals(6 * hour, summary.sleepMillis)
        assertEquals(0, summary.sleeps)
    }

    @Test
    fun `el sueno en curso cuenta hasta ahora`() {
        val events = listOf(event(EventType.SLEEP, dayStart + 13 * hour, end = null))
        val summary = summarize(events, dayStart, dayEnd, now = dayStart + 14 * hour)

        assertEquals(hour, summary.sleepMillis)
        assertEquals(1, summary.sleeps)
    }

    @Test
    fun `cuenta tomas, mililitros y panales`() {
        val events = listOf(
            event(EventType.BOTTLE, dayStart + hour) { copy(amountMl = 120) },
            event(EventType.BOTTLE, dayStart + 4 * hour) { copy(amountMl = 90) },
            event(EventType.BREAST, dayStart + 6 * hour, dayStart + 6 * hour + 900_000) {
                copy(leftMillis = 600_000, rightMillis = 300_000)
            },
            event(EventType.DIAPER, dayStart + 2 * hour) { copy(diaperKind = DiaperKind.WET) },
            event(EventType.DIAPER, dayStart + 3 * hour) { copy(diaperKind = DiaperKind.MIXED) },
            event(EventType.SOLID, dayStart + 5 * hour) { copy(food = "Pera") },
        )
        val summary = summarize(events, dayStart, dayEnd, now = dayStart + 12 * hour)

        assertEquals(3, summary.feeds)
        assertEquals(210, summary.bottleMl)
        assertEquals(900_000L, summary.breastMillis)
        assertEquals(2, summary.diapers)
        assertEquals(2, summary.wet)
        assertEquals(1, summary.dirty)
        assertEquals(1, summary.solids)
    }
}
