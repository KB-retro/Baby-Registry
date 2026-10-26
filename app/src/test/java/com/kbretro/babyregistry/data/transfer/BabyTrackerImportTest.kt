package com.kbretro.babyregistry.data.transfer

import com.kbretro.babyregistry.data.model.AppointmentKind
import com.kbretro.babyregistry.data.model.DiaperKind
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.data.model.MilestoneKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Formato de la exportación CSV de Baby Tracker (datos de ejemplo inventados). */
class BabyTrackerImportTest {
    private val zone = ZoneOffset.UTC
    private fun at(text: String) = LocalDateTime.parse(text).toInstant(zone).toEpochMilli()

    private val files = listOf(
        "Ana_formula.csv" to """
            Bebé,Hora,Cantidad (ml),Nota
            "Ana","3/5/26 15:30","90",""
            "Ana","26/10/26 4:39","120","Se dejó un poco."
        """.trimIndent(),
        "Ana_diaper.csv" to """
            Bebé,Hora,Estado,Nota
            "Ana","3/5/26 16:30","Mixto","Primera caca del día."
            "Ana","3/5/26 19:15","Húmedo",""
            "Ana","4/5/26 9:00","Sucio",""
            "Ana","4/5/26 12:00","Seco",""
        """.trimIndent(),
        "Ana_medication.csv" to """
            Bebé,Hora,Nombre del medicamento,Cantidad,,Nota
            "Ana","13/6/26 1:00","Apiretal","0,7","ml",""
            "Ana","13/6/26 9:00","Ejercicios lengua","1","Sesion",""
            "Ana","13/6/26 9:00","Ejercicios mofletes","1","Sesion",""
        """.trimIndent(),
        "Ana_growth.csv" to """
            Bebé,Hora,Largo (cm),Peso (kg),Tamaño de la cabeza (cm),Nota
            "Ana","24/6/26 11:00","59","5,73",,""
            "Ana","19/8/26 13:00",,"6,82",,""
        """.trimIndent(),
        "Ana_vaccine.csv" to """
            Bebé,Hora,Vacuna,Nota
            "Ana","1/7/26 10:15","PCV",""
        """.trimIndent(),
        "Ana_milestone.csv" to """
            Bebé,Hora,Momento clave,Nota
            "Ana","8/5/26 1:30","Caída Cordón umbilical",""
            "Ana","12/6/26 10:00","Operación ","Frenectomía corte frenillo."
            "Ana","16/10/26 12:57","Primer diente",""
            "Ana","20/10/26 18:00","Primera palabra","Mamá"
            "Ana","21/10/26 9:00","Bautizo","En la iglesia del pueblo"
        """.trimIndent(),
    )

    @Test
    fun `reconoce los archivos de baby tracker`() {
        assertTrue(files.all { (name, text) -> BabyTrackerImport.matches(name, text) })
        assertFalse(BabyTrackerImport.matches("datos.csv", "Fecha,Tipo\n1/1/26,biberón"))
    }

    @Test
    fun `importa biberones, pañales, medicación, medidas y vacunas`() {
        val r = BabyTrackerImport.parse(files, zone)

        assertEquals(setOf("Ana"), r.babyNames)
        val byType = r.events.groupBy { it.type }

        val bottles = byType.getValue(EventType.BOTTLE)
        assertEquals(listOf(90, 120), bottles.map { it.amountMl })
        assertEquals(at("2026-10-26T04:39"), bottles[1].startTime)
        assertEquals("Se dejó un poco.", bottles[1].notes)

        assertEquals(
            listOf(DiaperKind.MIXED, DiaperKind.WET, DiaperKind.DIRTY),
            byType.getValue(EventType.DIAPER).map { it.diaperKind },
        )

        // Dos medicamentos distintos a la misma hora no son duplicados.
        val meds = byType.getValue(EventType.MEDICINE)
        assertEquals(3, meds.size)
        assertEquals("0,7 ml", meds.first().dose)

        assertEquals(listOf(LocalDate.of(2026, 6, 24), LocalDate.of(2026, 8, 19)), r.growth.map { it.date })
        assertEquals(5.73, r.growth[0].weightKg!!, 1e-9)
        assertEquals(59.0, r.growth[0].lengthCm!!, 1e-9)

        val vaccine = r.appointments.single()
        assertEquals(AppointmentKind.VACCINE, vaccine.kind)
        assertEquals("PCV", vaccine.title)
        assertTrue(vaccine.done)

        assertEquals(mapOf("Pañales \"Seco\" (sin equivalente)" to 1), r.skipped)

        val milestones = r.milestones
        assertEquals(
            listOf(MilestoneKind.UMBILICAL_CORD, MilestoneKind.SURGERY, MilestoneKind.FIRST_TOOTH, MilestoneKind.FIRST_WORD, MilestoneKind.OTHER),
            milestones.map { it.kind },
        )
        assertEquals("Frenectomía corte frenillo", milestones[1].detail)
        assertEquals("Mamá", milestones[3].detail)
        assertEquals("Bautizo", milestones[4].detail)
        assertEquals("En la iglesia del pueblo", milestones[4].notes)
        assertEquals(at("2026-05-08T01:30"), milestones[0].time)
    }

    @Test
    fun `fechas en formato estadounidense`() {
        val us = listOf("Ana_formula.csv" to "Baby,Time,Amount (ml),Note\n\"Ana\",\"10/26/26, 4:39 PM\",\"120\",\"\"\n\"Ana\",\"10/27/26, 8:00 AM\",\"90\",\"\"")
        val r = BabyTrackerImport.parse(us, zone)
        assertEquals(listOf(at("2026-10-26T16:39"), at("2026-10-27T08:00")), r.events.map { it.startTime })
    }
}
