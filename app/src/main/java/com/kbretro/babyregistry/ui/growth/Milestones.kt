package com.kbretro.babyregistry.ui.growth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kbretro.babyregistry.data.model.BabyEntity
import com.kbretro.babyregistry.data.model.MilestoneEntity
import com.kbretro.babyregistry.data.model.MilestoneKind
import com.kbretro.babyregistry.ui.common.ClockField
import com.kbretro.babyregistry.ui.common.DateField
import com.kbretro.babyregistry.ui.common.Fmt
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun MilestoneEmoji(kind: MilestoneKind, size: Int = 44) {
    Box(
        Modifier
            .size(size.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(kind.style.emoji, fontSize = (size * 0.5).sp)
    }
}

/** Una tarjeta de la línea de tiempo: qué, cuándo y a qué edad. */
@Composable
fun MilestoneCard(baby: BabyEntity, milestone: MilestoneEntity, onClick: () -> Unit) {
    val date = Fmt.localDate(milestone.time)
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            MilestoneEmoji(milestone.kind)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(milestone.title(), style = MaterialTheme.typography.titleMedium)
                Text(
                    "${Fmt.shortDate(date)} · ${Fmt.age(baby.birthDate, date).replaceFirstChar { it.lowercase() }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                milestone.notes?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun MilestonesEmpty() {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🌟", fontSize = 40.sp)
        Spacer(Modifier.height(8.dp))
        Text("Aún no hay hitos", style = MaterialTheme.typography.titleMedium)
        Text(
            "Guarda la caída del cordón, el primer diente, su primera palabra…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MilestoneSheet(
    babyId: Long,
    birthDate: LocalDate,
    initial: MilestoneEntity?,
    onSave: (MilestoneEntity) -> Unit,
    onDelete: (MilestoneEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }
    val initialTime = initial?.time ?: System.currentTimeMillis()
    var kind by remember { mutableStateOf(initial?.kind) }
    var detail by remember { mutableStateOf(initial?.detail.orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    var date by remember { mutableStateOf(Fmt.localDate(initialTime)) }
    var hour by remember { mutableIntStateOf(Fmt.dateTime(initialTime).hour) }
    var minute by remember { mutableIntStateOf(Fmt.dateTime(initialTime).minute) }
    var confirmDelete by remember { mutableStateOf(false) }

    val style = kind?.style
    val valid = kind != null && (style?.detailRequired != true || detail.isNotBlank()) && !date.isBefore(birthDate)

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
            Text(if (initial == null) "Nuevo hito" else "Editar hito", style = MaterialTheme.typography.headlineSmall)

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MilestoneKind.entries.forEach { k ->
                    FilterChip(
                        selected = kind == k,
                        onClick = {
                            if (kind != k) {
                                // Al cambiar de tipo, la respuesta anterior ya no aplica.
                                if (kind != null) detail = ""
                                kind = k
                            }
                        },
                        label = { Text("${k.style.emoji} ${k.style.label}") },
                    )
                }
            }

            if (style?.detailLabel != null) {
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text(style.detailLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (style.suggestions.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        style.suggestions.forEach { s ->
                            SuggestionChip(onClick = { detail = s }, label = { Text(s) })
                        }
                    }
                }
            }

            DateField("Día", date, { date = it })
            ClockField("Hora", Fmt.at(date, hour, minute), { h, m ->
                hour = h
                minute = m
            })
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas (opcional)") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            if (date.isBefore(birthDate)) {
                Text(
                    "La fecha no puede ser anterior al nacimiento.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(
                onClick = {
                    val selected = kind ?: return@Button
                    onSave(
                        MilestoneEntity(
                            id = initial?.id ?: 0,
                            babyId = babyId,
                            time = Fmt.at(date, hour, minute),
                            kind = selected,
                            detail = detail.trim().ifEmpty { null }?.takeIf { selected.style.detailLabel != null },
                            notes = notes.trim().ifEmpty { null },
                        ),
                    )
                    close()
                },
                enabled = valid,
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
                        if (confirmDelete) "Confirmar eliminación" else "Eliminar hito",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
