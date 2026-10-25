package com.kbretro.babyregistry.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.ui.common.EventDetailDialog
import com.kbretro.babyregistry.ui.common.EventRow
import com.kbretro.babyregistry.ui.common.Fmt
import com.kbretro.babyregistry.ui.common.rememberNow
import com.kbretro.babyregistry.ui.common.style
import com.kbretro.babyregistry.ui.entry.EntryRequest
import com.kbretro.babyregistry.ui.entry.EntrySheet
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val now = rememberNow()
    var detail by remember { mutableStateOf<EventEntity?>(null) }
    var editing by remember { mutableStateOf<EventEntity?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("Historial") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 24.dp),
        ) {
            item(key = "filters") {
                FilterBar(selected = state.filter, onSelect = viewModel::setFilter)
            }
            if (state.loaded && state.days.isEmpty()) {
                item(key = "empty") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Aún no hay registros",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            state.days.forEach { (date, events) ->
                stickyHeader(key = date.toString()) { DayHeader(date, events) }
                items(events, key = { it.id }) { event ->
                    EventRow(event, now, onClick = { detail = event })
                }
            }
        }
    }

    detail?.let { event ->
        EventDetailDialog(
            event = event,
            now = now,
            onDismiss = { detail = null },
            onDelete = {
                viewModel.delete(event)
                detail = null
            },
            onEdit = {
                detail = null
                editing = event
            },
        )
    }
    editing?.let { event ->
        EntrySheet(
            request = EntryRequest(event.type, event),
            onDone = {},
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun FilterBar(selected: EventType?, onSelect: (EventType?) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("Todo") })
        EventType.entries.forEach { type ->
            val style = type.style()
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(if (selected == type) null else type) },
                label = { Text(style.label) },
                leadingIcon = {
                    Icon(style.icon, contentDescription = null, tint = style.color, modifier = Modifier.size(18.dp))
                },
            )
        }
    }
}

@Composable
private fun DayHeader(date: LocalDate, events: List<EventEntity>) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(Fmt.dayHeader(date), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(
                if (events.size == 1) "1 registro" else "${events.size} registros",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
