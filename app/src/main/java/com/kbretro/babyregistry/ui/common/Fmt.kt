package com.kbretro.babyregistry.ui.common

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formateo de horas, duraciones y edades en español. */
object Fmt {
    private val spanish: Locale = Locale.forLanguageTag("es-ES")
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
    private val shortDateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val dayFormat = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", spanish)
    private val dayYearFormat = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", spanish)
    private val dayMonthFormat = DateTimeFormatter.ofPattern("d MMM", spanish)

    fun dateTime(millis: Long): LocalDateTime =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime()

    fun localDate(millis: Long): LocalDate = dateTime(millis).toLocalDate()

    fun time(millis: Long): String = dateTime(millis).format(timeFormat)

    fun shortDate(date: LocalDate): String = date.format(shortDateFormat)

    /** "Hoy 14:05", "Ayer 23:10" o "3 oct 09:30". */
    fun dayTime(millis: Long, today: LocalDate = LocalDate.now()): String {
        val date = localDate(millis)
        val day = when (date) {
            today -> "Hoy"
            today.minusDays(1) -> "Ayer"
            else -> date.format(dayMonthFormat)
        }
        return "$day ${time(millis)}"
    }

    fun dayHeader(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
        today -> "Hoy"
        today.minusDays(1) -> "Ayer"
        else -> date.format(if (date.year == today.year) dayFormat else dayYearFormat)
            .replaceFirstChar { it.titlecase(spanish) }
    }

    fun temperature(celsius: Double): String = "%.1f °C".format(spanish, celsius)

    /** Número con hasta [decimals] decimales y coma decimal: "4,25". */
    fun decimal(value: Double, decimals: Int = 2): String {
        val text = "%.${decimals}f".format(spanish, value)
        return if (decimals == 0) text else text.trimEnd('0').trimEnd(',')
    }

    /** Interpreta "4,25" o "4.25". */
    fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

    /** "Hoy 14:05", "Mañana 09:30" o "3 oct 09:30" (para fechas futuras). */
    fun dayTimeFuture(millis: Long, today: LocalDate = LocalDate.now()): String {
        val date = localDate(millis)
        val day = when (date) {
            today -> "Hoy"
            today.plusDays(1) -> "Mañana"
            today.minusDays(1) -> "Ayer"
            else -> date.format(dayMonthFormat)
        }
        return "$day ${time(millis)}"
    }

    fun shortDay(date: LocalDate): String = date.format(dayMonthFormat)

    /** Cronómetro: "05:09" o "1:02:03". */
    fun clock(millis: Long): String {
        val totalSeconds = millis.coerceAtLeast(0) / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    /** Duración legible: "45 s", "12 min", "2 h", "1 h 05 min". */
    fun duration(millis: Long): String {
        val safe = millis.coerceAtLeast(0)
        val totalMinutes = safe / 60_000
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return when {
            totalMinutes == 0L -> "${safe / 1000} s"
            h == 0L -> "$m min"
            m == 0L -> "$h h"
            else -> "$h h %02d min".format(m)
        }
    }

    fun ago(millis: Long, now: Long): String =
        if (now - millis < 60_000) "Ahora mismo" else "Hace ${duration(now - millis)}"

    /** Edad: "12 días", "3 meses y 5 días", "1 año y 2 meses". */
    fun age(birthDate: LocalDate, today: LocalDate = LocalDate.now()): String {
        if (today.isBefore(birthDate)) return "Aún no ha nacido"
        val p = Period.between(birthDate, today)
        fun plural(n: Int, one: String, many: String) = "$n ${if (n == 1) one else many}"
        return when {
            p.years > 0 && p.months > 0 -> "${plural(p.years, "año", "años")} y ${plural(p.months, "mes", "meses")}"
            p.years > 0 -> plural(p.years, "año", "años")
            p.months > 0 && p.days > 0 -> "${plural(p.months, "mes", "meses")} y ${plural(p.days, "día", "días")}"
            p.months > 0 -> plural(p.months, "mes", "meses")
            p.days == 0 -> "Recién nacido"
            else -> plural(p.days, "día", "días")
        }
    }

    /** Cambia la hora de [millis] conservando su día; si el resultado es futuro, usa el día anterior. */
    fun withTime(millis: Long, hour: Int, minute: Int, now: Long = System.currentTimeMillis()): Long {
        val zone = ZoneId.systemDefault()
        val date = localDate(millis)
        val candidate = date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
        return if (candidate > now + 60_000) {
            date.minusDays(1).atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
        } else {
            candidate
        }
    }

    /** Fecha y hora futura a partir de un día y una hora concretos. */
    fun at(date: LocalDate, hour: Int, minute: Int): Long =
        date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /** Convierte una hora elegida (h:m) en el instante pasado más reciente con esa hora. */
    fun pastTimeAt(hour: Int, minute: Int, now: Long = System.currentTimeMillis()): Long {
        val zone = ZoneId.systemDefault()
        val today = localDate(now)
        val candidate = today.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
        return if (candidate > now + 60_000) {
            today.minusDays(1).atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
        } else {
            candidate
        }
    }
}
