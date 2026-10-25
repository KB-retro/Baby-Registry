package com.kbretro.babyregistry.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kbretro.babyregistry.domain.DayPattern
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

data class BarSeries(val name: String, val color: Color, val values: List<Float>)

/** Redondea hacia arriba a un valor "bonito" para el eje (1, 2, 2,5, 5 × 10ⁿ). */
fun niceMax(value: Float): Float {
    if (value <= 0f) return 1f
    val magnitude = 10.0.pow(floor(log10(value.toDouble()))).toFloat()
    return listOf(1f, 2f, 2.5f, 5f, 10f).map { it * magnitude }.first { it >= value }
}

@Composable
fun ChartLegend(items: List<Pair<String, Color>>, modifier: Modifier = Modifier, round: Boolean = false) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        items.forEach { (name, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(if (round) CircleShape else RoundedCornerShape(2.dp))
                        .background(color),
                )
                Spacer(Modifier.width(6.dp))
                Text(name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun DrawScope.drawLabel(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    x: Float,
    y: Float,
    alignX: Float = 0.5f,
    alignY: Float = 0.5f,
) {
    val layout = measurer.measure(text, style)
    drawText(layout, topLeft = Offset(x - layout.size.width * alignX, y - layout.size.height * alignY))
}

/**
 * Gráfico de barras por día. Con varias series, las apila con un hueco de 2 dp entre segmentos.
 * Tocar una barra muestra su valor en la cabecera.
 */
@Composable
fun BarChart(
    series: List<BarSeries>,
    labels: List<String>,
    axisLabels: List<String>,
    format: (Float) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 170.dp,
) {
    val n = labels.size
    if (n == 0 || series.isEmpty()) return
    var selected by remember(n) { mutableIntStateOf(n - 1) }
    val totals = List(n) { i -> series.sumOf { it.values[i].toDouble() }.toFloat() }
    val maxValue = niceMax(totals.max())
    val measurer = rememberTextMeasurer()
    val axisStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val sel = selected.coerceIn(0, n - 1)

    Column(modifier) {
        Text(labels[sel], style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(format(totals[sel]), style = MaterialTheme.typography.titleMedium)
        if (series.size > 1) {
            Text(
                series.joinToString(" · ") { "${it.name} ${format(it.values[sel])}" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(n) {
                    detectTapGestures { offset ->
                        val left = 36.dp.toPx()
                        val slot = (size.width - left) / n
                        selected = ((offset.x - left) / slot).toInt().coerceIn(0, n - 1)
                    }
                },
        ) {
            val left = 36.dp.toPx()
            val bottom = 20.dp.toPx()
            val plotW = size.width - left
            val plotH = size.height - bottom
            val gap = 2.dp.toPx()
            val radius = 4.dp.toPx()

            // Rejilla discreta: 0, mitad y máximo.
            listOf(0f, maxValue / 2, maxValue).forEach { v ->
                val y = plotH - v / maxValue * plotH
                drawLine(gridColor, Offset(left, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                drawLabel(measurer, format(v), axisStyle, left - 6.dp.toPx(), y, alignX = 1f)
            }

            val slot = plotW / n
            val barW = minOf(slot * 0.62f, 28.dp.toPx())
            for (i in 0 until n) {
                val cx = left + slot * (i + 0.5f)
                val alpha = if (i == sel) 1f else 0.55f
                var base = plotH
                val nonZero = series.indices.filter { series[it].values[i] > 0f }
                nonZero.forEachIndexed { k, s ->
                    val h = series[s].values[i] / maxValue * plotH
                    val top = base - h
                    val segmentBottom = if (k == 0) base else base - gap
                    if (segmentBottom > top) {
                        val isTop = k == nonZero.lastIndex
                        val r = if (isTop) CornerRadius(radius) else CornerRadius.Zero
                        val path = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    rect = Rect(cx - barW / 2, top, cx + barW / 2, segmentBottom),
                                    topLeft = r,
                                    topRight = r,
                                    bottomRight = CornerRadius.Zero,
                                    bottomLeft = CornerRadius.Zero,
                                ),
                            )
                        }
                        drawPath(path, series[s].color.copy(alpha = alpha))
                    }
                    base = top
                }
                val label = axisLabels.getOrNull(i).orEmpty()
                if (label.isNotEmpty()) {
                    drawLabel(measurer, label, axisStyle, cx, plotH + 4.dp.toPx(), alignY = 0f)
                }
            }
        }
        if (series.size > 1) {
            Spacer(Modifier.height(8.dp))
            ChartLegend(series.map { it.name to it.color })
        }
    }
}

/**
 * Patrón de 24 h: una fila por día con los tramos de sueño y un punto por cada toma.
 * [ringColor] debe ser el color de fondo de la tarjeta (anillo de 2 dp alrededor de los puntos).
 */
@Composable
fun SleepPatternChart(
    days: List<DayPattern>,
    dayLabel: (DayPattern) -> String,
    sleepColor: Color,
    feedColor: Color,
    ringColor: Color,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val axisStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val rowHeight = 20.dp
    val rowGap = 6.dp
    val topAxis = 18.dp

    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(topAxis + (rowHeight + rowGap) * days.size),
        ) {
            val labelW = 48.dp.toPx()
            val trackW = size.width - labelW
            val rowH = rowHeight.toPx()
            val gapPx = rowGap.toPx()
            val top = topAxis.toPx()

            for (h in 0..24 step 6) {
                val x = labelW + trackW * h / 24f
                drawLabel(measurer, "${h}h", axisStyle, x.coerceIn(labelW + 8.dp.toPx(), size.width - 10.dp.toPx()), top / 2)
                drawLine(gridColor, Offset(x, top), Offset(x, size.height), strokeWidth = 1.dp.toPx())
            }
            days.forEachIndexed { row, day ->
                val y = top + row * (rowH + gapPx) + gapPx / 2
                drawLabel(measurer, dayLabel(day), axisStyle, 0f, y + rowH / 2, alignX = 0f)
                drawRoundRect(
                    trackColor.copy(alpha = 0.6f),
                    topLeft = Offset(labelW, y),
                    size = Size(trackW, rowH),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
                day.sleeps.forEach { segment ->
                    val x0 = labelW + segment.start * trackW
                    val x1 = labelW + segment.end * trackW
                    drawRoundRect(
                        sleepColor,
                        topLeft = Offset(x0, y),
                        size = Size(maxOf(x1 - x0, 2.dp.toPx()), rowH),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                    )
                }
                day.feeds.forEach { f ->
                    val center = Offset(labelW + f * trackW, y + rowH / 2)
                    drawCircle(ringColor, radius = 6.dp.toPx(), center = center)
                    drawCircle(feedColor, radius = 4.dp.toPx(), center = center)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        ChartLegend(listOf("Sueño" to sleepColor, "Toma" to feedColor), round = false)
    }
}

/** Divide [count] etiquetas para que no se amontonen: muestra 1 de cada [every]. */
fun sparseLabels(labels: List<String>, maxVisible: Int = 8): List<String> {
    if (labels.size <= maxVisible) return labels
    val every = ceil(labels.size / maxVisible.toFloat()).toInt()
    return labels.mapIndexed { i, l -> if ((labels.size - 1 - i) % every == 0) l else "" }
}
