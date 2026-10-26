package com.kbretro.babyregistry.domain

import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType

/** Estimación de cuándo ocurrirá algo, basada en [samples] intervalos recientes. */
data class Prediction(val at: Long, val samples: Int)

/**
 * Predicciones sencillas a partir de los patrones de los últimos días:
 * la mediana de los intervalos recientes, ignorando valores atípicos.
 */
object Predictions {
    const val WINDOW_MILLIS = 3 * 24 * 3_600_000L
    private const val MINUTE = 60_000L
    private const val MIN_SAMPLES = 3

    /** Próxima toma: última toma + mediana del tiempo entre tomas. */
    fun nextFeed(events: List<EventEntity>, now: Long): Prediction? {
        val feeds = events
            .filter { (it.type == EventType.BREAST || it.type == EventType.BOTTLE) && it.startTime in (now - WINDOW_MILLIS)..now }
            .sortedBy { it.startTime }
        if (feeds.any { it.isOngoing }) return null
        val intervals = feeds.zipWithNext { a, b -> b.startTime - a.startTime }
            .filter { it in 20 * MINUTE..8 * 60 * MINUTE }
        if (intervals.size < MIN_SAMPLES) return null
        return Prediction(feeds.last().startTime + median(intervals), intervals.size)
    }

    /** Próximo sueño: hora en que despertó + mediana de las ventanas de vigilia. */
    fun nextSleep(events: List<EventEntity>, now: Long): Prediction? {
        val sleeps = events
            .filter { it.type == EventType.SLEEP && it.startTime in (now - WINDOW_MILLIS)..now }
            .sortedBy { it.startTime }
        if (sleeps.isEmpty() || sleeps.any { it.isOngoing }) return null
        val windows = sleeps.zipWithNext { a, b -> b.startTime - (a.endTime ?: a.startTime) }
            .filter { it in 10 * MINUTE..8 * 60 * MINUTE }
        if (windows.size < MIN_SAMPLES) return null
        val lastWake = sleeps.last().endTime ?: return null
        return Prediction(lastWake + median(windows), windows.size)
    }

    fun median(values: List<Long>): Long {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
    }
}
