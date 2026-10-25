package com.kbretro.babyregistry.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BreastTimerTest {
    private val min = 60_000L

    @Test
    fun `cambiar de lado acumula el tiempo del lado anterior`() {
        var e = BreastTimer.start(babyId = 1, side = BreastSide.LEFT, now = 0)
        e = BreastTimer.tapSide(e, BreastSide.RIGHT, now = 5 * min)

        assertEquals(5 * min, e.leftMillis)
        assertEquals(BreastSide.RIGHT, e.activeSide)
        assertEquals(3 * min, BreastTimer.sideMillis(e, BreastSide.RIGHT, now = 8 * min))
        assertEquals(8 * min, BreastTimer.totalMillis(e, now = 8 * min))
    }

    @Test
    fun `pulsar el lado activo pausa el temporizador`() {
        var e = BreastTimer.start(1, BreastSide.LEFT, now = 0)
        e = BreastTimer.tapSide(e, BreastSide.LEFT, now = 4 * min)

        assertNull(e.activeSide)
        assertEquals(4 * min, BreastTimer.totalMillis(e, now = 10 * min))

        e = BreastTimer.tapSide(e, BreastSide.LEFT, now = 10 * min)
        assertEquals(6 * min, BreastTimer.totalMillis(e, now = 12 * min))
    }

    @Test
    fun `terminar cierra el evento y recuerda el ultimo lado`() {
        var e = BreastTimer.start(1, BreastSide.LEFT, now = 0)
        e = BreastTimer.tapSide(e, BreastSide.RIGHT, now = 7 * min)
        e = BreastTimer.stop(e, now = 12 * min)

        assertEquals(12 * min, e.endTime)
        assertEquals(7 * min, e.leftMillis)
        assertEquals(5 * min, e.rightMillis)
        assertEquals(BreastSide.RIGHT, e.lastSide)
        assertEquals(BreastSide.LEFT, e.lastSide?.opposite)
        assertNull(e.activeSide)
    }
}
