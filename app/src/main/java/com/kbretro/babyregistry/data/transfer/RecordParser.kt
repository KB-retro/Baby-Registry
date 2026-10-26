package com.kbretro.babyregistry.data.transfer

import com.kbretro.babyregistry.data.model.BottleContent
import com.kbretro.babyregistry.data.model.BreastSide
import com.kbretro.babyregistry.data.model.DiaperKind
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * Reconoce registros en texto exportado por otras apps (líneas de PDF, filas de CSV u objetos JSON).
 *
 * Cada registro necesita una fecha (propia o de una línea anterior que solo tenga fecha, típica de
 * los PDF agrupados por días), una hora y una palabra clave que indique el tipo. Entiende español
 * e inglés. Lo que no reconoce se devuelve aparte para mostrarlo antes de importar.
 */
object RecordParser {

    data class Result(val events: List<EventEntity>, val unrecognized: List<String>)

    private const val MINUTE = 60_000L

    private val isoDate = Regex("""(\d{4})-(\d{1,2})-(\d{1,2})(?:[T ](\d{1,2}):(\d{2})(?::\d{2}(?:\.\d+)?)?)?""")
    private val dmyDate = Regex("""\b(\d{1,2})[/.-](\d{1,2})[/.-](\d{2,4})\b""")
    private val time = Regex("""\b(\d{1,2}):(\d{2})(?::\d{2})?\b(?:\s*([ap])\.?\s?m\b\.?)?""")
    private val amount = Regex("""(\d+(?:[.,]\d+)?)\s*(ml|oz|onzas?)\b""")
    private val hoursMinutes = Regex("""(\d+)\s*h(?:oras?|rs?|ours?)?\s*(?:y\s*)?(\d+)\s*m(?:in(?:utos?|s|utes?)?)?\b""")
    private val hoursOnly = Regex("""(\d+(?:[.,]\d+)?)\s*h(?:oras?|rs?|ours?)?\b""")
    private val minutesOnly = Regex("""(\d+)\s*m(?:in(?:utos?|s|utes?)?)?\b""")
    // Primero "izquierdo 10 min"; si no, "10 min izquierdo".
    private val leftSide = listOf(Regex("""(?:izq\w*|left)\D{0,3}(\d+)\s*m"""), Regex("""(\d+)\s*m\w*\s*(?:izq\w*|left)"""))
    private val rightSide = listOf(Regex("""(?:der\w*|right)\D{0,3}(\d+)\s*m"""), Regex("""(\d+)\s*m\w*\s*(?:der\w*|right)"""))
    private val temperature = Regex("""(\d{2,3}(?:[.,]\d)?)\s*(?:°|º)?\s*([cf])?\b""")

    private val keywords: List<Pair<EventType, List<String>>> = listOf(
        EventType.TEMPERATURE to listOf("temperatura", "temperature", "fiebre", "fever", "°c", "ºc"),
        EventType.MEDICINE to listOf("medicamento", "medicina", "medicacion", "medication", "medicine", "dosis", "dose", "paracetamol", "ibuprofeno", "ibuprofen", "vitamina", "vitamin"),
        EventType.DIAPER to listOf("panal", "diaper", "nappy", "pis", "caca", "pee", "poo", "poop", "wet", "dirty", "mojado", "sucio"),
        EventType.SLEEP to listOf("sueno", "siesta", "dormir", "durmio", "dormido", "sleep", "nap"),
        EventType.BOTTLE to listOf("biberon", "bottle", "formula"),
        EventType.BREAST to listOf("pecho", "breast", "nursing", "lactancia", "amamant", "teta"),
        EventType.SOLID to listOf("solido", "solids", "solid", "comida", "papilla", "pure", "food", "meal"),
    )

    private val keywordPatterns: List<Pair<EventType, List<Regex>>> = keywords.map { (type, words) ->
        type to words.map { word ->
            if (word.first().isLetter()) Regex("""(?<![\p{L}])${Regex.escape(word)}""") else Regex(Regex.escape(word))
        }
    }

    fun parse(records: List<String>, zone: ZoneId = ZoneId.systemDefault()): Result {
        val events = mutableListOf<EventEntity>()
        val unrecognized = mutableListOf<String>()
        var contextDate: LocalDate? = null
        for (raw in records) {
            val record = raw.trim()
            if (record.isEmpty()) continue
            val text = normalize(record)
            val (date, times) = dateAndTimes(text)
            val type = detectType(text)
            if (type == null) {
                // Una línea con fecha y sin tipo es una cabecera de día (PDF agrupados por fecha).
                if (date != null) contextDate = date else unrecognized += record
                continue
            }
            val day = date ?: contextDate
            val start = times.firstOrNull()
            if (day == null || start == null) {
                unrecognized += record
                continue
            }
            val startMillis = day.atTime(start).atZone(zone).toInstant().toEpochMilli()
            val endMillis = times.getOrNull(1)?.let { end ->
                val endDay = if (end.isBefore(start)) day.plusDays(1) else day
                endDay.atTime(end).atZone(zone).toInstant().toEpochMilli()
            }
            val event = build(type, record, text, startMillis, endMillis)
            if (event == null) unrecognized += record else events += event
        }
        val unique = events.distinctBy { it.importKey() }.sortedBy { it.startTime }
        return Result(unique, unrecognized)
    }

    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

    private fun detectType(text: String): EventType? {
        val hasAmount = amount.containsMatchIn(text)
        val type = keywordPatterns.firstOrNull { (_, patterns) -> patterns.any { it.containsMatchIn(text) } }?.first
        return when {
            // "Leche materna 120 ml" es un biberón aunque mencione el pecho.
            type == EventType.BREAST && hasAmount -> EventType.BOTTLE
            type != null -> type
            // "120 ml" sin más contexto: casi siempre es un biberón.
            hasAmount -> EventType.BOTTLE
            else -> null
        }
    }

    private fun dateAndTimes(text: String): Pair<LocalDate?, List<LocalTime>> {
        val found = mutableListOf<Pair<Int, LocalTime>>()
        var date: LocalDate? = null
        var rest = text
        isoDate.findAll(text).forEach { m ->
            val (y, mo, d) = m.destructured
            if (date == null) date = runCatching { LocalDate.of(y.toInt(), mo.toInt(), d.toInt()) }.getOrNull()
            val h = m.groupValues[4]
            if (h.isNotEmpty()) {
                runCatching { LocalTime.of(h.toInt(), m.groupValues[5].toInt()) }.getOrNull()?.let { found += m.range.first to it }
            }
            rest = rest.replaceRange(m.range, " ".repeat(m.value.length))
        }
        dmyDate.findAll(rest).forEach { m ->
            val a = m.groupValues[1].toInt()
            val b = m.groupValues[2].toInt()
            var y = m.groupValues[3].toInt()
            if (y < 100) y += 2000
            // Por defecto día/mes; si el segundo número no puede ser un mes, es formato mes/día.
            val (day, month) = if (b > 12 && a <= 12) b to a else a to b
            if (date == null) date = runCatching { LocalDate.of(y, month, day) }.getOrNull()
            rest = rest.replaceRange(m.range, " ".repeat(m.value.length))
        }
        time.findAll(rest).forEach { m ->
            var h = m.groupValues[1].toInt()
            val min = m.groupValues[2].toInt()
            when (m.groupValues[3]) {
                "p" -> if (h < 12) h += 12
                "a" -> if (h == 12) h = 0
            }
            if (h in 0..23 && min in 0..59) found += m.range.first to LocalTime.of(h, min)
        }
        return date to found.sortedBy { it.first }.map { it.second }
    }

    private fun durationMinutes(text: String): Int? {
        // Las horas tipo "14:05" y las cantidades no son duraciones.
        val clean = text.replace(time, " ").replace(amount, " ")
        hoursMinutes.find(clean)?.let { return it.groupValues[1].toInt() * 60 + it.groupValues[2].toInt() }
        hoursOnly.find(clean)?.let { return (it.groupValues[1].replace(',', '.').toDouble() * 60).roundToInt() }
        minutesOnly.find(clean)?.let { return it.groupValues[1].toInt() }
        return null
    }

    private fun sideMinutes(patterns: List<Regex>, text: String): Int? =
        patterns.firstNotNullOfOrNull { it.find(text)?.groupValues?.get(1)?.toIntOrNull() }

    private fun build(type: EventType, record: String, text: String, start: Long, end: Long?): EventEntity? {
        val base = EventEntity(babyId = 0, type = type, startTime = start, endTime = start, notes = "Importado: ${record.take(120)}")
        return when (type) {
            EventType.BOTTLE -> {
                val ml = amount.find(text)?.let { m ->
                    val value = m.groupValues[1].replace(',', '.').toDouble()
                    if (m.groupValues[2] == "ml") value.roundToInt() else (value * 29.5735).roundToInt()
                }
                val content = when {
                    listOf("materna", "breast", "pecho", "extraida", "expressed").any { text.contains(it) } -> BottleContent.BREAST_MILK
                    else -> BottleContent.FORMULA
                }
                base.copy(amountMl = ml, bottleContent = content)
            }
            EventType.BREAST -> {
                val left = sideMinutes(leftSide, text)
                val right = sideMinutes(rightSide, text)
                val total = durationMinutes(text) ?: end?.let { ((it - start) / MINUTE).toInt() }
                val (l, r) = when {
                    left != null || right != null -> (left ?: 0) to (right ?: 0)
                    total != null -> total to 0
                    else -> return null
                }
                if (l + r <= 0) return null
                val lastSide = when {
                    r > 0 && l == 0 -> BreastSide.RIGHT
                    l > 0 && r == 0 && left != null -> BreastSide.LEFT
                    else -> null
                }
                base.copy(
                    endTime = start + (l + r) * MINUTE,
                    leftMillis = l * MINUTE,
                    rightMillis = r * MINUTE,
                    lastSide = lastSide,
                )
            }
            EventType.SLEEP -> {
                val finish = end ?: durationMinutes(text)?.let { start + it * MINUTE } ?: return null
                if (finish <= start) return null
                base.copy(endTime = finish)
            }
            EventType.DIAPER -> {
                val both = listOf("pis y caca", "ambos", "mixto", "both", "mixed", "wet and dirty", "pee and poo").any { text.contains(it) }
                val dirty = listOf("caca", "poo", "dirty", "sucio", "heces", "stool").any { text.contains(it) }
                base.copy(diaperKind = if (both) DiaperKind.MIXED else if (dirty) DiaperKind.DIRTY else DiaperKind.WET)
            }
            EventType.TEMPERATURE -> {
                val celsius = temperature.findAll(text.replace(time, " "))
                    .mapNotNull { m ->
                        val v = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return@mapNotNull null
                        when {
                            v in 34.0..43.0 -> v
                            v in 93.0..110.0 -> (v - 32) * 5 / 9
                            else -> null
                        }
                    }
                    .firstOrNull() ?: return null
                base.copy(temperatureC = (celsius * 10).roundToInt() / 10.0)
            }
            EventType.MEDICINE -> base.copy(medicineName = describe(record), dose = amount.find(text)?.value)
            EventType.SOLID -> base.copy(food = describe(record))
        }
    }

    /** Texto del registro sin fechas ni horas, para usar como nombre de alimento o medicamento. */
    private fun describe(record: String): String =
        record.replace(isoDate, " ").replace(dmyDate, " ").replace(time, " ")
            .replace(Regex("\\s*[·|;,]\\s*"), " ").replace(Regex("\\s+"), " ").trim().take(60).ifEmpty { "Importado" }
}
