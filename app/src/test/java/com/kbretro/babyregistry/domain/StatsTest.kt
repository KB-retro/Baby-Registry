package com.kbretro.babyregistry.domain

import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class StatsTest {
    private val zone = ZoneOffset.UTC
    private val day = LocalDate.of(2026, 3, 10)
    private val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
    private val hour = 3_600_000L

    @Test
    fun `el patron recorta el sueno nocturno a cada dia`() {
        val events = listOf(
            EventEntity(babyId = 1, type = EventType.SLEEP, startTime = dayStart - 3 * hour, endTime = dayStart + 6 * hour),
            EventEntity(babyId = 1, type = EventType.BOTTLE, startTime = dayStart + 12 * hour, endTime = dayStart + 12 * hour),
        )
        val pattern = dayPattern(events, day, now = dayStart + 20 * hour, zone = zone)

        assertEquals(listOf(DaySegment(0f, 0.25f)), pattern.sleeps)
        assertEquals(listOf(0.5f), pattern.feeds)

        val previous = dayPattern(events, day.minusDays(1), now = dayStart + 20 * hour, zone = zone)
        assertEquals(listOf(DaySegment(0.875f, 1f)), previous.sleeps)
    }

    @Test
    fun `estadisticas por dia`() {
        val events = listOf(
            EventEntity(babyId = 1, type = EventType.BOTTLE, startTime = dayStart + hour, endTime = dayStart + hour, amountMl = 100),
            EventEntity(babyId = 1, type = EventType.BOTTLE, startTime = dayStart + 30 * hour, endTime = dayStart + 30 * hour, amountMl = 80),
        )
        val stats = dailyStats(events, listOf(day, day.plusDays(1)), now = dayStart + 40 * hour, zone = zone)

        assertEquals(listOf(100, 80), stats.map { it.summary.bottleMl })
        assertEquals(listOf(1, 1), stats.map { it.summary.feeds })
    }
}
