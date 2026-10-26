package com.kbretro.babyregistry.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ImportExport
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kbretro.babyregistry.data.model.BabyEntity
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.domain.summarize
import com.kbretro.babyregistry.ui.common.BabyEditDialog
import com.kbretro.babyregistry.ui.common.EventDetailDialog
import com.kbretro.babyregistry.ui.common.Fmt
import com.kbretro.babyregistry.ui.common.SectionTitle
import com.kbretro.babyregistry.ui.common.rememberNotificationPermissionRequest
import com.kbretro.babyregistry.ui.common.rememberNow
import com.kbretro.babyregistry.ui.entry.EntryDefaults
import com.kbretro.babyregistry.ui.entry.EntryRequest
import com.kbretro.babyregistry.ui.entry.EntrySheet
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onSeeAll: () -> Unit, onOpenData: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val now = rememberNow()
    var entry by remember { mutableStateOf<EntryRequest?>(null) }
    var detail by remember { mutableStateOf<EventEntity?>(null) }
    var switcher by remember { mutableStateOf(false) }
    var editingBaby by remember { mutableStateOf<BabyEntity?>(null) }
    val requestNotifications = rememberNotificationPermissionRequest()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun notify(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    val baby = state.baby ?: return

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                navigationIcon = { BabyAvatar(baby.name, onClick = { switcher = true }) },
                actions = {
                    IconButton(onClick = onOpenData) {
                        Icon(Icons.Rounded.ImportExport, contentDescription = "Importar y exportar")
                    }
                },
                title = {
                    Column {
                        Text(baby.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            Fmt.age(baby.birthDate),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(state.active, key = { it.id }) { event ->
                when (event.type) {
                    EventType.BREAST -> BreastTimerCard(
                        event = event,
                        now = now,
                        onSide = { side -> viewModel.tapBreastSide(event.id, side) },
                        onStop = {
                            viewModel.stopTimer(event.id)
                            notify("Toma de pecho guardada")
                        },
                        onDiscard = { viewModel.delete(event.id) },
                    )
                    EventType.SLEEP -> SleepTimerCard(
                        event = event,
                        now = now,
                        onStop = {
                            viewModel.stopTimer(event.id)
                            notify("Sueño guardado")
                        },
                        onDiscard = { viewModel.delete(event.id) },
                    )
                    else -> Unit
                }
            }
            item(key = "actions") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Registrar")
                    QuickActions(
                        types = EventType.daily.filter(baby::isVisible),
                        activeTypes = state.activeTypes,
                        onClick = { entry = EntryRequest(it) },
                    )
                }
            }
            item(key = "since") { SinceRow(state, now) }
            item(key = "predictions") { PredictionsCard(state, now) }
            item(key = "today") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Hoy")
                    TodaySummary(summarize(state.today, state.dayStart, state.dayEnd, now))
                }
            }
            item(key = "recent") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle("Actividad reciente", Modifier.weight(1f))
                        TextButton(onClick = onSeeAll) { Text("Ver todo") }
                    }
                    RecentList(state.recent, now, onClick = { detail = it })
                }
            }
        }
    }

    entry?.let { request ->
        EntrySheet(
            request = request,
            defaults = EntryDefaults(suggestedSide = state.suggestedSide, lastBottle = state.lastBottle),
            onStartBreast = { side ->
                requestNotifications()
                viewModel.startBreast(side)
            },
            onStartSleep = {
                requestNotifications()
                viewModel.startSleep()
            },
            onDone = ::notify,
            onDismiss = { entry = null },
        )
    }
    detail?.let { event ->
        EventDetailDialog(
            event = event,
            now = now,
            onDismiss = { detail = null },
            onDelete = {
                viewModel.delete(event.id)
                detail = null
                notify("Registro eliminado")
            },
            onEdit = {
                detail = null
                entry = EntryRequest(event.type, event)
            },
        )
    }
    if (switcher) {
        BabySwitcherSheet(
            babies = state.babies,
            selectedId = baby.id,
            onSelect = {
                viewModel.selectBaby(it.id)
                switcher = false
            },
            onEdit = {
                switcher = false
                editingBaby = it
            },
            onAdd = {
                switcher = false
                editingBaby = BabyEntity(name = "", birthDate = LocalDate.now())
            },
            onDismiss = { switcher = false },
        )
    }
    editingBaby?.let { editing ->
        BabyEditDialog(
            baby = editing,
            onDismiss = { editingBaby = null },
            onSave = {
                viewModel.saveBaby(it)
                editingBaby = null
                notify(if (editing.id == 0L) "Bebé añadido" else "Perfil actualizado")
            },
            onDelete = if (editing.id != 0L && state.babies.size > 1) {
                {
                    viewModel.deleteBaby(editing)
                    editingBaby = null
                    notify("Bebé eliminado")
                }
            } else {
                null
            },
        )
    }
}

@Composable
private fun BabyAvatar(name: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .padding(start = 12.dp, end = 4.dp)
            .size(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                name.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
