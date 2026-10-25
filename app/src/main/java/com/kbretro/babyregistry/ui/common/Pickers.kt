package com.kbretro.babyregistry.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
private object PastOrToday : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= System.currentTimeMillis()
    override fun isSelectableYear(year: Int) = year <= LocalDate.now().year
}

@OptIn(ExperimentalMaterial3Api::class)
private object AnyDate : SelectableDates

@Composable
private fun PickerCard(icon: ImageVector, label: String, value: String, onClick: () -> Unit, modifier: Modifier) {
    OutlinedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(label, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Selector de fecha. Con [pastOnly] no se pueden elegir días futuros. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    date: LocalDate?,
    onChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    pastOnly: Boolean = true,
    placeholder: String = "Elegir",
) {
    var show by remember { mutableStateOf(false) }
    PickerCard(Icons.Rounded.CalendarMonth, label, date?.let(Fmt::shortDate) ?: placeholder, { show = true }, modifier)
    if (show) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (date ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = if (pastOnly) PastOrToday else AnyDate,
        )
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    show = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = state)
        }
    }
}

/** Selector de la hora del día (sin ajustar la fecha). */
@Composable
fun ClockField(label: String, millis: Long, onChange: (hour: Int, minute: Int) -> Unit, modifier: Modifier = Modifier) {
    var show by remember { mutableStateOf(false) }
    PickerCard(Icons.Rounded.Schedule, label, Fmt.time(millis), { show = true }, modifier)
    if (show) {
        TimePickerDialog(
            initialMillis = millis,
            onDismiss = { show = false },
            onConfirm = { h, m ->
                onChange(h, m)
                show = false
            },
        )
    }
}
