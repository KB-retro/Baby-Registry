package com.kbretro.babyregistry.data.transfer

import com.kbretro.babyregistry.data.model.AppointmentEntity
import com.kbretro.babyregistry.data.model.AppointmentKind
import com.kbretro.babyregistry.data.model.BottleContent
import com.kbretro.babyregistry.data.model.DiaperKind
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.data.model.GrowthEntity
import com.kbretro.babyregistry.data.model.MilestoneEntity
import com.kbretro.babyregistry.data.model.MilestoneKind
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Importa la exportación CSV de la app "Baby Tracker" (un archivo por tipo: Nombre_formula.csv,
 * Nombre_diaper.csv…, normalmente dentro de un .zip). Cada archivo empieza por las columnas
 * "Bebé, Hora" (o "Baby, Time") y la hora va como "26/10/26 4:53".
 */
object BabyTrackerImport {

    data class Result(
        val events: List<EventEntity>,
        val growth: List<GrowthEntity>,
        val appointments: List<AppointmentEntity>,
        val milestones: List<MilestoneEntity>,
        val babyNames: Set<String>,
        /** Lo que no se puede importar, con su número (p. ej. "Hitos" → 4). */
        val skipped: Map<String, Int>,
    ) {
        val total: Int get() = events.size + growth.size + appointments.size + milestones.size
    }

    private val fileKind = Regex("""_([a-z]+)\.csv$""", RegexOption.IGNORE_CASE)
    private val dateTime = Regex("""(\d{1,2})/(\d{1,2})/(\d{2,4}),?\s+(\d{1,2}):(\d{2})(?::\d{2})?\s*([ap])?\.?\s?m?\.?""", RegexOption.IGNORE_CASE)

    /** ¿Tiene pinta de exportación de Baby Tracker? (nombre "X_tipo.csv" y columnas "Bebé, Hora"). */
    fun matches(fileName: String, text: String): Boolean {
        val firstLine = text.removePrefix("\uFEFF").lineSequence().firstOrNull().orEmpty()
        val header = CsvReader.read(firstLine).firstOrNull() ?: return false
        val first = header.take(2).map { RecordParser.normalize(it.trim()) }
        return fileKind.containsMatchIn(fileName) &&
            first.size == 2 && first[0] in setOf("bebe", "baby") && first[1] in setOf("hora", "time")
    }

    fun parse(files: List<Pair<String, String>>, zone: ZoneId = ZoneId.systemDefault()): Result {
        val events = mutableListOf<EventEntity>()
        val growth = mutableListOf<GrowthEntity>()
        val appointments = mutableListOf<AppointmentEntity>()
        val milestones = mutableListOf<MilestoneEntity>()
        val names = mutableSetOf<String>()
        val skipped = linkedMapOf<String, Int>()
        fun skip(label: String, count: Int = 1) {
            if (count > 0) skipped[label] = (skipped[label] ?: 0) + count
        }

        for ((fileName, text) in files) {
            val rows = CsvReader.read(text.removePrefix("﻿"))
            if (rows.size < 2) continue
            val header = rows.first().map { RecordParser.normalize(it) }
            val data = rows.drop(1)
            val kind = fileKind.find(fileName)?.groupValues?.get(1)?.lowercase().orEmpty()
            val dayFirst = isDayFirst(data.map { it.getOrElse(1) { "" } })
            data.mapNotNullTo(names) { it.firstOrNull()?.trim()?.ifEmpty { null } }

            fun column(vararg keys: String) = header.indexOfFirst { h -> keys.any { h.contains(it) } }
            val noteCol = column("nota", "note")
            fun note(row: List<String>) = row.getOrNull(noteCol)?.trim()?.ifEmpty { null }
            fun time(row: List<String>): Long? =
                parseDateTime(row.getOrElse(1) { "" }, dayFirst)?.atZone(zone)?.toInstant()?.toEpochMilli()

            when (kind) {
                "formula", "bottle" -> {
                    val amountCol = column("ml", "cantidad", "amount")
                    data.forEach { row ->
                        val at = time(row) ?: return@forEach skip("Biberones sin fecha válida")
                        events += EventEntity(
                            babyId = 0,
                            type = EventType.BOTTLE,
                            startTime = at,
                            endTime = at,
                            amountMl = row.getOrNull(amountCol)?.let(::decimal)?.toInt(),
                            bottleContent = BottleContent.FORMULA,
                            notes = note(row),
                        )
                    }
                }
                "diaper" -> {
                    val statusCol = column("estado", "status")
                    data.forEach { row ->
                        val at = time(row) ?: return@forEach skip("Pañales sin fecha válida")
                        val status = RecordParser.normalize(row.getOrElse(statusCol) { "" }.trim())
                        val diaperKind = when (status) {
                            "humedo", "wet", "pis" -> DiaperKind.WET
                            "sucio", "dirty", "caca" -> DiaperKind.DIRTY
                            "mixto", "mixed", "both" -> DiaperKind.MIXED
                            else -> null
                        }
                        if (diaperKind == null) {
                            skip("Pañales \"${row.getOrElse(statusCol) { "" }.trim()}\" (sin equivalente)")
                        } else {
                            events += EventEntity(
                                babyId = 0,
                                type = EventType.DIAPER,
                                startTime = at,
                                endTime = at,
                                diaperKind = diaperKind,
                                notes = note(row),
                            )
                        }
                    }
                }
                "medication", "medicine" -> {
                    val nameCol = column("medicamento", "medicine", "medication", "nombre", "name")
                    val amountCol = column("cantidad", "amount")
                    data.forEach { row ->
                        val at = time(row) ?: return@forEach skip("Medicamentos sin fecha válida")
                        // La unidad va en la columna sin título que sigue a la cantidad.
                        val dose = listOfNotNull(
                            row.getOrNull(amountCol)?.trim()?.ifEmpty { null },
                            row.getOrNull(amountCol + 1)?.trim()?.ifEmpty { null }?.takeIf { amountCol + 1 != noteCol },
                        ).joinToString(" ").ifEmpty { null }
                        events += EventEntity(
                            babyId = 0,
                            type = EventType.MEDICINE,
                            startTime = at,
                            endTime = at,
                            medicineName = row.getOrNull(nameCol)?.trim()?.ifEmpty { null } ?: "Medicamento",
                            dose = dose,
                            notes = note(row),
                        )
                    }
                }
                "growth" -> {
                    val lengthCol = column("largo", "length", "altura", "height")
                    val weightCol = column("peso", "weight")
                    val headCol = column("cabeza", "head")
                    data.forEach { row ->
                        val at = parseDateTime(row.getOrElse(1) { "" }, dayFirst)
                            ?: return@forEach skip("Medidas sin fecha válida")
                        val entry = GrowthEntity(
                            babyId = 0,
                            date = at.toLocalDate(),
                            weightKg = row.getOrNull(weightCol)?.let(::decimal),
                            lengthCm = row.getOrNull(lengthCol)?.let(::decimal),
                            headCm = row.getOrNull(headCol)?.let(::decimal),
                            notes = note(row),
                        )
                        if (entry.weightKg == null && entry.lengthCm == null && entry.headCm == null) {
                            skip("Medidas vacías")
                        } else {
                            growth += entry
                        }
                    }
                }
                "vaccine", "vaccination" -> {
                    val nameCol = column("vacuna", "vaccine")
                    data.forEach { row ->
                        val at = time(row) ?: return@forEach skip("Vacunas sin fecha válida")
                        appointments += AppointmentEntity(
                            babyId = 0,
                            time = at,
                            kind = AppointmentKind.VACCINE,
                            title = row.getOrNull(nameCol)?.trim()?.ifEmpty { null } ?: "Vacuna",
                            notes = note(row),
                            done = true,
                        )
                    }
                }
                "milestone" -> {
                    val nameCol = column("momento", "milestone", "hito")
                    data.forEach { row ->
                        val at = time(row) ?: return@forEach skip("Hitos sin fecha válida")
                        milestones += milestone(row.getOrNull(nameCol)?.trim().orEmpty(), note(row), at)
                    }
                }
                else -> {
                    // Tipos sin formato verificado (pecho, sueño…): se intenta con el analizador general,
                    // añadiendo el tipo del nombre del archivo como palabra clave.
                    val records = data.map { row -> "$kind · " + row.drop(1).joinToString(" · ") }
                    val result = RecordParser.parse(records, zone)
                    events += result.events.map { it.copy(notes = null) }
                    skip("Filas no reconocidas de $fileName", result.unrecognized.size)
                }
            }
        }
        return Result(
            events = events.distinctBy { it.importKey() }.sortedBy { it.startTime },
            growth = growth.distinctBy { it.date }.sortedBy { it.date },
            appointments = appointments,
            milestones = milestones,
            babyNames = names,
            skipped = skipped,
        )
    }

    /**
     * Traduce el nombre del hito de Baby Tracker a un tipo de la app. Para los tipos que piden
     * "cuál" (operación, primera palabra…) la nota de Baby Tracker se usa como detalle.
     */
    fun milestone(name: String, note: String?, time: Long): MilestoneEntity {
        val n = RecordParser.normalize(name)
        val kind = when {
            "cordon" in n || "umbilical" in n || "cord" in n -> MilestoneKind.UMBILICAL_CORD
            "diente" in n || "tooth" in n -> MilestoneKind.FIRST_TOOTH
            "operacion" in n || "cirugia" in n || "surgery" in n || "operation" in n -> MilestoneKind.SURGERY
            "palabra" in n || "word" in n -> MilestoneKind.FIRST_WORD
            "carcajada" in n || "risa" in n || "laugh" in n -> MilestoneKind.FIRST_LAUGH
            "sonrisa" in n || "smile" in n -> MilestoneKind.FIRST_SMILE
            "paso" in n || "camin" in n || "step" in n || "walk" in n -> MilestoneKind.FIRST_STEPS
            "gate" in n || "crawl" in n -> MilestoneKind.CRAWLS
            "sienta" in n || "sentad" in n || "sit" in n -> MilestoneKind.SITS
            "vuelta" in n || "roll" in n -> MilestoneKind.ROLLS_OVER
            "cabeza" in n || "head" in n -> MilestoneKind.HOLDS_HEAD
            "comida" in n || "solido" in n || "food" in n -> MilestoneKind.FIRST_FOOD
            else -> MilestoneKind.OTHER
        }
        val asksDetail = kind in setOf(MilestoneKind.SURGERY, MilestoneKind.FIRST_WORD, MilestoneKind.FIRST_TOOTH, MilestoneKind.FIRST_FOOD)
        return when {
            kind == MilestoneKind.OTHER -> MilestoneEntity(babyId = 0, time = time, kind = kind, detail = name.ifEmpty { null }, notes = note)
            asksDetail && note != null -> MilestoneEntity(babyId = 0, time = time, kind = kind, detail = note.trimEnd('.', ' '))
            else -> MilestoneEntity(babyId = 0, time = time, kind = kind, notes = note)
        }
    }

    /** Decide si las fechas van día/mes (como en España) o mes/día mirando todo el archivo. */
    private fun isDayFirst(values: List<String>): Boolean {
        var dayFirst = 0
        var monthFirst = 0
        values.forEach { v ->
            val m = dateTime.find(v) ?: return@forEach
            val a = m.groupValues[1].toInt()
            val b = m.groupValues[2].toInt()
            if (a > 12) dayFirst++
            if (b > 12) monthFirst++
        }
        return dayFirst >= monthFirst
    }

    fun parseDateTime(text: String, dayFirst: Boolean): LocalDateTime? {
        val m = dateTime.find(text.trim()) ?: return null
        val a = m.groupValues[1].toInt()
        val b = m.groupValues[2].toInt()
        var year = m.groupValues[3].toInt()
        if (year < 100) year += 2000
        var hour = m.groupValues[4].toInt()
        val minute = m.groupValues[5].toInt()
        when (m.groupValues[6].lowercase()) {
            "p" -> if (hour < 12) hour += 12
            "a" -> if (hour == 12) hour = 0
        }
        val (day, month) = if (dayFirst) a to b else b to a
        return runCatching { LocalDate.of(year, month, day).atTime(hour, minute) }.getOrNull()
    }

    private fun decimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()
}
