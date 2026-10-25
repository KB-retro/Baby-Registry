package com.kbretro.babyregistry.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import kotlinx.coroutines.delay

/** Hora actual que se refresca cada [intervalMillis], alineada al segundo. */
@Composable
fun rememberNow(intervalMillis: Long = 1_000L): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(intervalMillis) {
        while (true) {
            now = System.currentTimeMillis()
            delay(intervalMillis - now % intervalMillis)
        }
    }
    return now
}

@Composable
fun EventIcon(type: EventType, size: Dp = 40.dp) {
    val style = type.style()
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(style.color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(style.icon, contentDescription = null, tint = style.color, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
fun EventRow(event: EventEntity, now: Long, onClick: (() -> Unit)? = null) {
    ListItem(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { EventIcon(event.type) },
        headlineContent = { Text(event.title()) },
        supportingContent = {
            Text(event.detail(now), maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        trailingContent = {
            Text(Fmt.time(event.startTime), style = MaterialTheme.typography.labelLarge)
        },
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(vertical = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ChoiceRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) {
                Text(label, maxLines = 1)
            }
        }
    }
}

@Composable
fun NumberStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    step: Int,
    range: IntRange,
    suffix: String,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (label == null) Arrangement.Center else Arrangement.Start,
    ) {
        if (label != null) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        }
        FilledTonalIconButton(
            onClick = { onValueChange((value - step).coerceIn(range)) },
            enabled = value > range.first,
        ) { Icon(Icons.Rounded.Remove, contentDescription = "Menos") }
        Text(
            text = "$value $suffix",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 104.dp),
        )
        FilledTonalIconButton(
            onClick = { onValueChange((value + step).coerceIn(range)) },
            enabled = value < range.last,
        ) { Icon(Icons.Rounded.Add, contentDescription = "Más") }
    }
}

/** Campo que muestra una hora y abre un selector. Mantiene el día; si la hora queda en el futuro, pasa al día anterior. */
@Composable
fun TimeSelector(label: String, millis: Long, onChange: (Long) -> Unit, modifier: Modifier = Modifier) {
    var show by remember { mutableStateOf(false) }
    OutlinedCard(onClick = { show = true }, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Schedule, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(label, modifier = Modifier.weight(1f))
            Text(Fmt.dayTime(millis), style = MaterialTheme.typography.titleMedium)
        }
    }
    if (show) {
        TimePickerDialog(
            initialMillis = millis,
            onDismiss = { show = false },
            onConfirm = { h, m ->
                onChange(Fmt.withTime(millis, h, m))
                show = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(initialMillis: Long, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    val initial = Fmt.dateTime(initialMillis)
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("Aceptar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        text = { TimePicker(state = state) },
    )
}

@Composable
fun EventDetailDialog(
    event: EventEntity,
    now: Long,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (() -> Unit)? = null,
) {
    var confirming by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { EventIcon(event.type) },
        title = { Text(event.title()) },
        text = {
            Column {
                Text(event.detail(now), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.size(8.dp))
                Text("Inicio: ${Fmt.dayTime(event.startTime)}")
                val end = event.endTime
                if (end != null && end != event.startTime) Text("Fin: ${Fmt.dayTime(end)}")
                event.notes?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.size(8.dp))
                    Text("Notas: $it")
                }
            }
        },
        confirmButton = {
            if (onEdit != null && event.isEditable && !confirming) {
                TextButton(onClick = onEdit) { Text("Editar") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cerrar") }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (confirming) onDelete() else confirming = true }) {
                Text(
                    if (confirming) "Confirmar eliminación" else "Eliminar",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}
