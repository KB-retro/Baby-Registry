package com.kbretro.babyregistry.data.transfer

import android.app.Application
import com.kbretro.babyregistry.data.model.AppointmentEntity
import com.kbretro.babyregistry.data.model.AppointmentKind
import com.kbretro.babyregistry.data.model.BabyEntity
import com.kbretro.babyregistry.data.model.BottleContent
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.data.model.FeedingType
import com.kbretro.babyregistry.data.model.GrowthEntity
import com.kbretro.babyregistry.data.model.MilestoneEntity
import com.kbretro.babyregistry.data.model.MilestoneKind
import com.kbretro.babyregistry.data.model.Sex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BackupJsonTest {

    @Test
    fun `la copia se lee igual que se escribio`() {
        val baby = BabyEntity(
            id = 7,
            name = "Leo",
            birthDate = LocalDate.of(2026, 5, 1),
            sex = Sex.BOY,
            createdAt = 0,
            feedingType = FeedingType.MIXED,
            hiddenTypes = "SOLID",
        )
        val backup = BabyBackup(
            baby = baby,
            events = listOf(
                EventEntity(id = 3, babyId = 7, type = EventType.BOTTLE, startTime = 1000, endTime = 1000, amountMl = 120, bottleContent = BottleContent.FORMULA),
                EventEntity(id = 4, babyId = 7, type = EventType.TEMPERATURE, startTime = 2000, endTime = 2000, temperatureC = 37.9, notes = "axilar"),
            ),
            growth = listOf(GrowthEntity(id = 1, babyId = 7, date = LocalDate.of(2026, 6, 1), weightKg = 5.1)),
            appointments = listOf(AppointmentEntity(id = 2, babyId = 7, time = 5000, kind = AppointmentKind.VACCINE, title = "2 meses", reminderAt = 4000)),
            milestones = listOf(MilestoneEntity(id = 5, babyId = 7, time = 6000, kind = MilestoneKind.SURGERY, detail = "Sindactilia", notes = "Todo bien")),
        )

        val content = TextExtractor.json(BackupJson.write(listOf(backup), exportedAt = 9000))
        assertTrue(content is FileContent.Backup)
        val read = (content as FileContent.Backup).babies.single()

        // Los identificadores no se guardan (al restaurar se crean nuevos): se comparan los datos.
        assertEquals(
            baby.copy(id = 0, createdAt = 0, uuid = "", updatedAt = 0),
            read.baby.copy(createdAt = 0, uuid = "", updatedAt = 0),
        )
        assertEquals(backup.events.map { it.copy(id = 0, babyId = 0, uuid = "", updatedAt = 0) }, read.events.map { it.copy(uuid = "", updatedAt = 0) })
        assertEquals(backup.growth.map { it.copy(id = 0, babyId = 0, uuid = "", updatedAt = 0) }, read.growth.map { it.copy(uuid = "", updatedAt = 0) })
        assertEquals(
            backup.appointments.map { it.copy(id = 0, babyId = 0, uuid = "", updatedAt = 0) },
            read.appointments.map { it.copy(uuid = "", updatedAt = 0) },
        )
        assertEquals(
            backup.milestones.map { it.copy(id = 0, babyId = 0, uuid = "", updatedAt = 0) },
            read.milestones.map { it.copy(uuid = "", updatedAt = 0) },
        )
    }

    @Test
    fun `json de otra app con dias anidados y horas en epoch`() {
        // 2026-10-20 08:00 y 09:00 en la zona del sistema.
        val zone = java.time.ZoneId.systemDefault()
        val t1 = LocalDate.of(2026, 10, 20).atTime(8, 0).atZone(zone).toEpochSecond()
        val json = """
            {"baby": "Lucía", "days": [
              {"date": "2026-10-20", "entries": [
                {"type": "bottle", "time": "08:00", "amount": "120 ml"},
                {"type": "diaper", "time": "09:00", "kind": "wet"}
              ]},
              {"entries": [{"type": "sleep", "start": $t1, "durationMinutes": "45 min"}]}
            ]}
        """.trimIndent()
        val content = TextExtractor.json(json) as FileContent.Records
        val result = RecordParser.parse(content.records)

        assertEquals(listOf(EventType.BOTTLE, EventType.SLEEP, EventType.DIAPER), result.events.map { it.type })
        assertEquals(120, result.events.first().amountMl)
        val sleep = result.events.first { it.type == EventType.SLEEP }
        assertEquals(t1 * 1000, sleep.startTime)
        assertEquals(45 * 60_000L, sleep.endTime!! - sleep.startTime)
    }
}
