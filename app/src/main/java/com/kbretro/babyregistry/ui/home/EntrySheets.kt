package com.kbretro.babyregistry.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.kbretro.babyregistry.data.model.BottleContent
import com.kbretro.babyregistry.data.model.BreastSide
import com.kbretro.babyregistry.data.model.DiaperKind
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.data.model.StoolColor
import com.kbretro.babyregistry.ui.common.ChoiceRow
import com.kbretro.babyregistry.ui.common.EventIcon
import com.kbretro.babyregistry.ui.common.NumberStepper
import com.kbretro.babyregistry.ui.common.TimeSelector
import com.kbretro.babyregistry.ui.common.label
import com.kbretro.babyregistry.ui.common.style
import com.kbretro.babyregistry.ui.common.swatch
import kotlinx.coroutines.launch

private const val MINUTE = 60_000L

/** Hoja inferior para registrar un evento del tipo indicado. [onDone] recibe el mensaje de confirmación. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrySheet(
    type: EventType,
    state: HomeUiState,
    viewModel: HomeViewModel,
    onDone: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun finish(message: String) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            onDone(message)
        }
    }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                EventIcon(type, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Text(type.style().label, style = MaterialTheme.typography.headlineSmall)
            }
            when (type) {
                EventType.BREAST -> BreastEntry(
                    suggested = state.suggestedSide,
                    onStart = { side ->
                        viewModel.startBreast(side)
                        finish("Temporizador iniciado")
                    },
                    onManual = { start, left, right, last ->
                        viewModel.addManualBreast(start, left, right, last)
                        finish("Toma de pecho guardada")
                    },
                )
                EventType.BOTTLE -> BottleEntry(
                    defaultMl = state.lastBottle?.amountMl ?: 90,
                    defaultContent = state.lastBottle?.bottleContent ?: BottleContent.FORMULA,
                ) { ml, content, time, notes ->
                    viewModel.addBottle(ml, content, time, notes)
                    finish("Biberón de $ml ml guardado")
                }
                EventType.SOLID -> SolidEntry { food, isNew, reaction, time ->
                    viewModel.addSolid(food, isNew, reaction, time)
                    finish("Comida guardada")
                }
                EventType.SLEEP -> SleepEntry(
                    onStart = {
                        viewModel.startSleep()
                        finish("¡Dulces sueños!")
                    },
                    onManual = { start, end ->
                        viewModel.addManualSleep(start, end)
                        finish("Sueño guardado")
                    },
                )
                EventType.DIAPER -> DiaperEntry { kind, color, time, notes ->
                    viewModel.addDiaper(kind, color, time, notes)
                    finish("Pañal guardado")
                }
            }
        }
    }
}

@Composable
private fun SaveButton(enabled: Boolean = true, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Icon(Icons.Rounded.Check, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Guardar")
    }
}

@Composable
private fun OrDivider(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f))
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        HorizontalDivider(Modifier.weight(1f))
    }
}

@Composable
private fun BreastEntry(
    suggested: BreastSide?,
    onStart: (BreastSide) -> Unit,
    onManual: (start: Long, left: Int, right: Int, last: BreastSide) -> Unit,
) {
    val color = EventType.BREAST.style().color
    if (suggested != null) {
        Text(
            "Sugerencia: empieza por el ${suggested.label.lowercase()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BreastSide.entries.forEach { side ->
            val modifier = Modifier
                .weight(1f)
                .height(64.dp)
            val content: @Composable () -> Unit = {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(side.label)
            }
            if (side == suggested || suggested == null) {
                Button(
                    onClick = { onStart(side) },
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                    modifier = modifier,
                ) { content() }
            } else {
                OutlinedButton(onClick = { onStart(side) }, modifier = modifier) { content() }
            }
        }
    }

    OrDivider("o registra una toma pasada")

    val now = remember { System.currentTimeMillis() }
    var start by remember { mutableLongStateOf(now - 30 * MINUTE) }
    var left by remember { mutableIntStateOf(10) }
    var right by remember { mutableIntStateOf(0) }
    var lastChoice by remember { mutableStateOf(BreastSide.RIGHT) }
    TimeSelector("Inicio", start, { start = it })
    NumberStepper(left, { left = it }, step = 1, range = 0..90, suffix = "min", label = "Izquierdo")
    NumberStepper(right, { right = it }, step = 1, range = 0..90, suffix = "min", label = "Derecho")
    val last = when {
        left > 0 && right == 0 -> BreastSide.LEFT
        right > 0 && left == 0 -> BreastSide.RIGHT
        else -> lastChoice
    }
    if (left > 0 && right > 0) {
        Text("¿Con qué pecho terminó?", style = MaterialTheme.typography.labelLarge)
        ChoiceRow(BreastSide.entries.map { it to it.label }, lastChoice, { lastChoice = it })
    }
    SaveButton(enabled = left + right > 0, color = color) { onManual(start, left, right, last) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BottleEntry(
    defaultMl: Int,
    defaultContent: BottleContent,
    onSave: (ml: Int, content: BottleContent, time: Long, notes: String) -> Unit,
) {
    var ml by remember { mutableIntStateOf(defaultMl) }
    var content by remember { mutableStateOf(defaultContent) }
    var time by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var notes by remember { mutableStateOf("") }

    NumberStepper(ml, { ml = it }, step = 10, range = 0..400, suffix = "ml")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(60, 90, 120, 150, 180, 210).forEach { preset ->
            FilterChip(selected = ml == preset, onClick = { ml = preset }, label = { Text("$preset ml") })
        }
    }
    ChoiceRow(BottleContent.entries.map { it to it.label }, content, { content = it })
    TimeSelector("Hora", time, { time = it })
    OutlinedTextField(
        value = notes,
        onValueChange = { notes = it },
        label = { Text("Notas (opcional)") },
        modifier = Modifier.fillMaxWidth(),
    )
    SaveButton(enabled = ml > 0, color = EventType.BOTTLE.style().color) { onSave(ml, content, time, notes) }
}

@Composable
private fun SolidEntry(onSave: (food: String, isNew: Boolean, reaction: String, time: Long) -> Unit) {
    var food by remember { mutableStateOf("") }
    var isNew by remember { mutableStateOf(false) }
    var reaction by remember { mutableStateOf("") }
    var time by remember { mutableLongStateOf(System.currentTimeMillis()) }

    OutlinedTextField(
        value = food,
        onValueChange = { food = it },
        label = { Text("¿Qué ha comido?") },
        placeholder = { Text("Ej.: puré de calabaza") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth(),
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isNew = !isNew },
    ) {
        Checkbox(checked = isNew, onCheckedChange = { isNew = it })
        Column {
            Text("Primera vez que lo prueba")
            Text(
                "Útil para detectar posibles alergias",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    OutlinedTextField(
        value = reaction,
        onValueChange = { reaction = it },
        label = { Text("Reacción / notas (opcional)") },
        placeholder = { Text("Ej.: le encantó, sarpullido…") },
        modifier = Modifier.fillMaxWidth(),
    )
    TimeSelector("Hora", time, { time = it })
    SaveButton(enabled = food.isNotBlank(), color = EventType.SOLID.style().color) {
        onSave(food, isNew, reaction, time)
    }
}

@Composable
private fun SleepEntry(onStart: () -> Unit, onManual: (start: Long, end: Long) -> Unit) {
    val color = EventType.SLEEP.style().color
    Button(
        onClick = onStart,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
    ) {
        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Se ha dormido ahora")
    }

    OrDivider("o registra un sueño pasado")

    val now = remember { System.currentTimeMillis() }
    var start by remember { mutableLongStateOf(now - 60 * MINUTE) }
    var end by remember { mutableLongStateOf(now) }
    TimeSelector("Se durmió", start, { start = it })
    TimeSelector("Se despertó", end, { end = it })
    val valid = end > start
    if (!valid) {
        Text(
            "La hora de despertar debe ser posterior a la de dormir.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    SaveButton(enabled = valid, color = color) { onManual(start, end) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiaperEntry(onSave: (kind: DiaperKind, color: StoolColor?, time: Long, notes: String) -> Unit) {
    var kind by remember { mutableStateOf(DiaperKind.WET) }
    var stool by remember { mutableStateOf<StoolColor?>(null) }
    var time by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var notes by remember { mutableStateOf("") }

    ChoiceRow(DiaperKind.entries.map { it to it.label }, kind, { kind = it })
    if (kind != DiaperKind.WET) {
        Text("Color (opcional)", style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StoolColor.entries.forEach { color ->
                ColorSwatch(color, selected = stool == color) { stool = if (stool == color) null else color }
            }
        }
        if (stool?.isWarning == true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Las heces de color ${stool?.label?.lowercase()} pueden requerir atención. " +
                        "Consulta con tu pediatra.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
    TimeSelector("Hora", time, { time = it })
    OutlinedTextField(
        value = notes,
        onValueChange = { notes = it },
        label = { Text("Notas (opcional)") },
        modifier = Modifier.fillMaxWidth(),
    )
    SaveButton(color = EventType.DIAPER.style().color) { onSave(kind, stool, time, notes) }
}

@Composable
private fun ColorSwatch(color: StoolColor, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .size(36.dp)
                .background(color.swatch, CircleShape)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = CircleShape,
                ),
        )
        Text(color.label, style = MaterialTheme.typography.labelSmall)
    }
}
