package com.kbretro.babyregistry.ui.health

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kbretro.babyregistry.data.model.AppointmentEntity
import com.kbretro.babyregistry.data.model.AppointmentKind
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.ui.common.EventDetailDialog
import com.kbretro.babyregistry.ui.common.EventRow
import com.kbretro.babyregistry.ui.common.FeverLevel
import com.kbretro.babyregistry.ui.common.Fmt
import com.kbretro.babyregistry.ui.common.SectionTitle
import com.kbretro.babyregistry.ui.common.feverLevel
import com.kbretro.babyregistry.ui.common.label
import com.kbretro.babyregistry.ui.common.rememberNow
import com.kbretro.babyregistry.ui.common.style
import com.kbretro.babyregistry.ui.entry.EntryRequest
import com.kbretro.babyregistry.ui.entry.EntrySheet
import kotlinx.coroutines.launch

private val AppointmentColor = Color(0xFF6E56A8)

private val AppointmentKind.icon: ImageVector
    get() = when (this) {
        AppointmentKind.DOCTOR -> Icons.Rounded.LocalHospital
        AppointmentKind.VACCINE -> Icons.Rounded.Vaccines
        AppointmentKind.OTHER -> Icons.Rounded.Event
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(viewModel: HealthViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val babyId = state.babyId ?: return
    val now = rememberNow(60_000L)
    var entry by remember { mutableStateOf<EntryRequest?>(null) }
    var detail by remember { mutableStateOf<EventEntity?>(null) }
    var appointment by remember { mutableStateOf<AppointmentEntity?>(null) }
    var addingAppointment by remember { mutableStateOf(false) }
    var showPast by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun notify(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    // Una cita sigue "próxima" hasta 2 h después de su hora, salvo que se marque como hecha.
    val upcoming = state.appointments.filter { !it.done && it.time > now - 2 * 3_600_000L }
    val past = state.appointments.filter { it !in upcoming }.sortedByDescending { it.time }
    val nextDose = state.medicines.filter { (it.reminderAt ?: 0) > now }.minByOrNull { it.reminderAt!! }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(title = { Text("Salud") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "actions") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HealthAction(EventType.TEMPERATURE.style().icon, "Temperatura", EventType.TEMPERATURE.style().color, Modifier.weight(1f)) {
                        entry = EntryRequest(EventType.TEMPERATURE)
                    }
                    HealthAction(EventType.MEDICINE.style().icon, "Medicamento", EventType.MEDICINE.style().color, Modifier.weight(1f)) {
                        entry = EntryRequest(EventType.MEDICINE)
                    }
                    HealthAction(Icons.Rounded.Event, "Cita", AppointmentColor, Modifier.weight(1f)) {
                        addingAppointment = true
                    }
                }
            }

            if (nextDose != null) {
                item(key = "next-dose") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = EventType.MEDICINE.style().color.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = EventType.MEDICINE.style().color)
                            Column(Modifier.padding(start = 12.dp)) {
                                Text("Próxima dosis", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    listOfNotNull(nextDose.medicineName, nextDose.dose).joinToString(" · "),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    "${Fmt.dayTimeFuture(nextDose.reminderAt ?: now)} (en ${Fmt.duration((nextDose.reminderAt ?: now) - now)})",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }

            item(key = "appointments-title") { SectionTitle("Próximas citas") }
            if (upcoming.isEmpty()) {
                item(key = "appointments-empty") { EmptyText("No hay citas pendientes. Añade revisiones o vacunas para recibir un aviso.") }
            }
            items(upcoming, key = { "a${it.id}" }) { a ->
                AppointmentRow(a, onToggle = { viewModel.setDone(a, !a.done) }, onClick = { appointment = a })
            }
            if (past.isNotEmpty()) {
                item(key = "past-toggle") {
                    TextButton(onClick = { showPast = !showPast }) {
                        Text(if (showPast) "Ocultar citas pasadas" else "Ver citas pasadas (${past.size})")
                    }
                }
                if (showPast) {
                    items(past, key = { "p${it.id}" }) { a ->
                        AppointmentRow(a, onToggle = { viewModel.setDone(a, !a.done) }, onClick = { appointment = a })
                    }
                }
            }

            item(key = "temp-title") { SectionTitle("Temperatura") }
            if (state.temperatures.isEmpty()) {
                item(key = "temp-empty") { EmptyText("Sin registros de temperatura.") }
            } else {
                item(key = "temp-list") { EventCard(state.temperatures.take(10), now) { detail = it } }
            }

            item(key = "med-title") { SectionTitle("Medicación") }
            if (state.medicines.isEmpty()) {
                item(key = "med-empty") { EmptyText("Sin registros de medicación.") }
            } else {
                item(key = "med-list") { EventCard(state.medicines.take(10), now) { detail = it } }
            }
        }
    }

    entry?.let { request ->
        EntrySheet(request = request, onDone = ::notify, onDismiss = { entry = null })
    }
    detail?.let { event ->
        EventDetailDialog(
            event = event,
            now = now,
            onDismiss = { detail = null },
            onDelete = {
                viewModel.deleteEvent(event)
                detail = null
                notify("Registro eliminado")
            },
            onEdit = {
                detail = null
                entry = EntryRequest(event.type, event)
            },
        )
    }
    if (addingAppointment || appointment != null) {
        AppointmentSheet(
            babyId = babyId,
            initial = appointment,
            onSave = {
                viewModel.saveAppointment(it)
                notify("Cita guardada")
            },
            onDelete = {
                viewModel.deleteAppointment(it)
                notify("Cita eliminada")
            },
            onDismiss = {
                addingAppointment = false
                appointment = null
            },
        )
    }
}

@Composable
private fun HealthAction(icon: ImageVector, label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.14f),
        modifier = modifier,
    ) {
        Column(Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun EmptyText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun EventCard(events: List<EventEntity>, now: Long, onClick: (EventEntity) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 4.dp)) {
            events.forEach { event ->
                val fever = event.temperatureC?.let(::feverLevel)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { EventRow(event, now, onClick = { onClick(event) }) }
                    if (fever == FeverLevel.FEVER || fever == FeverLevel.LOW_GRADE) {
                        Text(
                            fever?.label.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentRow(appointment: AppointmentEntity, onToggle: () -> Unit, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        ListItem(
            modifier = Modifier.clickable(onClick = onClick),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = { Icon(appointment.kind.icon, contentDescription = null, tint = AppointmentColor) },
            overlineContent = { Text(appointment.kind.label) },
            headlineContent = {
                Text(
                    appointment.title,
                    textDecoration = if (appointment.done) TextDecoration.LineThrough else null,
                )
            },
            supportingContent = {
                Text(
                    buildString {
                        append(Fmt.dayTimeFuture(appointment.time))
                        if (appointment.reminderAt != null && !appointment.done) append(" · con aviso")
                        appointment.notes?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingContent = { Checkbox(checked = appointment.done, onCheckedChange = { onToggle() }) },
        )
    }
}
