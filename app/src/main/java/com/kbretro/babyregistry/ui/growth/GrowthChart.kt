package com.kbretro.babyregistry.ui.growth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.kbretro.babyregistry.data.model.GrowthEntity
import com.kbretro.babyregistry.domain.GrowthIndicator
import com.kbretro.babyregistry.domain.Lms
import com.kbretro.babyregistry.domain.WhoGrowth
import com.kbretro.babyregistry.ui.common.ChartLegend
import com.kbretro.babyregistry.ui.common.Fmt
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

private data class Point(val months: Double, val value: Double, val entry: GrowthEntity)

/**
 * Curvas de percentiles de la OMS (P3, P15, P50, P85, P97) con las medidas del bebé encima.
 * Sin [table] (sexo sin indicar) solo se dibujan las medidas.
 */
@Composable
fun GrowthChart(
    indicator: GrowthIndicator,
    table: Lms?,
    birthDate: LocalDate,
    entries: List<GrowthEntity>,
    measurementColor: Color,
    ringColor: Color,
    modifier: Modifier = Modifier,
) {
    val points = entries
        .mapNotNull { e -> indicator.valueOf(e)?.let { Point(WhoGrowth.ageInMonths(birthDate, e.date), it, e) } }
        .filter { it.months >= 0 }
        .sortedBy { it.months }
    val currentAge = WhoGrowth.ageInMonths(birthDate, LocalDate.now())
    val lastAge = maxOf(points.maxOfOrNull { it.months } ?: 0.0, currentAge)
    val xMax = (ceil((lastAge + 2) / 3) * 3).coerceIn(6.0, WhoGrowth.MAX_MONTHS.toDouble())
    val xTick = when {
        xMax <= 12 -> 2.0
        xMax <= 24 -> 3.0
        xMax <= 36 -> 6.0
        else -> 12.0
    }

    val curves: List<Pair<Int, List<Pair<Double, Double>>>> = remember(table, xMax) {
        if (table == null) {
            emptyList()
        } else {
            WhoGrowth.percentiles.map { (p, z) ->
                p to generateSequence(0.0) { it + 0.25 }.takeWhile { it <= xMax }
                    .mapNotNull { m -> WhoGrowth.valueAt(table, m, z)?.let { m to it } }
                    .toList()
            }
        }
    }
    val visibleValues = curves.flatMap { c -> c.second.map { it.second } } + points.map { it.value }
    if (visibleValues.isEmpty()) {
        Text(
            "Añade una medida para ver la gráfica.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val step = when (indicator) {
        GrowthIndicator.WEIGHT -> if (xMax <= 12) 1.0 else 2.0
        GrowthIndicator.LENGTH -> 5.0
        GrowthIndicator.HEAD -> 2.0
    }
    val yMin = floor(visibleValues.min() / step) * step
    val yMax = ceil(visibleValues.max() / step) * step + if (curves.isEmpty()) step else 0.0

    var selected by remember(entries, indicator) { mutableStateOf(points.lastOrNull()) }
    val measurer = rememberTextMeasurer()
    val axisStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val curveColor = MaterialTheme.colorScheme.primary

    Column(modifier) {
        selected?.let { p ->
            Text(
                "${Fmt.shortDate(p.entry.date)} · ${Fmt.age(birthDate, p.entry.date)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val percentile = table?.let { t -> WhoGrowth.zScore(t, p.months, p.value)?.let(WhoGrowth::percentile) }
            Text(
                listOfNotNull(
                    "${Fmt.decimal(p.value)} ${indicator.unit}",
                    percentile?.let { "percentil ${formatPercentile(it)}" },
                ).joinToString(" · "),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(260.dp)
                .pointerInput(points, xMax) {
                    detectTapGestures { offset ->
                        val left = 36.dp.toPx()
                        val right = 30.dp.toPx()
                        val plotW = size.width - left - right
                        val months = (offset.x - left) / plotW * xMax
                        selected = points.minByOrNull { abs(it.months - months) } ?: selected
                    }
                },
        ) {
            val left = 36.dp.toPx()
            val right = 30.dp.toPx()
            val bottom = 20.dp.toPx()
            val plotW = size.width - left - right
            val plotH = size.height - bottom
            fun x(m: Double) = left + (m / xMax * plotW).toFloat()
            fun y(v: Double) = plotH - ((v - yMin) / (yMax - yMin) * plotH).toFloat()

            // Rejilla y ejes.
            var v = yMin
            while (v <= yMax + 1e-9) {
                drawLine(gridColor, Offset(left, y(v)), Offset(left + plotW, y(v)), strokeWidth = 1.dp.toPx())
                val layout = measurer.measure(Fmt.decimal(v, 1), axisStyle)
                drawText(layout, topLeft = Offset(left - 6.dp.toPx() - layout.size.width, y(v) - layout.size.height / 2))
                v += step
            }
            var m = 0.0
            while (m <= xMax + 1e-9) {
                val layout = measurer.measure("${m.toInt()} m", axisStyle)
                drawText(layout, topLeft = Offset(x(m) - layout.size.width / 2, plotH + 4.dp.toPx()))
                m += xTick
            }

            // Curvas de la OMS: la mediana más marcada, los extremos discontinuos.
            curves.forEach { (p, values) ->
                if (values.size < 2) return@forEach
                val path = Path().apply {
                    moveTo(x(values.first().first), y(values.first().second))
                    values.drop(1).forEach { (mm, vv) -> lineTo(x(mm), y(vv)) }
                }
                val stroke = when (p) {
                    50 -> Stroke(width = 2.5.dp.toPx())
                    3, 97 -> Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                    else -> Stroke(width = 1.5.dp.toPx())
                }
                drawPath(path, curveColor.copy(alpha = if (p == 50) 0.9f else 0.45f), style = stroke)
                val end = values.last()
                val layout = measurer.measure("P$p", axisStyle)
                drawText(layout, topLeft = Offset(x(end.first) + 4.dp.toPx(), y(end.second) - layout.size.height / 2))
            }

            // Medidas del bebé.
            if (points.size >= 2) {
                val path = Path().apply {
                    moveTo(x(points.first().months), y(points.first().value))
                    points.drop(1).forEach { lineTo(x(it.months), y(it.value)) }
                }
                drawPath(path, measurementColor, style = Stroke(width = 2.dp.toPx()))
            }
            points.forEach { p ->
                val center = Offset(x(p.months), y(p.value))
                val isSelected = p == selected
                drawCircle(ringColor, radius = (if (isSelected) 8 else 6).dp.toPx(), center = center)
                drawCircle(measurementColor, radius = (if (isSelected) 6 else 4).dp.toPx(), center = center)
            }
        }
        Spacer(Modifier.height(8.dp))
        ChartLegend(
            buildList {
                add("Tu bebé" to measurementColor)
                if (curves.isNotEmpty()) add("Percentiles OMS" to curveColor.copy(alpha = 0.6f))
            },
            round = true,
        )
    }
}

fun formatPercentile(p: Double): String = when {
    p < 1 -> "<1"
    p > 99 -> ">99"
    else -> p.toInt().toString()
}
