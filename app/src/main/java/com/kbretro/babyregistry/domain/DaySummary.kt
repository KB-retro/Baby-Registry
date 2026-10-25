package com.kbretro.babyregistry.domain

import com.kbretro.babyregistry.data.model.BreastTimer
import com.kbretro.babyregistry.data.model.DiaperKind
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import java.time.LocalDate
import java.time.ZoneId

data class DaySummary(
    val feeds: Int = 0,
    val bottleMl: Int = 0,
    val breastMillis: Long = 0,
    val sleepMillis: Long = 0,
    val sleeps: Int = 0,
    val diapers: Int = 0,
    val wet: Int = 0,
    val dirty: Int = 0,
    val solids: Int = 0,
)

/** Resume los eventos de un día [dayStart, dayEnd). El sueño se recorta a los límites del día. */
fun summarize(events: List<EventEntity>, dayStart: Long, dayEnd: Long, now: Long): DaySummary {
    val startedToday = events.filter { it.startTime in dayStart until dayEnd }
    val sleepMillis = events.filter { it.type == EventType.SLEEP }.sumOf { e ->
        val end = minOf(e.endTime ?: now, dayEnd)
        val start = maxOf(e.startTime, dayStart)
        (end - start).coerceAtLeast(0)
    }
    val diapers = startedToday.filter { it.type == EventType.DIAPER }
    return DaySummary(
        feeds = startedToday.count { it.type == EventType.BREAST || it.type == EventType.BOTTLE },
        bottleMl = startedToday.filter { it.type == EventType.BOTTLE }.sumOf { it.amountMl ?: 0 },
        breastMillis = startedToday.filter { it.type == EventType.BREAST }
            .sumOf { BreastTimer.totalMillis(it, now) },
        sleepMillis = sleepMillis,
        sleeps = startedToday.count { it.type == EventType.SLEEP },
        diapers = diapers.size,
        wet = diapers.count { it.diaperKind == DiaperKind.WET || it.diaperKind == DiaperKind.MIXED },
        dirty = diapers.count { it.diaperKind == DiaperKind.DIRTY || it.diaperKind == DiaperKind.MIXED },
        solids = startedToday.count { it.type == EventType.SOLID },
    )
}

object DayRange {
    fun of(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> =
        date.atStartOfDay(zone).toInstant().toEpochMilli() to
            date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    fun today(): Pair<Long, Long> = of(LocalDate.now())
}
