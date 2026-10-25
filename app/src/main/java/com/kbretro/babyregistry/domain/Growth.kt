package com.kbretro.babyregistry.domain

import com.kbretro.babyregistry.data.model.GrowthEntity
import com.kbretro.babyregistry.data.model.Sex
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

/** Parámetros LMS de la OMS por mes de edad (índice 0 = nacimiento). */
class Lms(val l: DoubleArray, val m: DoubleArray, val s: DoubleArray)

enum class GrowthIndicator(val label: String, val unit: String) {
    WEIGHT("Peso", "kg"),
    LENGTH("Talla", "cm"),
    HEAD("Perímetro craneal", "cm");

    fun valueOf(entry: GrowthEntity): Double? = when (this) {
        WEIGHT -> entry.weightKg
        LENGTH -> entry.lengthCm
        HEAD -> entry.headCm
    }
}

/** Curvas de crecimiento de la OMS (0-5 años) con el método LMS. */
object WhoGrowth {
    const val MAX_MONTHS = 60
    private const val DAYS_PER_MONTH = 30.4375

    /** Percentiles que se dibujan y su puntuación z. */
    val percentiles: List<Pair<Int, Double>> = listOf(
        3 to -1.8808,
        15 to -1.0364,
        50 to 0.0,
        85 to 1.0364,
        97 to 1.8808,
    )

    fun table(indicator: GrowthIndicator, sex: Sex): Lms? = when (sex) {
        Sex.BOY -> when (indicator) {
            GrowthIndicator.WEIGHT -> WhoTables.WEIGHT_BOYS
            GrowthIndicator.LENGTH -> WhoTables.LENGTH_BOYS
            GrowthIndicator.HEAD -> WhoTables.HEAD_BOYS
        }
        Sex.GIRL -> when (indicator) {
            GrowthIndicator.WEIGHT -> WhoTables.WEIGHT_GIRLS
            GrowthIndicator.LENGTH -> WhoTables.LENGTH_GIRLS
            GrowthIndicator.HEAD -> WhoTables.HEAD_GIRLS
        }
        Sex.UNSPECIFIED -> null
    }

    fun ageInMonths(birthDate: LocalDate, date: LocalDate): Double =
        ChronoUnit.DAYS.between(birthDate, date) / DAYS_PER_MONTH

    private fun lmsAt(table: Lms, months: Double): Triple<Double, Double, Double>? {
        if (months < 0 || months > MAX_MONTHS) return null
        val i = floor(months).toInt().coerceAtMost(MAX_MONTHS)
        val j = (i + 1).coerceAtMost(MAX_MONTHS)
        val f = months - i
        fun lerp(a: DoubleArray) = a[i] + (a[j] - a[i]) * f
        return Triple(lerp(table.l), lerp(table.m), lerp(table.s))
    }

    /** Medida correspondiente a la puntuación [z] a una edad dada. */
    fun valueAt(table: Lms, months: Double, z: Double): Double? {
        val (l, m, s) = lmsAt(table, months) ?: return null
        return if (abs(l) < 1e-9) m * exp(s * z) else m * (1 + l * s * z).pow(1 / l)
    }

    fun zScore(table: Lms, months: Double, value: Double): Double? {
        if (value <= 0) return null
        val (l, m, s) = lmsAt(table, months) ?: return null
        return if (abs(l) < 1e-9) ln(value / m) / s else ((value / m).pow(l) - 1) / (l * s)
    }

    /** Percentil (0-100) de una puntuación z. */
    fun percentile(z: Double): Double = normalCdf(z) * 100

    // Aproximación de Abramowitz y Stegun 7.1.26 (error < 1.5e-7).
    private fun normalCdf(z: Double): Double {
        val x = abs(z) / sqrt(2.0)
        val t = 1 / (1 + 0.3275911 * x)
        val poly = t * (0.254829592 + t * (-0.284496736 + t * (1.421413741 + t * (-1.453152027 + t * 1.061405429))))
        val erf = 1 - poly * exp(-x * x)
        return if (z >= 0) (1 + erf) / 2 else (1 - erf) / 2
    }
}
