package com.kbretro.babyregistry.ui.growth

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kbretro.babyregistry.data.model.BabyEntity
import com.kbretro.babyregistry.data.model.GrowthEntity
import com.kbretro.babyregistry.data.model.MilestoneEntity
import com.kbretro.babyregistry.data.model.Sex
import com.kbretro.babyregistry.domain.GrowthIndicator
import com.kbretro.babyregistry.domain.WhoGrowth
import com.kbretro.babyregistry.ui.common.ChoiceRow
import com.kbretro.babyregistry.ui.common.DateField
import com.kbretro.babyregistry.ui.common.Fmt
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

private val MeasurementColor = Color(0xFFE5739D)

private val GrowthIndicator.shortLabel: String
    get() = when (this) {
        GrowthIndicator.WEIGHT -> "Peso"
        GrowthIndicator.LENGTH -> "Talla"
        GrowthIndicator.HEAD -> "Cabeza"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrowthScreen(viewModel: GrowthViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val baby = state.baby ?: return
    var indicator by rememberSaveable { mutableStateOf(GrowthIndicator.WEIGHT) }
    var editing by remember { mutableStateOf<GrowthEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editingMilestone by remember { mutableStateOf<MilestoneEntity?>(null) }
    var addingMilestone by remember { mutableStateOf(false) }
    val showMilestones = tab == 1

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                TopAppBar(title = { Text("Crecimiento") })
                PrimaryTabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Medidas") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Hitos") })
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (showMilestones) addingMilestone = true else adding = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(if (showMilestones) "Añadir hito" else "Añadir medida") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (showMilestones) {
                if (state.milestones.isEmpty()) {
                    item(key = "milestones-empty") { MilestonesEmpty() }
                }
                items(state.milestones, key = { "m${it.id}" }) { milestone ->
                    MilestoneCard(baby, milestone, onClick = { editingMilestone = milestone })
                }
                return@LazyColumn
            }
            item(key = "indicator") {
                ChoiceRow(GrowthIndicator.entries.map { it to it.shortLabel }, indicator, { indicator = it })
            }
            item(key = "summary") { LatestSummary(baby, state.entries, indicator) }
            item(key = "chart") {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(indicator.label, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        if (baby.sex == Sex.UNSPECIFIED) {
                            Text(
                                "Indica el sexo en el perfil del bebé (toca su inicial en Inicio) para ver los percentiles de la OMS.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        GrowthChart(
                            indicator = indicator,
                            table = WhoGrowth.table(indicator, baby.sex),
                            birthDate = baby.birthDate,
                            entries = state.entries,
                            measurementColor = MeasurementColor,
                            ringColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        )
                    }
                }
            }
            if (state.entries.isNotEmpty()) {
                item(key = "list-title") { Text("Medidas", style = MaterialTheme.typography.titleMedium) }
                items(state.entries, key = { it.id }) { entry ->
                    MeasurementRow(baby, entry, onClick = { editing = entry })
                }
            }
            item(key = "source") {
                Text(
                    "Curvas: Patrones de Crecimiento Infantil de la OMS (0-5 años). " +
                        "Son orientativas: consulta siempre con tu pediatra.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (addingMilestone || editingMilestone != null) {
        MilestoneSheet(
            babyId = baby.id,
            birthDate = baby.birthDate,
            initial = editingMilestone,
            onSave = { viewModel.saveMilestone(it) },
            onDelete = { viewModel.deleteMilestone(it) },
            onDismiss = {
                addingMilestone = false
                editingMilestone = null
            },
        )
    }

    if (adding || editing != null) {
        GrowthSheet(
            babyId = baby.id,
            birthDate = baby.birthDate,
            initial = editing,
            onSave = { viewModel.save(it) },
            onDelete = { viewModel.delete(it) },
            onDismiss = {
                adding = false
                editing = null
            },
        )
    }
}

@Composable
private fun LatestSummary(baby: BabyEntity, entries: List<GrowthEntity>, indicator: GrowthIndicator) {
    val withValue = entries.filter { indicator.valueOf(it) != null }
    val latest = withValue.firstOrNull() ?: return
    val previous = withValue.getOrNull(1)
    val value = indicator.valueOf(latest)!!
    val table = WhoGrowth.table(indicator, baby.sex)
    val percentile = table?.let { t ->
        WhoGrowth.zScore(t, WhoGrowth.ageInMonths(baby.birthDate, latest.date), value)?.let(WhoGrowth::percentile)
    }
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.MonitorWeight, contentDescription = null, tint = MeasurementColor)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Última medida · ${Fmt.shortDate(latest.date)}", style = MaterialTheme.typography.labelMedium)
                Text("${Fmt.decimal(value)} ${indicator.unit}", style = MaterialTheme.typography.headlineSmall)
                if (previous != null) {
                    val diff = value - indicator.valueOf(previous)!!
                    val days = ChronoUnit.DAYS.between(previous.date, latest.date)
                    val diffText = if (indicator == GrowthIndicator.WEIGHT) {
                        "${if (diff >= 0) "+" else "−"}${Math.round(kotlin.math.abs(diff) * 1000)} g"
                    } else {
                        "${if (diff >= 0) "+" else "−"}${Fmt.decimal(kotlin.math.abs(diff), 1)} cm"
                    }
                    Text(
                        "$diffText en $days ${if (days == 1L) "día" else "días"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (percentile != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Percentil", style = MaterialTheme.typography.labelMedium)
                    Text(formatPercentile(percentile), style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
    }
}

@Composable
private fun MeasurementRow(baby: BabyEntity, entry: GrowthEntity, onClick: () -> Unit) {
    val values = listOfNotNull(
        entry.weightKg?.let { "${Fmt.decimal(it)} kg" },
        entry.lengthCm?.let { "${Fmt.decimal(it, 1)} cm" },
        entry.headCm?.let { "PC ${Fmt.decimal(it, 1)} cm" },
    ).joinToString(" · ")
    Card(Modifier.fillMaxWidth()) {
        ListItem(
            modifier = Modifier.clickable(onClick = onClick),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = { Text(values) },
            supportingContent = { Text("${Fmt.shortDate(entry.date)} · ${Fmt.age(baby.birthDate, entry.date)}") },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GrowthSheet(
    babyId: Long,
    birthDate: LocalDate,
    initial: GrowthEntity?,
    onSave: (GrowthEntity) -> Unit,
    onDelete: (GrowthEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }
    var date by remember { mutableStateOf(initial?.date ?: LocalDate.now()) }
    var weight by remember { mutableStateOf(initial?.weightKg?.let { Fmt.decimal(it, 3) }.orEmpty()) }
    var length by remember { mutableStateOf(initial?.lengthCm?.let { Fmt.decimal(it, 1) }.orEmpty()) }
    var head by remember { mutableStateOf(initial?.headCm?.let { Fmt.decimal(it, 1) }.orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    var confirmDelete by remember { mutableStateOf(false) }

    val w = Fmt.parseDecimal(weight)
    val l = Fmt.parseDecimal(length)
    val h = Fmt.parseDecimal(head)
    // Rangos amplios para detectar errores de unidades (p. ej. gramos en lugar de kilos).
    val weightError = weight.isNotBlank() && (w == null || w !in 0.5..40.0)
    val lengthError = length.isNotBlank() && (l == null || l !in 30.0..130.0)
    val headError = head.isNotBlank() && (h == null || h !in 20.0..60.0)
    val hasValue = (w != null && !weightError) || (l != null && !lengthError) || (h != null && !headError)
    val valid = hasValue && !weightError && !lengthError && !headError && !date.isBefore(birthDate)

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
            Text(if (initial == null) "Nueva medida" else "Editar medida", style = MaterialTheme.typography.headlineSmall)
            DateField("Fecha", date, { date = it })
            MeasureField("Peso (kg)", weight, { weight = it }, weightError, "Ej.: 4,25")
            MeasureField("Talla (cm)", length, { length = it }, lengthError, "Ej.: 55,5")
            MeasureField("Perímetro craneal (cm)", head, { head = it }, headError, "Ej.: 38")
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas (opcional)") },
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
                    onSave(
                        GrowthEntity(
                            id = initial?.id ?: 0,
                            uuid = initial?.uuid ?: UUID.randomUUID().toString(),
                            babyId = babyId,
                            date = date,
                            weightKg = w.takeIf { !weightError },
                            lengthCm = l.takeIf { !lengthError },
                            headCm = h.takeIf { !headError },
                            notes = notes.ifBlank { null },
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
                        if (confirmDelete) "Confirmar eliminación" else "Eliminar medida",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun MeasureField(label: String, value: String, onChange: (String) -> Unit, isError: Boolean, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onChange(text.filter { it.isDigit() || it == ',' || it == '.' }) },
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        isError = isError,
        supportingText = if (isError) {
            { Text("Revisa el valor y la unidad") }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}
