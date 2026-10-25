package com.kbretro.babyregistry.domain

import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import java.time.LocalDate
import java.time.ZoneId

data class DayStats(val date: LocalDate, val summary: DaySummary)

/** Tramo de un día expresado como fracción de las 24 h (0 = 00:00, 1 = 24:00). */
data class DaySegment(val start: Float, val end: Float)

data class DayPattern(val date: LocalDate, val sleeps: List<DaySegment>, val feeds: List<Float>)

fun dailyStats(
    events: List<EventEntity>,
    days: List<LocalDate>,
    now: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): List<DayStats> = days.map { date ->
    val (start, end) = DayRange.of(date, zone)
    DayStats(date, summarize(events, start, end, now))
}

fun dayPattern(
    events: List<EventEntity>,
    date: LocalDate,
    now: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): DayPattern {
    val (start, end) = DayRange.of(date, zone)
    val length = (end - start).toFloat()
    fun fraction(t: Long) = ((t - start) / length).coerceIn(0f, 1f)
    val sleeps = events
        .filter { it.type == EventType.SLEEP }
        .mapNotNull { e ->
            val from = maxOf(e.startTime, start)
            val to = minOf(e.endTime ?: now, end)
            if (to > from) DaySegment(fraction(from), fraction(to)) else null
        }
    val feeds = events
        .filter { (it.type == EventType.BREAST || it.type == EventType.BOTTLE) && it.startTime in start until end }
        .map { fraction(it.startTime) }
    return DayPattern(date, sleeps, feeds)
}

/** Media de los valores, o null si no hay ninguno. */
fun <T> List<T>.averageOf(selector: (T) -> Double): Double? = if (isEmpty()) null else sumOf(selector) / size
