package com.kbretro.babyregistry.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.kbretro.babyregistry.data.model.AppointmentEntity
import com.kbretro.babyregistry.data.model.BabyEntity
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.data.model.GrowthEntity
import com.kbretro.babyregistry.data.repository.TrackerRepository
import com.kbretro.babyregistry.domain.DayRange
import com.kbretro.babyregistry.domain.DayStats
import com.kbretro.babyregistry.domain.GrowthIndicator
import com.kbretro.babyregistry.domain.WhoGrowth
import com.kbretro.babyregistry.domain.averageOf
import com.kbretro.babyregistry.domain.dailyStats
import com.kbretro.babyregistry.ui.common.Fmt
import com.kbretro.babyregistry.ui.common.detail
import com.kbretro.babyregistry.ui.common.label
import com.kbretro.babyregistry.ui.common.title
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/** Datos del informe para el pediatra. */
data class ReportData(
    val baby: BabyEntity,
    val days: List<DayStats>,
    val growth: List<GrowthEntity>,
    val health: List<EventEntity>,
    val appointments: List<AppointmentEntity>,
    val generatedAt: Long,
)

/** Genera un informe PDF de los últimos días y lo deja listo para compartir. */
@Singleton
class ReportExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: TrackerRepository,
) {
    suspend fun export(rangeDays: Int): Uri? = withContext(Dispatchers.IO) {
        val data = collect(rangeDays) ?: return@withContext null
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val safeName = data.baby.name.replace(Regex("[^\\p{L}\\p{N}]+"), "-").trim('-').ifEmpty { "bebe" }
        val file = File(dir, "informe-$safeName-${LocalDate.now()}.pdf")
        PdfRenderer(data).writeTo(file)
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    private suspend fun collect(rangeDays: Int): ReportData? {
        val baby = repo.currentBaby.first() ?: return null
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        val dates = (rangeDays - 1 downTo 0).map { today.minusDays(it.toLong()) }
        val from = DayRange.of(dates.first()).first
        val to = DayRange.of(today).second
        val events = repo.overlapping(baby.id, from, to).first()
        return ReportData(
            baby = baby,
            days = dailyStats(events, dates, now),
            growth = repo.growth(baby.id).first(),
            health = events.filter { it.type in EventType.health }.sortedBy { it.startTime },
            appointments = repo.appointments(baby.id).first().filter { !it.done && it.time >= now },
            generatedAt = now,
        )
    }
}

/** Dibuja el informe en páginas A4 con un cursor vertical sencillo. */
private class PdfRenderer(private val data: ReportData) {
    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 40f
    private val document = PdfDocument()
    private var page: PdfDocument.Page? = null
    private var pageNumber = 0
    private var y = 0f

    private val titlePaint = paint(20f, bold = true)
    private val headingPaint = paint(14f, bold = true)
    private val textPaint = paint(10.5f)
    private val mutedPaint = paint(9f).apply { color = 0xFF666666.toInt() }
    private val linePaint = Paint().apply { color = 0xFFDDDDDD.toInt(); strokeWidth = 1f }

    private val canvas: Canvas get() = page!!.canvas

    fun writeTo(file: File) {
        newPage()
        header()
        summary()
        dailyTable()
        growth()
        health()
        appointments()
        footer()
        page?.let(document::finishPage)
        file.outputStream().use(document::writeTo)
        document.close()
    }

    private fun paint(size: Float, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        color = 0xFF1C1B1F.toInt()
        typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun newPage() {
        page?.let(document::finishPage)
        pageNumber++
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        y = margin
        canvas.drawText("Página $pageNumber", pageWidth - margin - 50f, pageHeight - 20f, mutedPaint)
    }

    private fun ensureSpace(height: Float) {
        if (y + height > pageHeight - margin) newPage()
    }

    private fun text(value: String, paint: Paint = textPaint, x: Float = margin, advance: Float = paint.textSize * 1.5f) {
        ensureSpace(advance)
        y += paint.textSize
        canvas.drawText(fit(value, paint, pageWidth - margin - x), x, y, paint)
        y += advance - paint.textSize
    }

    /** Recorta el texto con "…" si no cabe en [width]. */
    private fun fit(value: String, paint: Paint, width: Float): String {
        if (paint.measureText(value) <= width) return value
        var end = value.length
        while (end > 0 && paint.measureText(value.substring(0, end) + "…") > width) end--
        return value.substring(0, end).trimEnd() + "…"
    }

    private fun heading(value: String) {
        y += 10f
        ensureSpace(40f)
        text(value, headingPaint)
        canvas.drawLine(margin, y - 4f, pageWidth - margin, y - 4f, linePaint)
        y += 4f
    }

    private fun row(columns: List<String>, widths: List<Float>, paint: Paint = textPaint) {
        ensureSpace(paint.textSize * 1.6f)
        y += paint.textSize
        var x = margin
        columns.forEachIndexed { i, value ->
            canvas.drawText(value, x, y, paint)
            x += widths[i]
        }
        y += paint.textSize * 0.6f
    }

    private fun header() {
        val baby = data.baby
        text("Informe de ${baby.name}", titlePaint, advance = 30f)
        text("Nacimiento: ${Fmt.shortDate(baby.birthDate)} · Edad: ${Fmt.age(baby.birthDate)} · Sexo: ${baby.sex.label}")
        val first = data.days.first().date
        val last = data.days.last().date
        text("Periodo: ${Fmt.shortDate(first)} – ${Fmt.shortDate(last)} (${data.days.size} días)", mutedPaint)
    }

    private fun summary() {
        // Las medias excluyen el día de hoy, que aún no ha terminado.
        val full = data.days.filter { it.date != LocalDate.now() }.ifEmpty { data.days }
        heading("Media diaria")
        text("Tomas: ${Fmt.decimal(full.averageOf { it.summary.feeds.toDouble() } ?: 0.0, 1)}")
        text("Biberón: ${(full.averageOf { it.summary.bottleMl.toDouble() } ?: 0.0).roundToInt()} ml")
        text("Pecho: ${Fmt.duration((full.averageOf { it.summary.breastMillis.toDouble() } ?: 0.0).toLong())}")
        text("Sueño: ${Fmt.duration((full.averageOf { it.summary.sleepMillis.toDouble() } ?: 0.0).toLong())}")
        text(
            "Pañales: ${Fmt.decimal(full.averageOf { it.summary.diapers.toDouble() } ?: 0.0, 1)} " +
                "(pis ${Fmt.decimal(full.averageOf { it.summary.wet.toDouble() } ?: 0.0, 1)}, " +
                "caca ${Fmt.decimal(full.averageOf { it.summary.dirty.toDouble() } ?: 0.0, 1)})",
        )
    }

    private fun dailyTable() {
        heading("Día a día")
        val widths = listOf(90f, 55f, 65f, 70f, 85f, 70f, 70f)
        row(listOf("Día", "Tomas", "Biberón", "Pecho", "Sueño", "Pis", "Caca"), widths, mutedPaint)
        data.days.reversed().forEach { d ->
            val s = d.summary
            row(
                listOf(
                    Fmt.shortDay(d.date),
                    s.feeds.toString(),
                    if (s.bottleMl > 0) "${s.bottleMl} ml" else "–",
                    if (s.breastMillis > 0) Fmt.duration(s.breastMillis) else "–",
                    if (s.sleepMillis > 0) Fmt.duration(s.sleepMillis) else "–",
                    s.wet.toString(),
                    s.dirty.toString(),
                ),
                widths,
            )
        }
    }

    private fun growth() {
        if (data.growth.isEmpty()) return
        heading("Crecimiento (percentiles OMS)")
        val widths = listOf(80f, 110f, 110f, 110f, 100f)
        row(listOf("Fecha", "Peso", "Talla", "P. craneal", "Edad"), widths, mutedPaint)
        data.growth.take(8).forEach { g ->
            fun cell(indicator: GrowthIndicator, decimals: Int): String {
                val value = indicator.valueOf(g) ?: return "–"
                val table = WhoGrowth.table(indicator, data.baby.sex)
                val percentile = table?.let {
                    WhoGrowth.zScore(it, WhoGrowth.ageInMonths(data.baby.birthDate, g.date), value)?.let(WhoGrowth::percentile)
                }
                val p = percentile?.let { " (P${it.toInt().coerceIn(1, 99)})" }.orEmpty()
                return "${Fmt.decimal(value, decimals)} ${indicator.unit}$p"
            }
            row(
                listOf(
                    Fmt.shortDate(g.date),
                    cell(GrowthIndicator.WEIGHT, 3),
                    cell(GrowthIndicator.LENGTH, 1),
                    cell(GrowthIndicator.HEAD, 1),
                    Fmt.age(data.baby.birthDate, g.date),
                ),
                widths,
            )
        }
    }

    private fun health() {
        if (data.health.isEmpty()) return
        heading("Temperatura y medicación")
        data.health.forEach { e ->
            text("${Fmt.dayTime(e.startTime)} · ${e.title()}: ${e.detail(data.generatedAt)}")
        }
    }

    private fun appointments() {
        if (data.appointments.isEmpty()) return
        heading("Próximas citas")
        data.appointments.forEach { a ->
            text("${Fmt.dayTimeFuture(a.time)} · ${a.kind.label}: ${a.title}")
        }
    }

    private fun footer() {
        y += 16f
        text("Generado con Baby Registry el ${Fmt.dayTime(data.generatedAt)}.", mutedPaint)
        text("Los percentiles usan los Patrones de Crecimiento Infantil de la OMS.", mutedPaint)
        text("Este informe es orientativo y no sustituye la valoración médica.", mutedPaint)
    }
}
