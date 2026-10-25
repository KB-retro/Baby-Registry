package com.kbretro.babyregistry.ui.entry

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kbretro.babyregistry.data.model.BottleContent
import com.kbretro.babyregistry.data.model.BreastSide
import com.kbretro.babyregistry.data.model.DiaperKind
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.data.model.StoolColor
import com.kbretro.babyregistry.ui.common.ChoiceRow
import com.kbretro.babyregistry.ui.common.EventIcon
import com.kbretro.babyregistry.ui.common.FeverLevel
import com.kbretro.babyregistry.ui.common.Fmt
import com.kbretro.babyregistry.ui.common.NumberStepper
import com.kbretro.babyregistry.ui.common.TimeSelector
import com.kbretro.babyregistry.ui.common.feverLevel
import com.kbretro.babyregistry.ui.common.label
import com.kbretro.babyregistry.ui.common.rememberNotificationPermissionRequest
import com.kbretro.babyregistry.ui.common.style
import com.kbretro.babyregistry.ui.common.swatch
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE

/** Qué hoja abrir: un tipo nuevo o un registro existente para editarlo. */
data class EntryRequest(val type: EventType, val initial: EventEntity? = null)

/** Datos de contexto para precargar los formularios. */
data class EntryDefaults(
    val suggestedSide: BreastSide? = null,
    val lastBottle: EventEntity? = null,
)

/**
 * Hoja inferior para registrar o editar un evento.
 * [onStartBreast] y [onStartSleep] arrancan temporizadores (solo al crear).
 * [onDone] recibe el mensaje de confirmación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrySheet(
    request: EntryRequest,
    onDone: (String) -> Unit,
    onDismiss: () -> Unit,
    defaults: EntryDefaults = EntryDefaults(),
    onStartBreast: ((BreastSide) -> Unit)? = null,
    onStartSleep: (() -> Unit)? = null,
    viewModel: EntryViewModel = hiltViewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val type = request.type
    val initial = request.initial
    fun finish(message: String) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            onDone(message)
        }
    }
    fun save(message: String, build: (Long) -> EventEntity) {
        viewModel.save(initial, build)
        finish(if (initial != null) "Cambios guardados" else message)
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
                Text(
                    if (initial != null) "Editar ${type.style().label.lowercase()}" else type.style().label,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            when (type) {
                EventType.BREAST -> BreastEntry(
                    initial = initial,
                    suggested = defaults.suggestedSide,
                    onStart = onStartBreast?.let { start ->
                        { side: BreastSide ->
                            start(side)
                            finish("Temporizador iniciado")
                        }
                    },
                    onSave = { save("Toma de pecho guardada", it) },
                )
                EventType.BOTTLE -> BottleEntry(initial, defaults.lastBottle) { ml, build ->
                    save("Biberón de $ml ml guardado", build)
                }
                EventType.SOLID -> SolidEntry(initial) { save("Comida guardada", it) }
                EventType.SLEEP -> SleepEntry(
                    initial = initial,
                    onStart = onStartSleep?.let { start ->
                        {
                            start()
                            finish("¡Dulces sueños!")
                        }
                    },
                    onSave = { save("Sueño guardado", it) },
                )
                EventType.DIAPER -> DiaperEntry(initial) { save("Pañal guardado", it) }
                EventType.TEMPERATURE -> TemperatureEntry(initial) { save("Temperatura guardada", it) }
                EventType.MEDICINE -> MedicineEntry(initial) { save("Medicamento guardado", it) }
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
private fun NotesField(value: String, onChange: (String) -> Unit, label: String = "Notas (opcional)") {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun BreastEntry(
    initial: EventEntity?,
    suggested: BreastSide?,
    onStart: ((BreastSide) -> Unit)?,
    onSave: ((Long) -> EventEntity) -> Unit,
) {
    val color = EventType.BREAST.style().color
    if (initial == null && onStart != null) {
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
    }

    var start by remember { mutableLongStateOf(initial?.startTime ?: (System.currentTimeMillis() - 30 * MINUTE)) }
    var left by remember { mutableIntStateOf(initial?.let { (it.leftMillis / MINUTE.toDouble()).roundToInt() } ?: 10) }
    var right by remember { mutableIntStateOf(initial?.let { (it.rightMillis / MINUTE.toDouble()).roundToInt() } ?: 0) }
    var lastChoice by remember { mutableStateOf(initial?.lastSide ?: BreastSide.RIGHT) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
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
    NotesField(notes, { notes = it })
    SaveButton(enabled = left + right > 0, color = color) {
        onSave { babyId ->
            val leftMs = left * MINUTE
            val rightMs = right * MINUTE
            EventEntity(
                babyId = babyId,
                type = EventType.BREAST,
                startTime = start,
                endTime = start + leftMs + rightMs,
                leftMillis = leftMs,
                rightMillis = rightMs,
                lastSide = last,
                notes = notes.ifBlank { null },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BottleEntry(
    initial: EventEntity?,
    lastBottle: EventEntity?,
    onSave: (ml: Int, (Long) -> EventEntity) -> Unit,
) {
    val source = initial ?: lastBottle
    var ml by remember { mutableIntStateOf(source?.amountMl ?: 90) }
    var content by remember { mutableStateOf(source?.bottleContent ?: BottleContent.FORMULA) }
    var time by remember { mutableLongStateOf(initial?.startTime ?: System.currentTimeMillis()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }

    NumberStepper(ml, { ml = it }, step = 10, range = 0..400, suffix = "ml")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(60, 90, 120, 150, 180, 210).forEach { preset ->
            FilterChip(selected = ml == preset, onClick = { ml = preset }, label = { Text("$preset ml") })
        }
    }
    ChoiceRow(BottleContent.entries.map { it to it.label }, content, { content = it })
    TimeSelector("Hora", time, { time = it })
    NotesField(notes, { notes = it })
    SaveButton(enabled = ml > 0, color = EventType.BOTTLE.style().color) {
        onSave(ml) { babyId ->
            EventEntity(
                babyId = babyId,
                type = EventType.BOTTLE,
                startTime = time,
                endTime = time,
                amountMl = ml,
                bottleContent = content,
                notes = notes.ifBlank { null },
            )
        }
    }
}

@Composable
private fun SolidEntry(initial: EventEntity?, onSave: ((Long) -> EventEntity) -> Unit) {
    var food by remember { mutableStateOf(initial?.food.orEmpty()) }
    var isNew by remember { mutableStateOf(initial?.isNewFood ?: false) }
    var reaction by remember { mutableStateOf(initial?.reaction.orEmpty()) }
    var time by remember { mutableLongStateOf(initial?.startTime ?: System.currentTimeMillis()) }

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
    NotesField(reaction, { reaction = it }, label = "Reacción / notas (opcional)")
    TimeSelector("Hora", time, { time = it })
    SaveButton(enabled = food.isNotBlank(), color = EventType.SOLID.style().color) {
        onSave { babyId ->
            EventEntity(
                babyId = babyId,
                type = EventType.SOLID,
                startTime = time,
                endTime = time,
                food = food.trim(),
                isNewFood = isNew,
                reaction = reaction.ifBlank { null },
            )
        }
    }
}

@Composable
private fun SleepEntry(initial: EventEntity?, onStart: (() -> Unit)?, onSave: ((Long) -> EventEntity) -> Unit) {
    val color = EventType.SLEEP.style().color
    if (initial == null && onStart != null) {
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
    }

    val now = remember { System.currentTimeMillis() }
    var start by remember { mutableLongStateOf(initial?.startTime ?: (now - HOUR)) }
    var end by remember { mutableLongStateOf(initial?.endTime ?: now) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    TimeSelector("Se durmió", start, { start = it })
    TimeSelector("Se despertó", end, { end = it })
    val valid = end > start
    if (valid) {
        Text("Duración: ${Fmt.duration(end - start)}", style = MaterialTheme.typography.bodyMedium)
    } else {
        Text(
            "La hora de despertar debe ser posterior a la de dormir.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    NotesField(notes, { notes = it })
    SaveButton(enabled = valid, color = color) {
        onSave { babyId ->
            EventEntity(
                babyId = babyId,
                type = EventType.SLEEP,
                startTime = start,
                endTime = end,
                notes = notes.ifBlank { null },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiaperEntry(initial: EventEntity?, onSave: ((Long) -> EventEntity) -> Unit) {
    var kind by remember { mutableStateOf(initial?.diaperKind ?: DiaperKind.WET) }
    var stool by remember { mutableStateOf(initial?.stoolColor) }
    var time by remember { mutableLongStateOf(initial?.startTime ?: System.currentTimeMillis()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }

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
        val selected = stool
        if (selected?.isWarning == true) {
            WarningText(
                "Las heces de color ${selected.label.lowercase()} pueden requerir atención. Consulta con tu pediatra.",
            )
        }
    }
    TimeSelector("Hora", time, { time = it })
    NotesField(notes, { notes = it })
    SaveButton(color = EventType.DIAPER.style().color) {
        onSave { babyId ->
            EventEntity(
                babyId = babyId,
                type = EventType.DIAPER,
                startTime = time,
                endTime = time,
                diaperKind = kind,
                stoolColor = stool.takeIf { kind != DiaperKind.WET },
                notes = notes.ifBlank { null },
            )
        }
    }
}

@Composable
private fun WarningText(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
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

@Composable
private fun TemperatureEntry(initial: EventEntity?, onSave: ((Long) -> EventEntity) -> Unit) {
    // Se trabaja en décimas de grado para evitar errores de redondeo.
    var tenths by remember { mutableIntStateOf(initial?.temperatureC?.let { (it * 10).roundToInt() } ?: 368) }
    var time by remember { mutableLongStateOf(initial?.startTime ?: System.currentTimeMillis()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    val celsius = tenths / 10.0
    val level = feverLevel(celsius)

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        FilledTonalIconButton(onClick = { tenths = (tenths - 1).coerceAtLeast(340) }) {
            Icon(Icons.Rounded.Remove, contentDescription = "Menos")
        }
        Column(Modifier.widthIn(min = 140.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(Fmt.temperature(celsius), style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
            Text(
                level.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (level == FeverLevel.NORMAL) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            )
        }
        FilledTonalIconButton(onClick = { tenths = (tenths + 1).coerceAtMost(430) }) {
            Icon(Icons.Rounded.Add, contentDescription = "Más")
        }
    }
    if (level == FeverLevel.FEVER || level == FeverLevel.LOW) {
        WarningText(
            "En menores de 3 meses, cualquier fiebre (38 °C o más) o una temperatura baja " +
                "requiere consultar al pediatra cuanto antes.",
        )
    }
    TimeSelector("Hora", time, { time = it })
    NotesField(notes, { notes = it }, label = "Notas (p. ej. axilar, rectal…)")
    SaveButton(color = EventType.TEMPERATURE.style().color) {
        onSave { babyId ->
            EventEntity(
                babyId = babyId,
                type = EventType.TEMPERATURE,
                startTime = time,
                endTime = time,
                temperatureC = celsius,
                notes = notes.ifBlank { null },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MedicineEntry(initial: EventEntity?, onSave: ((Long) -> EventEntity) -> Unit) {
    var name by remember { mutableStateOf(initial?.medicineName.orEmpty()) }
    var dose by remember { mutableStateOf(initial?.dose.orEmpty()) }
    var time by remember { mutableLongStateOf(initial?.startTime ?: System.currentTimeMillis()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    val initialHours = initial?.let { e -> e.reminderAt?.let { ((it - e.startTime) / HOUR).toInt() } }
    var reminderHours by remember { mutableStateOf(initialHours) }
    val requestPermission = rememberNotificationPermissionRequest()

    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Medicamento") },
        placeholder = { Text("Ej.: paracetamol") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = dose,
        onValueChange = { dose = it },
        label = { Text("Dosis") },
        placeholder = { Text("Ej.: 2,5 ml") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    TimeSelector("Hora de la toma", time, { time = it })
    Text("Recordarme la próxima dosis", style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = reminderHours == null, onClick = { reminderHours = null }, label = { Text("No") })
        listOf(4, 6, 8, 12, 24).forEach { h ->
            FilterChip(
                selected = reminderHours == h,
                onClick = {
                    reminderHours = h
                    requestPermission()
                },
                label = { Text("En $h h") },
            )
        }
    }
    reminderHours?.let { h ->
        Text(
            "Te avisaremos: ${Fmt.dayTimeFuture(time + h * HOUR)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    NotesField(notes, { notes = it })
    Text(
        "Sigue siempre las indicaciones de tu pediatra sobre dosis e intervalos.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SaveButton(enabled = name.isNotBlank(), color = EventType.MEDICINE.style().color) {
        onSave { babyId ->
            EventEntity(
                babyId = babyId,
                type = EventType.MEDICINE,
                startTime = time,
                endTime = time,
                medicineName = name.trim(),
                dose = dose.ifBlank { null },
                reminderAt = reminderHours?.let { time + it * HOUR },
                notes = notes.ifBlank { null },
            )
        }
    }
}
