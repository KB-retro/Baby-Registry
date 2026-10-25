package com.kbretro.babyregistry.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FmtTest {
    @Test
    fun clock() {
        assertEquals("00:00", Fmt.clock(0))
        assertEquals("05:09", Fmt.clock(309_000))
        assertEquals("1:02:03", Fmt.clock(3_723_000))
    }

    @Test
    fun duration() {
        assertEquals("45 s", Fmt.duration(45_000))
        assertEquals("12 min", Fmt.duration(12 * 60_000L))
        assertEquals("2 h", Fmt.duration(120 * 60_000L))
        assertEquals("1 h 05 min", Fmt.duration(65 * 60_000L))
    }

    @Test
    fun age() {
        val birth = LocalDate.of(2026, 1, 10)
        assertEquals("Recién nacido", Fmt.age(birth, birth))
        assertEquals("1 día", Fmt.age(birth, LocalDate.of(2026, 1, 11)))
        assertEquals("3 meses y 5 días", Fmt.age(birth, LocalDate.of(2026, 4, 15)))
        assertEquals("1 año", Fmt.age(birth, LocalDate.of(2027, 1, 10)))
        assertEquals("1 año y 2 meses", Fmt.age(birth, LocalDate.of(2027, 3, 20)))
    }
}
