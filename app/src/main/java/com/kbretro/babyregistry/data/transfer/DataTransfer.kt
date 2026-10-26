package com.kbretro.babyregistry.data.transfer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.kbretro.babyregistry.data.model.AppointmentEntity
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.GrowthEntity
import com.kbretro.babyregistry.data.model.MilestoneEntity
import com.kbretro.babyregistry.data.photos.PhotoFiles
import com.kbretro.babyregistry.data.repository.TrackerRepository
import com.kbretro.babyregistry.reminders.Reminders
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/** Resultado de analizar un archivo, que se muestra antes de importar nada. */
sealed interface ImportPreview {
    val fileName: String

    /** Copia de seguridad propia: se restauran bebés, registros, medidas y citas. */
    data class Backup(override val fileName: String, val babies: List<BabyBackup>) : ImportPreview

    /** Exportación de la app Baby Tracker: registros, medidas y vacunas para el bebé seleccionado. */
    data class BabyTracker(
        override val fileName: String,
        val babyName: String,
        val result: BabyTrackerImport.Result,
    ) : ImportPreview

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
                milestones = repo.milestones(baby.id).first(),
            )
        }
        val json = BackupJson.write(babies, System.currentTimeMillis()) { name ->
            PhotoFiles.readBytes(context, name)?.let { Base64.getEncoder().encodeToString(it) }
        }
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray()) }
            ?: error("No se pudo escribir el archivo")
        babies.sumOf { it.events.size }
    }

    suspend fun analyze(uri: Uri): ImportPreview = withContext(Dispatchers.IO) {
        val name = displayName(uri)
        when (val content = TextExtractor.read(context, uri, context.contentResolver.getType(uri), name)) {
            is FileContent.Backup -> ImportPreview.Backup(name, content.babies)
            is FileContent.BabyTracker -> ImportPreview.BabyTracker(name, repo.currentBaby.first()?.name.orEmpty(), content.result)
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
            is ImportPreview.BabyTracker -> {
                val babyId = repo.currentBabyId() ?: return@withContext ImportOutcome(0, 0)
                val r = preview.result
                addAll(babyId, r.events, r.growth, r.appointments, r.milestones)
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
            val babyId = match?.id ?: repo.addBaby(backup.baby.copy(photoPath = restorePhoto(backup.babyPhoto)))
            val milestones = backup.milestones.mapIndexed { i, m -> m.copy(photoPath = restorePhoto(backup.milestonePhotos[i])) }
            val outcome = addAll(babyId, backup.events, backup.growth, backup.appointments, milestones)
            added += outcome.added
            duplicates += outcome.duplicates
        }
        return ImportOutcome(added, duplicates)
    }

    /** Añade registros, medidas y citas omitiendo los que ya existen. */
    private suspend fun addAll(
        babyId: Long,
        events: List<EventEntity>,
        growth: List<GrowthEntity>,
        appointments: List<AppointmentEntity>,
        milestones: List<MilestoneEntity>,
    ): ImportOutcome {
        val eventOutcome = addEvents(babyId, events)

        val dates = repo.growth(babyId).first().map { it.date }.toSet()
        val newGrowth = growth.filter { it.date !in dates }
        newGrowth.forEach { repo.saveGrowth(it.copy(id = 0, babyId = babyId)) }

        val keys = repo.appointments(babyId).first().map { it.time to it.title }.toSet()
        val newAppointments = appointments.filter { (it.time to it.title) !in keys }
        newAppointments.forEach { repo.insertAppointment(it.copy(id = 0, babyId = babyId)) }

        val milestoneKeys = repo.milestones(babyId).first().map { Triple(it.time, it.kind, it.detail) }.toSet()
        val newMilestones = milestones.filter { Triple(it.time, it.kind, it.detail) !in milestoneKeys }
        newMilestones.forEach { repo.saveMilestone(it.copy(id = 0, babyId = babyId)) }
        // Las fotos restauradas de hitos que ya existían no se usan: se borran.
        (milestones - newMilestones.toSet()).forEach { PhotoFiles.delete(context, it.photoPath) }

        val added = eventOutcome.added + newGrowth.size + newAppointments.size + newMilestones.size
        val total = events.size + growth.size + appointments.size + milestones.size
        return ImportOutcome(added = added, duplicates = total - added)
    }

    private suspend fun restorePhoto(base64: String?): String? =
        base64?.let { runCatching { PhotoFiles.saveBytes(context, Base64.getDecoder().decode(it)) }.getOrNull() }

    private suspend fun addEvents(babyId: Long, events: List<EventEntity>): ImportOutcome {
        val existing = repo.all(babyId).first().map { it.importKey() }.toSet()
        val fresh = events.filter { it.importKey() !in existing }
        repo.insertAll(fresh.map { it.copy(id = 0, babyId = babyId) })
        return ImportOutcome(added = fresh.size, duplicates = events.size - fresh.size)
    }

    private fun displayName(uri: Uri): String =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: uri.lastPathSegment.orEmpty()
}
