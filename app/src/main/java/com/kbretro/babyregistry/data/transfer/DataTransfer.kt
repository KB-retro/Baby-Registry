package com.kbretro.babyregistry.data.transfer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.repository.TrackerRepository
import com.kbretro.babyregistry.reminders.Reminders
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Resultado de analizar un archivo, que se muestra antes de importar nada. */
sealed interface ImportPreview {
    val fileName: String

    /** Copia de seguridad propia: se restauran bebés, registros, medidas y citas. */
    data class Backup(override val fileName: String, val babies: List<BabyBackup>) : ImportPreview

    /** Datos de otra app: registros reconocidos para el bebé seleccionado. */
    data class Foreign(
        override val fileName: String,
        val babyName: String,
        val events: List<EventEntity>,
        val unrecognized: List<String>,
    ) : ImportPreview
}

data class ImportOutcome(val added: Int, val duplicates: Int)

@Singleton
class DataTransfer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: TrackerRepository,
    private val reminders: Reminders,
) {
    /** Escribe la copia completa (todos los bebés) en el archivo elegido por el usuario. */
    suspend fun exportBackup(uri: Uri): Int = withContext(Dispatchers.IO) {
        val babies = repo.babies.first().map { baby ->
            BabyBackup(
                baby = baby,
                events = repo.all(baby.id).first(),
                growth = repo.growth(baby.id).first(),
                appointments = repo.appointments(baby.id).first(),
            )
        }
        val json = BackupJson.write(babies, System.currentTimeMillis())
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray()) }
            ?: error("No se pudo escribir el archivo")
        babies.sumOf { it.events.size }
    }

    suspend fun analyze(uri: Uri): ImportPreview = withContext(Dispatchers.IO) {
        val name = displayName(uri)
        when (val content = TextExtractor.read(context, uri, context.contentResolver.getType(uri), name)) {
            is FileContent.Backup -> ImportPreview.Backup(name, content.babies)
            is FileContent.Records -> {
                val result = RecordParser.parse(content.records)
                val baby = repo.currentBaby.first()
                ImportPreview.Foreign(name, baby?.name.orEmpty(), result.events, result.unrecognized)
            }
        }
    }

    /** Importa sin borrar nada; se omiten los registros que ya existen (mismo tipo y hora). */
    suspend fun apply(preview: ImportPreview): ImportOutcome = withContext(Dispatchers.IO) {
        val outcome = when (preview) {
            is ImportPreview.Backup -> restore(preview.babies)
            is ImportPreview.Foreign -> {
                val babyId = repo.currentBabyId() ?: return@withContext ImportOutcome(0, 0)
                addEvents(babyId, preview.events)
            }
        }
        reminders.rescheduleAll()
        outcome
    }

    private suspend fun restore(backups: List<BabyBackup>): ImportOutcome {
        var added = 0
        var duplicates = 0
        val existingBabies = repo.babies.first()
        for (backup in backups) {
            val match = existingBabies.firstOrNull {
                it.name.equals(backup.baby.name, ignoreCase = true) && it.birthDate == backup.baby.birthDate
            }
            val babyId = match?.id ?: repo.addBaby(backup.baby)
            val events = addEvents(babyId, backup.events)
            added += events.added
            duplicates += events.duplicates

            val dates = repo.growth(babyId).first().map { it.date }.toSet()
            backup.growth.filter { it.date !in dates }.forEach { repo.saveGrowth(it.copy(id = 0, babyId = babyId)) }

            val keys = repo.appointments(babyId).first().map { it.time to it.title }.toSet()
            backup.appointments.filter { (it.time to it.title) !in keys }
                .forEach { repo.insertAppointment(it.copy(id = 0, babyId = babyId)) }
        }
        return ImportOutcome(added, duplicates)
    }

    private suspend fun addEvents(babyId: Long, events: List<EventEntity>): ImportOutcome {
        val existing = repo.all(babyId).first().map { it.type to it.startTime }.toSet()
        val fresh = events.filter { (it.type to it.startTime) !in existing }
        repo.insertAll(fresh.map { it.copy(id = 0, babyId = babyId) })
        return ImportOutcome(added = fresh.size, duplicates = events.size - fresh.size)
    }

    private fun displayName(uri: Uri): String =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: uri.lastPathSegment.orEmpty()
}
