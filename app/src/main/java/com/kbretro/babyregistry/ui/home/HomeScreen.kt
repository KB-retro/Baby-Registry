package com.kbretro.babyregistry.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ImportExport
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.kbretro.babyregistry.ui.common.PulsingDot
import com.kbretro.babyregistry.ui.common.SectionTitle
import com.kbretro.babyregistry.ui.common.rememberNotificationPermissionRequest
import com.kbretro.babyregistry.ui.common.rememberNow
import com.kbretro.babyregistry.ui.common.style
import com.kbretro.babyregistry.ui.entry.EntryDefaults
import com.kbretro.babyregistry.ui.entry.EntryRequest
import com.kbretro.babyregistry.ui.entry.EntrySheet
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun HomeScreen(onSeeAll: () -> Unit, onOpenData: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Solo los cronómetros se refrescan cada segundo (dentro de sus tarjetas); el resto, cada minuto.
    val now = rememberNow(60_000L)
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
    val summary = remember(state.today, state.dayStart, now) { summarize(state.today, state.dayStart, state.dayEnd, now) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "hero", contentType = "hero") {
                HeroHeader(
                    baby = baby,
                    state = state,
                    now = now,
                    onAvatar = { switcher = true },
                    onOpenData = onOpenData,
                )
            }
            items(state.active, key = { "timer${it.id}" }, contentType = { "timer" }) { event ->
                Box(
                    Modifier
                        .animateItem()
                        .padding(horizontal = 16.dp),
                ) {
                    when (event.type) {
                        EventType.BREAST -> BreastTimerCard(
                            event = event,
                            onSide = { side -> viewModel.tapBreastSide(event.id, side) },
                            onStop = {
                                viewModel.stopTimer(event.id)
                                notify("Toma de pecho guardada")
                            },
                            onDiscard = { viewModel.delete(event.id) },
                        )
                        EventType.SLEEP -> SleepTimerCard(
                            event = event,
                            onStop = {
                                viewModel.stopTimer(event.id)
                                notify("Sueño guardado")
                            },
                            onDiscard = { viewModel.delete(event.id) },
                        )
                        else -> Unit
                    }
                }
            }
            item(key = "actions", contentType = "section") {
                Section("Registrar", Modifier.animateItem()) {
                    QuickActions(
                        types = EventType.daily.filter(baby::isVisible),
                        activeTypes = state.activeTypes,
                        onClick = { entry = EntryRequest(it) },
                    )
                }
            }
            item(key = "since", contentType = "section") {
                Box(Modifier.animateItem().padding(horizontal = 16.dp)) { SinceRow(state, now) }
            }
            item(key = "predictions", contentType = "section") {
                Box(Modifier.animateItem().padding(horizontal = 16.dp)) { PredictionsCard(state, now) }
            }
            item(key = "today", contentType = "section") {
                Section("Hoy", Modifier.animateItem()) { TodaySummary(summary) }
            }
            item(key = "recent", contentType = "section") {
                Column(Modifier.animateItem().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle("Actividad reciente", Modifier.weight(1f))
                        TextButton(onClick = onSeeAll) { Text("Ver todo") }
                    }
                    RecentTimeline(state.recent, now, onClick = { detail = it })
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
private fun Section(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title)
        content()
    }
}

/** Cabecera con degradado: bebé, edad y estado actual (durmiendo, despierto, tomando…). */
@Composable
private fun HeroHeader(
    baby: BabyEntity,
    state: HomeUiState,
    now: Long,
    onAvatar: () -> Unit,
    onOpenData: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val gradient = Brush.linearGradient(listOf(colors.primaryContainer, colors.secondaryContainer))
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(gradient)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 24.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onAvatar,
                    shape = CircleShape,
                    color = colors.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(56.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            baby.name.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.headlineSmall,
                            color = colors.primary,
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(baby.name, style = MaterialTheme.typography.headlineSmall, color = colors.onPrimaryContainer)
                    Text(Fmt.age(baby.birthDate), style = MaterialTheme.typography.bodyMedium, color = colors.onPrimaryContainer.copy(alpha = 0.75f))
                }
                IconButton(onClick = onOpenData) {
                    Icon(Icons.Rounded.ImportExport, contentDescription = "Importar y exportar", tint = colors.onPrimaryContainer)
                }
            }
            Spacer(Modifier.height(18.dp))
            StatusPill(state, now)
        }
    }
}

@Composable
private fun StatusPill(state: HomeUiState, now: Long) {
    val breast = state.active.firstOrNull { it.type == EventType.BREAST }
    val sleep = state.active.firstOrNull { it.type == EventType.SLEEP }
    val lastWake = state.lastSleep?.endTime
    val (emoji, text, live) = when {
        breast != null -> Triple("🤱", "Tomando pecho desde las ${Fmt.time(breast.startTime)}", EventType.BREAST.style().color)
        sleep != null -> Triple("😴", "Durmiendo · ${Fmt.duration(now - sleep.startTime)}", EventType.SLEEP.style().color)
        lastWake != null -> Triple("☀️", "Despierto · ${Fmt.duration(now - lastWake)}", null)
        else -> Triple("👋", "¡Hola! Empieza registrando algo", null)
    }
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
            if (live != null) {
                Spacer(Modifier.width(8.dp))
                PulsingDot(live, size = 8.dp)
            }
        }
    }
}
