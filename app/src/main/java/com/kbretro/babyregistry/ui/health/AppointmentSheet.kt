package com.kbretro.babyregistry.ui.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.kbretro.babyregistry.data.model.AppointmentEntity
import com.kbretro.babyregistry.data.model.AppointmentKind
import com.kbretro.babyregistry.ui.common.ChoiceRow
import com.kbretro.babyregistry.ui.common.ClockField
import com.kbretro.babyregistry.ui.common.DateField
import com.kbretro.babyregistry.ui.common.Fmt
import com.kbretro.babyregistry.ui.common.label
import com.kbretro.babyregistry.ui.common.rememberNotificationPermissionRequest
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val HOUR = 3_600_000L

private enum class ReminderOption(val label: String, val before: Long?) {
    NONE("No", null),
    ONE_HOUR("1 h antes", HOUR),
    ONE_DAY("1 día antes", 24 * HOUR),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppointmentSheet(
    babyId: Long,
    initial: AppointmentEntity?,
    onSave: (AppointmentEntity) -> Unit,
    onDelete: (AppointmentEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }
    val requestPermission = rememberNotificationPermissionRequest()

    val initialTime = initial?.time
    var kind by remember { mutableStateOf(initial?.kind ?: AppointmentKind.DOCTOR) }
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var date by remember { mutableStateOf(initialTime?.let(Fmt::localDate) ?: LocalDate.now().plusDays(1)) }
    var hour by remember { mutableIntStateOf(initialTime?.let { Fmt.dateTime(it).hour } ?: 10) }
    var minute by remember { mutableIntStateOf(initialTime?.let { Fmt.dateTime(it).minute } ?: 0) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    var reminder by remember {
        mutableStateOf(
            when (initial?.let { a -> a.reminderAt?.let { a.time - it } }) {
                null -> if (initial == null) ReminderOption.ONE_DAY else ReminderOption.NONE
                HOUR -> ReminderOption.ONE_HOUR
                else -> ReminderOption.ONE_DAY
            },
        )
    }
    var confirmDelete by remember { mutableStateOf(false) }
    val time = Fmt.at(date, hour, minute)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(if (initial == null) "Nueva cita" else "Editar cita", style = MaterialTheme.typography.headlineSmall)
            ChoiceRow(AppointmentKind.entries.map { it to it.label }, kind, { kind = it })
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") },
                placeholder = {
                    Text(if (kind == AppointmentKind.VACCINE) "Ej.: vacunas de los 2 meses" else "Ej.: revisión del mes")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            DateField("Día", date, { date = it }, pastOnly = false)
            ClockField("Hora", time, { h, m ->
                hour = h
                minute = m
            })
            Text("Recordatorio", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReminderOption.entries.forEach { option ->
                    FilterChip(
                        selected = reminder == option,
                        onClick = {
                            reminder = option
                            if (option.before != null) requestPermission()
                        },
                        label = { Text(option.label) },
                    )
                }
            }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas (opcional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    onSave(
                        AppointmentEntity(
                            id = initial?.id ?: 0,
                            babyId = babyId,
                            time = time,
                            kind = kind,
                            title = title.trim(),
                            notes = notes.ifBlank { null },
                            reminderAt = reminder.before?.let { time - it },
                            done = initial?.done ?: false,
                        ),
                    )
                    close()
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Guardar")
            }
            if (initial != null) {
                OutlinedButton(
                    onClick = {
                        if (confirmDelete) {
                            onDelete(initial)
                            close()
                        } else {
                            confirmDelete = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (confirmDelete) "Confirmar eliminación" else "Eliminar cita",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
