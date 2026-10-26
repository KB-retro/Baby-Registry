package com.kbretro.babyregistry.data.transfer

import com.kbretro.babyregistry.data.model.BottleContent
import com.kbretro.babyregistry.data.model.DiaperKind
import com.kbretro.babyregistry.data.model.EventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class RecordParserTest {
    private val zone = ZoneOffset.UTC
    private val min = 60_000L

    private fun at(text: String) = LocalDateTime.parse(text).toInstant(zone).toEpochMilli()

    @Test
    fun `csv en ingles con cabecera`() {
        val csv = """
            Type,Start,End,Duration,Notes
            Feed,2026-10-20 08:00,2026-10-20 08:15,00:15,"Bottle, 120ml"
            Diaper,2026-10-20 09:00,,,Poo
            Sleep,2026-10-20 10:00,2026-10-20 11:30,01:30,
        """.trimIndent()
        val result = RecordParser.parse(CsvReader.toRecords(csv), zone)

        assertEquals(listOf(EventType.BOTTLE, EventType.DIAPER, EventType.SLEEP), result.events.map { it.type })
        val bottle = result.events[0]
        assertEquals(at("2026-10-20T08:00"), bottle.startTime)
        assertEquals(120, bottle.amountMl)
        assertEquals(DiaperKind.DIRTY, result.events[1].diaperKind)
        assertEquals(at("2026-10-20T11:30"), result.events[2].endTime)
    }

    @Test
    fun `texto de pdf en espanol agrupado por dias`() {
        val lines = """
            Informe de Lucía
            Lunes, 20/10/2026
            08:05 Pecho izquierdo 10 min derecho 5 min
            09:30 Pañal pis y caca
            10:15 - 11:45 Siesta
            14:00 Biberón 150 ml fórmula
            16:00 Leche materna 90 ml
            Martes, 21/10/2026
            37,8 °C temperatura 07:30
        """.trimIndent().lines()
        val result = RecordParser.parse(lines, zone)
        val byType = result.events.groupBy { it.type }

        val breast = byType.getValue(EventType.BREAST).single()
        assertEquals(at("2026-10-20T08:05"), breast.startTime)
        assertEquals(10 * min, breast.leftMillis)
        assertEquals(5 * min, breast.rightMillis)

        assertEquals(DiaperKind.MIXED, byType.getValue(EventType.DIAPER).single().diaperKind)

        val sleep = byType.getValue(EventType.SLEEP).single()
        assertEquals(90 * min, sleep.endTime!! - sleep.startTime)

        val bottles = byType.getValue(EventType.BOTTLE)
        assertEquals(listOf(150, 90), bottles.map { it.amountMl })
        assertEquals(listOf(BottleContent.FORMULA, BottleContent.BREAST_MILK), bottles.map { it.bottleContent })

        val temp = byType.getValue(EventType.TEMPERATURE).single()
        assertEquals(37.8, temp.temperatureC!!, 0.001)
        assertEquals(at("2026-10-21T07:30"), temp.startTime)

        assertEquals(listOf("Informe de Lucía"), result.unrecognized)
    }

    @Test
    fun `formato estadounidense con am y pm`() {
        val result = RecordParser.parse(listOf("10/21/2026 3:30 PM Diaper wet"), zone)
        val diaper = result.events.single()
        assertEquals(at("2026-10-21T15:30"), diaper.startTime)
        assertEquals(DiaperKind.WET, diaper.diaperKind)
    }

    @Test
    fun `sin hora o sin tipo no se importa`() {
        val result = RecordParser.parse(listOf("20/10/2026 Biberón 120 ml", "Total semanal: 5"), zone)
        assertTrue(result.events.isEmpty())
        assertEquals(2, result.unrecognized.size)
    }

    @Test
    fun `csv con punto y coma y comillas`() {
        val rows = CsvReader.read("a;b;c\n\"x;1\";\"dice \"\"hola\"\"\";3\n")
        assertEquals(listOf(listOf("a", "b", "c"), listOf("x;1", "dice \"hola\"", "3")), rows)
    }
}
