package com.kbretro.babyregistry.ui.stats

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.domain.DayStats
import com.kbretro.babyregistry.domain.averageOf
import com.kbretro.babyregistry.domain.dailyStats
import com.kbretro.babyregistry.domain.dayPattern
import com.kbretro.babyregistry.ui.common.BarChart
import com.kbretro.babyregistry.ui.common.BarSeries
import com.kbretro.babyregistry.ui.common.ChoiceRow
import com.kbretro.babyregistry.ui.common.Fmt
import com.kbretro.babyregistry.ui.common.SleepPatternChart
import com.kbretro.babyregistry.ui.common.rememberNow
import com.kbretro.babyregistry.ui.common.sparseLabels
import com.kbretro.babyregistry.ui.common.style
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val spanish = Locale.forLanguageTag("es-ES")
private val shortDay = DateTimeFormatter.ofPattern("EEE d", spanish)
private val longDay = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", spanish)
private const val HOUR_MS = 3_600_000.0

// Validado con la guía de datos: pareja apta para daltonismo (ΔE ≥ 21) y diferenciada de la marca.
private val WetColor = Color(0xFF4F8EDC)
private val DirtyColor = Color(0xFFC08A2E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val now = rememberNow(60_000L)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }
    fun export() {
        if (exporting) return
        exporting = true
        scope.launch {
            val uri = viewModel.exportReport()
            exporting = false
            if (uri != null) {
                val send = Intent(Intent.ACTION_SEND)
                    .setType("application/pdf")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(Intent.createChooser(send, "Compartir informe"))
            }
        }
    }
    val stats = remember(state.events, state.days, now) { dailyStats(state.events, state.days, now) }
    val patterns = remember(state.events, state.days, now) {
        state.days.takeLast(minOf(state.days.size, 14)).reversed().map { dayPattern(state.events, it, now) }
    }
    val today = LocalDate.now()
    val labels = state.days.map { Fmt.dayHeader(it, today).let { h -> if (h == "Hoy" || h == "Ayer") h else it.format(longDay) } }
    val axis = sparseLabels(state.days.map { it.format(shortDay).take(5) })
    // Las medias usan los días completos (sin hoy) cuando hay más de uno.
    val fullDays = stats.filter { it.date != today }.ifEmpty { stats }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Gráficas") },
                actions = {
                    if (exporting) {
                        CircularProgressIndicator(Modifier.padding(12.dp).size(24.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = ::export, enabled = state.events.isNotEmpty()) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = "Exportar informe PDF")
                        }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "range") {
                ChoiceRow(
                    listOf(7 to "7 días", 14 to "14 días", 30 to "30 días"),
                    state.rangeDays,
                    viewModel::setRange,
                )
            }
            if (state.loaded && state.events.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "Aún no hay datos en este periodo. Registra tomas, sueño y pañales para ver tus gráficas.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return@LazyColumn
            }
            item(key = "averages") { Averages(fullDays) }
            item(key = "feeds") {
                ChartCard("Tomas al día") {
                    BarChart(
                        series = listOf(BarSeries("Tomas", EventType.BREAST.style().color, stats.map { it.summary.feeds.toFloat() })),
                        labels = labels,
                        axisLabels = axis,
                        format = { it.roundToInt().toString() },
                    )
                }
            }
            if (stats.any { it.summary.bottleMl > 0 }) {
                item(key = "ml") {
                    ChartCard("Biberón (ml al día)") {
                        BarChart(
                            series = listOf(BarSeries("ml", EventType.BOTTLE.style().color, stats.map { it.summary.bottleMl.toFloat() })),
                            labels = labels,
                            axisLabels = axis,
                            format = { "${it.roundToInt()} ml" },
                        )
                    }
                }
            }
            item(key = "sleep") {
                ChartCard("Sueño al día") {
                    BarChart(
                        series = listOf(
                            BarSeries("Sueño", EventType.SLEEP.style().color, stats.map { (it.summary.sleepMillis / HOUR_MS).toFloat() }),
                        ),
                        labels = labels,
                        axisLabels = axis,
                        format = { Fmt.duration((it * HOUR_MS).toLong()) },
                    )
                }
            }
            item(key = "pattern") {
                val ring = MaterialTheme.colorScheme.surfaceContainerHighest
                ChartCard("Patrón de sueño y tomas", subtitle = "Cada fila es un día, de 0 h a 24 h") {
                    SleepPatternChart(
                        days = patterns,
                        dayLabel = { Fmt.dayHeader(it.date, today).let { h -> if (h == "Hoy" || h == "Ayer") h else it.date.format(shortDay) } },
                        sleepColor = EventType.SLEEP.style().color,
                        feedColor = EventType.BREAST.style().color,
                        ringColor = ring,
                    )
                }
            }
            item(key = "diapers") {
                ChartCard("Pañales al día") {
                    BarChart(
                        series = listOf(
                            BarSeries("Pis", WetColor, stats.map { it.summary.wet.toFloat() }),
                            BarSeries("Caca", DirtyColor, stats.map { it.summary.dirty.toFloat() }),
                        ),
                        labels = labels,
                        axisLabels = axis,
                        format = { it.roundToInt().toString() },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartCard(title: String, subtitle: String? = null, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

@Composable
private fun Averages(days: List<DayStats>) {
    val feeds = days.averageOf { it.summary.feeds.toDouble() } ?: 0.0
    val ml = days.averageOf { it.summary.bottleMl.toDouble() } ?: 0.0
    val sleep = days.averageOf { it.summary.sleepMillis.toDouble() } ?: 0.0
    val diapers = days.averageOf { it.summary.diapers.toDouble() } ?: 0.0
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Media diaria", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AverageTile("Tomas", Fmt.decimal(feeds, 1), Modifier.weight(1f))
            AverageTile("Biberón", "${ml.roundToInt()} ml", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AverageTile("Sueño", Fmt.duration(sleep.toLong()), Modifier.weight(1f))
            AverageTile("Pañales", Fmt.decimal(diapers, 1), Modifier.weight(1f))
        }
    }
}

@Composable
private fun AverageTile(label: String, value: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
