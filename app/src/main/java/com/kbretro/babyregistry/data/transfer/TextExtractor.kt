package com.kbretro.babyregistry.data.transfer

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream

/** Lo que contiene un archivo a importar. */
sealed interface FileContent {
    data class Backup(val babies: List<BabyBackup>) : FileContent
    data class Records(val records: List<String>) : FileContent
    data class BabyTracker(val result: BabyTrackerImport.Result) : FileContent
}

/** Lee JSON, CSV, PDF o texto y lo convierte en una copia propia o en registros de texto. */
object TextExtractor {

    fun read(context: Context, uri: Uri, mimeType: String?, fileName: String): FileContent {
        val name = fileName.lowercase()
        return when {
            name.endsWith(".zip") || mimeType in zipTypes -> zip(context, uri)
            name.endsWith(".pdf") || mimeType == "application/pdf" -> FileContent.Records(pdfLines(context, uri))
            name.endsWith(".json") || mimeType == "application/json" -> json(readText(context, uri))
            name.endsWith(".csv") || mimeType == "text/csv" || mimeType == "text/comma-separated-values" ->
                csvFiles(listOf(fileName to readText(context, uri)))
            else -> {
                val text = readText(context, uri)
                if (text.trimStart().startsWith("{") || text.trimStart().startsWith("[")) json(text)
                else FileContent.Records(text.lines())
            }
        }
    }

    private val zipTypes = setOf("application/zip", "application/x-zip-compressed", "application/x-zip")

    /** Un .zip con varios CSV (como la exportación de Baby Tracker) o con JSON/texto. */
    private fun zip(context: Context, uri: Uri): FileContent {
        val files = mutableListOf<Pair<String, String>>()
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                generateSequence { zip.nextEntry }.forEach { entry ->
                    val entryName = entry.name.substringAfterLast('/')
                    val supported = entryName.lowercase().let { it.endsWith(".csv") || it.endsWith(".json") || it.endsWith(".txt") }
                    if (!entry.isDirectory && supported && !entryName.startsWith(".")) {
                        files += entryName to zip.readBytes().toString(Charsets.UTF_8)
                    }
                }
            }
        }
        val json = files.filter { it.first.lowercase().endsWith(".json") }
        if (json.size == 1 && files.size == 1) return json(json.single().second)
        return csvFiles(files.filterNot { it.first.lowercase().endsWith(".json") })
    }

    private fun csvFiles(files: List<Pair<String, String>>): FileContent {
        val (babyTracker, others) = files.partition { (name, text) -> BabyTrackerImport.matches(name, text) }
        if (babyTracker.isNotEmpty() && others.isEmpty()) {
            return FileContent.BabyTracker(BabyTrackerImport.parse(babyTracker))
        }
        return FileContent.Records(files.flatMap { (name, text) ->
            if (name.lowercase().endsWith(".csv")) CsvReader.toRecords(text) else text.lines()
        })
    }

    private fun readText(context: Context, uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }.orEmpty()

    private fun pdfLines(context: Context, uri: Uri): List<String> {
        PDFBoxResourceLoader.init(context.applicationContext)
        val stream = context.contentResolver.openInputStream(uri) ?: return emptyList()
        return stream.use { input ->
            PDDocument.load(input).use { document ->
                PDFTextStripper().apply { sortByPosition = true }.getText(document).lines()
            }
        }
    }

    fun json(text: String): FileContent {
        val root = JSONTokener(text).nextValue()
        if (root is JSONObject && BackupJson.isBackup(root)) return FileContent.Backup(BackupJson.read(root))
        val records = mutableListOf<String>()
        collectRecords(root, records)
        return FileContent.Records(records)
    }

    /** Cada objeto JSON que contiene solo valores simples es un registro ("clave valor · clave valor"). */
    private fun collectRecords(node: Any?, out: MutableList<String>) {
        when (node) {
            is JSONArray -> (0 until node.length()).forEach { collectRecords(node.opt(it), out) }
            is JSONObject -> {
                val children = node.keys().asSequence().map { node.opt(it) }.toList()
                if (children.any { it is JSONArray || it is JSONObject }) {
                    val own = flatten(node, onlySimple = true)
                    children.filter { it is JSONArray || it is JSONObject }.forEach { child ->
                        val before = out.size
                        collectRecords(child, out)
                        // Los valores sencillos del padre (p. ej. la fecha del día) se añaden como contexto.
                        if (own.isNotBlank()) for (i in before until out.size) out[i] = "$own · ${out[i]}"
                    }
                } else {
                    out += flatten(node, onlySimple = true)
                }
            }
        }
    }

    private fun flatten(node: JSONObject, onlySimple: Boolean): String =
        node.keys().asSequence()
            .mapNotNull { key ->
                val value = node.opt(key)
                if (onlySimple && (value is JSONArray || value is JSONObject || value == JSONObject.NULL)) null
                else "$key ${epochToText(value) ?: value}"
            }
            .joinToString(" · ")

    /** Muchas apps guardan las horas como epoch (segundos o milisegundos): se pasan a "yyyy-MM-dd HH:mm". */
    private fun epochToText(value: Any?): String? {
        val n = (value as? Number)?.toLong() ?: return null
        val millis = when (n) {
            in 1_000_000_000L..4_000_000_000L -> n * 1000
            in 1_000_000_000_000L..4_000_000_000_000L -> n
            else -> return null
        }
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime().format(epochFormat)
    }

    private val epochFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
}
