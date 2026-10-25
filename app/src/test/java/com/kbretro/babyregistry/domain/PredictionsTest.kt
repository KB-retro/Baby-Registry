package com.kbretro.babyregistry.domain

import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PredictionsTest {
    private val min = 60_000L
    private val hour = 60 * min
    private val now = 100 * 24 * hour

    private fun bottle(at: Long) = EventEntity(babyId = 1, type = EventType.BOTTLE, startTime = at, endTime = at)
    private fun sleep(start: Long, end: Long?) = EventEntity(babyId = 1, type = EventType.SLEEP, startTime = start, endTime = end)

    @Test
    fun `la proxima toma usa la mediana de los intervalos`() {
        // Intervalos: 3 h, 3 h, 2 h 30, y uno atípico de 10 h que se descarta.
        val starts = listOf(now - 20 * hour, now - 10 * hour, now - 7 * hour, now - 4 * hour, now - 90 * min)
        val prediction = Predictions.nextFeed(starts.map(::bottle), now)!!

        assertEquals(now - 90 * min + 3 * hour, prediction.at)
        assertEquals(3, prediction.samples)
    }

    @Test
    fun `sin datos suficientes no hay prediccion`() {
        assertNull(Predictions.nextFeed(listOf(bottle(now - 3 * hour), bottle(now - hour)), now))
    }

    @Test
    fun `el proximo sueno usa las ventanas de vigilia`() {
        val sleeps = listOf(
            sleep(now - 12 * hour, now - 11 * hour), // vigilia 2 h
            sleep(now - 9 * hour, now - 8 * hour), // vigilia 1 h 30
            sleep(now - 390 * min, now - 5 * hour), // vigilia 2 h
            sleep(now - 3 * hour, now - 2 * hour),
        )
        val prediction = Predictions.nextSleep(sleeps, now)!!

        assertEquals(now - 2 * hour + 2 * hour, prediction.at)
    }

    @Test
    fun `si esta durmiendo no se predice el sueno`() {
        val sleeps = listOf(
            sleep(now - 12 * hour, now - 11 * hour),
            sleep(now - 9 * hour, now - 8 * hour),
            sleep(now - 6 * hour, now - 5 * hour),
            sleep(now - 30 * min, null),
        )
        assertNull(Predictions.nextSleep(sleeps, now))
    }
}
