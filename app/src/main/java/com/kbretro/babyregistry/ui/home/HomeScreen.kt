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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.domain.summarize
import com.kbretro.babyregistry.ui.common.EventDetailDialog
import com.kbretro.babyregistry.ui.common.Fmt
import com.kbretro.babyregistry.ui.common.SectionTitle
import com.kbretro.babyregistry.ui.common.rememberNow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onSeeAll: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val now = rememberNow()
    var sheet by rememberSaveable { mutableStateOf<EventType?>(null) }
    var detail by remember { mutableStateOf<EventEntity?>(null) }
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
                navigationIcon = { BabyAvatar(baby.name) },
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
                    QuickActions(state.activeTypes, onClick = { sheet = it })
                }
            }
            item(key = "since") { SinceRow(state, now) }
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

    sheet?.let { type ->
        EntrySheet(
            type = type,
            state = state,
            viewModel = viewModel,
            onDone = ::notify,
            onDismiss = { sheet = null },
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
        )
    }
}

@Composable
private fun BabyAvatar(name: String) {
    Surface(
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
