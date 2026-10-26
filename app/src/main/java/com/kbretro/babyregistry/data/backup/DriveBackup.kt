package com.kbretro.babyregistry.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kbretro.babyregistry.data.repository.settings
import com.kbretro.babyregistry.data.transfer.DataTransfer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class DriveBackupState(
    val target: Uri? = null,
    val lastBackupAt: Long? = null,
    val lastError: String? = null,
) {
    val enabled: Boolean get() = target != null
}

/**
 * Copia automática diaria a un archivo elegido por el usuario (normalmente en Google Drive, a través
 * del selector de archivos del sistema). No necesita claves de Google: Android da permiso de escritura
 * permanente sobre ese archivo y la copia se reescribe cada día con WorkManager.
 */
@Singleton
class DriveBackup @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transfer: DataTransfer,
) {
    private val targetKey = stringPreferencesKey("drive_backup_uri")
    private val lastKey = longPreferencesKey("drive_backup_last")
    private val errorKey = stringPreferencesKey("drive_backup_error")

    val state: Flow<DriveBackupState> = context.settings.data.map { p ->
        DriveBackupState(
            target = p[targetKey]?.let(Uri::parse),
            lastBackupAt = p[lastKey],
            lastError = p[errorKey],
        )
    }

    /** Activa la copia diaria en el archivo [uri] y hace la primera copia ya. */
    suspend fun enable(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        context.settings.edit {
            it[targetKey] = uri.toString()
            it.remove(errorKey)
        }
        val daily = PeriodicWorkRequestBuilder<DriveBackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, daily)
        backupNow()
    }

    suspend fun disable() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        val target = state.first().target
        if (target != null) {
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    target,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
        context.settings.edit {
            it.remove(targetKey)
            it.remove(lastKey)
            it.remove(errorKey)
        }
    }

    /** Lanza una copia en segundo plano (sobrevive aunque se cierre la app). */
    fun requestBackup() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "$WORK_NAME-now",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<DriveBackupWorker>().build(),
        )
    }

    /** Escribe la copia completa ahora. Devuelve false si no se pudo (y guarda el motivo). */
    suspend fun backupNow(): Boolean {
        val target = state.first().target ?: return false
        return runCatching { transfer.exportBackup(target) }.fold(
            onSuccess = {
                context.settings.edit {
                    it[lastKey] = System.currentTimeMillis()
                    it.remove(errorKey)
                }
                true
            },
            onFailure = { e ->
                context.settings.edit {
                    it[errorKey] = "No se pudo guardar la copia (${e.javaClass.simpleName}). Vuelve a elegir el archivo."
                }
                false
            },
        )
    }

    companion object {
        const val WORK_NAME = "drive-backup"
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DriveBackupEntryPoint {
    fun driveBackup(): DriveBackup
}

class DriveBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val backup = EntryPointAccessors.fromApplication(applicationContext, DriveBackupEntryPoint::class.java).driveBackup()
        return when {
            backup.backupNow() -> Result.success()
            runAttemptCount < 3 -> Result.retry()
            else -> Result.failure()
        }
    }
}
