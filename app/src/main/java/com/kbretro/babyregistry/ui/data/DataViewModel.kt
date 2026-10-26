package com.kbretro.babyregistry.ui.data

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kbretro.babyregistry.data.backup.DriveBackup
import com.kbretro.babyregistry.data.backup.DriveBackupState
import com.kbretro.babyregistry.data.transfer.DataTransfer
import com.kbretro.babyregistry.data.transfer.ImportPreview
import com.kbretro.babyregistry.export.ReportExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DataUiState(
    val busy: Boolean = false,
    val preview: ImportPreview? = null,
    val message: String? = null,
)

@HiltViewModel
class DataViewModel @Inject constructor(
    private val transfer: DataTransfer,
    private val exporter: ReportExporter,
    private val driveBackup: DriveBackup,
) : ViewModel() {

    val drive: StateFlow<DriveBackupState> =
        driveBackup.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DriveBackupState())

    fun enableDrive(uri: Uri) = run {
        driveBackup.enable(uri)
        "Copia automática activada"
    }

    fun driveBackupNow() = run {
        if (driveBackup.backupNow()) "Copia guardada en Drive" else "No se pudo guardar la copia"
    }

    fun disableDrive() = run {
        driveBackup.disable()
        "Copia automática desactivada"
    }

    private val _state = MutableStateFlow(DataUiState())
    val state: StateFlow<DataUiState> = _state.asStateFlow()

    private fun run(block: suspend () -> String?) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val message = runCatching { block() }.getOrElse { "No se pudo completar: ${it.message ?: it.javaClass.simpleName}" }
            _state.update { it.copy(busy = false, message = message ?: it.message) }
        }
    }

    fun exportBackup(uri: Uri) = run {
        val count = transfer.exportBackup(uri)
        "Copia guardada ($count registros)"
    }

    fun analyze(uri: Uri) = run {
        val preview = transfer.analyze(uri)
        _state.update { it.copy(preview = preview) }
        null
    }

    fun confirmImport() {
        val preview = _state.value.preview ?: return
        _state.update { it.copy(preview = null) }
        run {
            val outcome = transfer.apply(preview)
            buildString {
                append("Importados ${outcome.added} registros")
                if (outcome.duplicates > 0) append(" (${outcome.duplicates} ya existían)")
            }
        }
    }

    fun dismissPreview() = _state.update { it.copy(preview = null) }

    fun messageShown() = _state.update { it.copy(message = null) }

    suspend fun exportPdf(days: Int): Uri? = exporter.export(days)
}
