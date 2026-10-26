package com.kbretro.babyregistry.ui.data

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kbretro.babyregistry.data.transfer.ImportPreview
import com.kbretro.babyregistry.ui.common.ChoiceRow
import com.kbretro.babyregistry.ui.common.EventIcon
import com.kbretro.babyregistry.ui.common.Fmt
import com.kbretro.babyregistry.ui.common.style
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(onBack: () -> Unit, viewModel: DataViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val drive by viewModel.drive.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var pdfDays by remember { mutableIntStateOf(30) }
    var sharingPdf by remember { mutableStateOf(false) }

    val createJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::exportBackup)
    }
    val chooseDriveFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::enableDrive)
    }
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::analyze)
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Importar y exportar") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.busy || sharingPdf) {
                item(key = "progress") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            }
            item(key = "drive-title") { Text("Copia en Google Drive", style = MaterialTheme.typography.titleMedium) }
            item(key = "drive") {
                ActionCard(
                    icon = Icons.Rounded.CloudUpload,
                    title = if (drive.enabled) "Copia diaria activada" else "Copia automática diaria",
                    text = if (drive.enabled) {
                        drive.lastError ?: drive.lastBackupAt?.let { "Última copia: ${Fmt.dayTime(it)} ✓" } ?: "Preparando la primera copia…"
                    } else {
                        "Elige dónde guardarla: en la ventana que se abre, toca ☰ y selecciona Google Drive. " +
                            "La app actualizará ese archivo cada día (con fotos incluidas). " +
                            "Para recuperarla en otro móvil: Importar → elige ese archivo."
                    },
                ) {
                    if (drive.enabled) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::driveBackupNow, enabled = !state.busy) { Text("Copiar ahora") }
                            OutlinedButton(onClick = viewModel::disableDrive, enabled = !state.busy) { Text("Desactivar") }
                        }
                    } else {
                        Button(onClick = { chooseDriveFile.launch("baby-registry-copia.json") }, enabled = !state.busy) {
                            Text("Elegir ubicación en Drive")
                        }
                    }
                }
            }
            item(key = "android-backup") {
                Text(
                    "Además, la copia de seguridad de Android (Ajustes → Google → Copia de seguridad) guarda los datos y las fotos " +
                        "de la app en tu cuenta de Google y los restaura al instalarla en un móvil nuevo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item(key = "export-title") { Text("Exportar", style = MaterialTheme.typography.titleMedium) }
            item(key = "export-json") {
                ActionCard(
                    icon = Icons.Rounded.FileDownload,
                    title = "Copia completa (.json)",
                    text = "Todos los bebés con sus registros, medidas y citas. Úsala para cambiar de móvil o guardar una copia de seguridad.",
                ) {
                    Button(onClick = { createJson.launch("baby-registry-${LocalDate.now()}.json") }, enabled = !state.busy) {
                        Text("Exportar JSON")
                    }
                }
            }
            item(key = "export-pdf") {
                ActionCard(
                    icon = Icons.Rounded.PictureAsPdf,
                    title = "Informe (.pdf)",
                    text = "Resumen del bebé seleccionado para el pediatra: medias, tabla por días, crecimiento y salud.",
                ) {
                    ChoiceRow(listOf(7 to "7 días", 14 to "14 días", 30 to "30 días"), pdfDays, { pdfDays = it })
                    Spacer(Modifier.size(8.dp))
                    Button(
                        onClick = {
                            sharingPdf = true
                            scope.launch {
                                val uri = runCatching { viewModel.exportPdf(pdfDays) }.getOrNull()
                                sharingPdf = false
                                if (uri == null) {
                                    snackbar.showSnackbar("No se pudo crear el informe")
                                } else {
                                    val send = Intent(Intent.ACTION_SEND)
                                        .setType("application/pdf")
                                        .putExtra(Intent.EXTRA_STREAM, uri)
                                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    context.startActivity(Intent.createChooser(send, "Compartir informe"))
                                }
                            }
                        },
                        enabled = !sharingPdf,
                    ) { Text("Crear y compartir PDF") }
                }
            }
            item(key = "import-title") { Text("Importar", style = MaterialTheme.typography.titleMedium) }
            item(key = "import") {
                ActionCard(
                    icon = Icons.Rounded.FileUpload,
                    title = "Desde un archivo",
                    text = "Copias de Baby Registry (.json), la exportación de Baby Tracker (.zip o .csv) u otras apps en JSON, CSV, PDF o texto. " +
                        "Verás un resumen antes de importar y no se borra nada.",
                ) {
                    OutlinedButton(
                        onClick = { openFile.launch(arrayOf("application/json", "application/pdf", "application/zip", "application/x-zip-compressed", "text/*", "application/octet-stream")) },
                        enabled = !state.busy,
                    ) { Text("Elegir archivo") }
                }
            }
            item(key = "tips") {
                Text(
                    "Con datos de otras apps se reconocen tomas, biberones, pañales, sueño, sólidos, temperatura y medicación " +
                        "cuando cada línea tiene fecha (o un día como cabecera), hora y una palabra clave como \"biberón\", \"pañal\", " +
                        "\"siesta\", \"bottle\" o \"diaper\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    state.preview?.let { preview ->
        PreviewDialog(preview, onConfirm = viewModel::confirmImport, onDismiss = viewModel::dismissPreview)
    }
}

@Composable
private fun ActionCard(icon: ImageVector, title: String, text: String, actions: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleSmall)
            }
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            actions()
        }
    }
}

@Composable
private fun PreviewDialog(preview: ImportPreview, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val canImport = when (preview) {
        is ImportPreview.Backup -> preview.babies.isNotEmpty()
        is ImportPreview.Foreign -> preview.events.isNotEmpty()
        is ImportPreview.BabyTracker -> preview.result.total > 0
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Description, contentDescription = null) },
        title = { Text(preview.fileName.ifEmpty { "Archivo" }) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (preview) {
                    is ImportPreview.Backup -> {
                        Text("Copia de seguridad de Baby Registry", style = MaterialTheme.typography.titleSmall)
                        preview.babies.forEach { b ->
                            Text(
                                "• ${b.baby.name}: ${b.events.size} registros, ${b.growth.size} medidas, " +
                                    "${b.appointments.size} citas, ${b.milestones.size} hitos",
                            )
                        }
                        Text(
                            "Si un bebé ya existe (mismo nombre y fecha de nacimiento) se completan sus datos; si no, se crea.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is ImportPreview.BabyTracker -> {
                        val r = preview.result
                        Text("Exportación de Baby Tracker", style = MaterialTheme.typography.titleSmall)
                        val from = r.babyNames.joinToString(", ").ifEmpty { "el bebé del archivo" }
                        Text("Datos de $from → se añadirán a ${preview.babyName}.")
                        r.events.groupBy { it.type }.forEach { (type, events) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                EventIcon(type, size = 28.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("${type.style().label}: ${events.size}")
                            }
                        }
                        if (r.growth.isNotEmpty()) Text("Medidas de crecimiento: ${r.growth.size}")
                        if (r.appointments.isNotEmpty()) Text("Vacunas (como citas hechas): ${r.appointments.size}")
                        if (r.milestones.isNotEmpty()) Text("Hitos: ${r.milestones.size}")
                        if (r.skipped.isNotEmpty()) {
                            Text(
                                "No se importa:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            r.skipped.forEach { (label, count) ->
                                Text("• $label: $count", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    is ImportPreview.Foreign -> {
                        if (preview.events.isEmpty()) {
                            Text("No se ha reconocido ningún registro en este archivo.")
                        } else {
                            Text("Se añadirán a ${preview.babyName}:", style = MaterialTheme.typography.titleSmall)
                            preview.events.groupBy { it.type }.forEach { (type, events) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    EventIcon(type, size = 28.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("${type.style().label}: ${events.size}")
                                }
                            }
                        }
                        if (preview.unrecognized.isNotEmpty()) {
                            Text(
                                "${preview.unrecognized.size} líneas no reconocidas, por ejemplo:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            preview.unrecognized.take(3).forEach {
                                Text("“${it.take(80)}”", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Text("No se borrará nada. Los registros que ya existan no se duplican.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onConfirm, enabled = canImport) { Text("Importar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
