package com.kbretro.babyregistry.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BabyChangingStation
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.domain.DaySummary
import com.kbretro.babyregistry.ui.common.EventRow
import com.kbretro.babyregistry.ui.common.Fmt
import com.kbretro.babyregistry.ui.common.style

@Composable
fun QuickActions(activeTypes: Set<EventType>, onClick: (EventType) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EventType.entries.forEach { type ->
            val style = type.style()
            val active = type in activeTypes
            Surface(
                onClick = { onClick(type) },
                enabled = !active,
                shape = RoundedCornerShape(20.dp),
                color = style.color.copy(alpha = if (active) 0.28f else 0.14f),
                modifier = Modifier.weight(1f),
            ) {
                Column(
                    Modifier.padding(vertical = 14.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(style.icon, contentDescription = null, tint = style.color, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (active) "En curso" else style.label,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** "Desde la última vez": tiempo desde la última toma, pañal y estado de sueño. */
@Composable
fun SinceRow(state: HomeUiState, now: Long) {
    val feed = state.lastFeed
    val diaper = state.lastDiaper
    val sleep = state.lastSleep
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SinceTile(
            label = "Última toma",
            value = when {
                feed == null -> "—"
                feed.isOngoing -> "Ahora"
                else -> Fmt.duration(now - feed.startTime)
            },
            caption = feed?.let { "a las ${Fmt.time(it.startTime)}" } ?: "Sin registros",
            color = EventType.BOTTLE.style().color,
            modifier = Modifier.weight(1f),
        )
        SinceTile(
            label = "Último pañal",
            value = diaper?.let { Fmt.duration(now - it.startTime) } ?: "—",
            caption = diaper?.let { "a las ${Fmt.time(it.startTime)}" } ?: "Sin registros",
            color = EventType.DIAPER.style().color,
            modifier = Modifier.weight(1f),
        )
        val sleepEnd = sleep?.endTime
        SinceTile(
            label = when {
                sleep == null -> "Sueño"
                sleepEnd == null -> "Durmiendo"
                else -> "Despierto"
            },
            value = when {
                sleep == null -> "—"
                sleepEnd == null -> Fmt.duration(now - sleep.startTime)
                else -> Fmt.duration(now - sleepEnd)
            },
            caption = when {
                sleep == null -> "Sin registros"
                sleepEnd == null -> "desde las ${Fmt.time(sleep.startTime)}"
                else -> "desde las ${Fmt.time(sleepEnd)}"
            },
            color = EventType.SLEEP.style().color,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SinceTile(label: String, value: String, caption: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
            Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun TodaySummary(summary: DaySummary) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(
                icon = Icons.Rounded.LocalDrink,
                color = EventType.BOTTLE.style().color,
                title = "Tomas",
                value = summary.feeds.toString(),
                caption = buildList {
                    if (summary.bottleMl > 0) add("${summary.bottleMl} ml")
                    if (summary.breastMillis > 0) add("${Fmt.duration(summary.breastMillis)} pecho")
                }.joinToString(" · ").ifEmpty { "Ninguna aún" },
                modifier = Modifier.weight(1f),
            )
            StatTile(
                icon = Icons.Rounded.Bedtime,
                color = EventType.SLEEP.style().color,
                title = "Sueño",
                value = if (summary.sleepMillis > 0) Fmt.duration(summary.sleepMillis) else "0 min",
                caption = when (summary.sleeps) {
                    0 -> "Sin siestas aún"
                    1 -> "1 periodo"
                    else -> "${summary.sleeps} periodos"
                },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(
                icon = Icons.Rounded.BabyChangingStation,
                color = EventType.DIAPER.style().color,
                title = "Pañales",
                value = summary.diapers.toString(),
                caption = "Pis ${summary.wet} · Caca ${summary.dirty}",
                modifier = Modifier.weight(1f),
            )
            StatTile(
                icon = Icons.Rounded.Restaurant,
                color = EventType.SOLID.style().color,
                title = "Sólidos",
                value = summary.solids.toString(),
                caption = if (summary.solids == 1) "comida" else "comidas",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatTile(
    icon: ImageVector,
    color: Color,
    title: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(title, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(
                caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun RecentList(events: List<EventEntity>, now: Long, onClick: (EventEntity) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        if (events.isEmpty()) {
            Text(
                "Todavía no hay registros. Usa los botones de arriba para empezar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            Column(Modifier.padding(vertical = 4.dp)) {
                events.forEach { event ->
                    EventRow(event, now, onClick = { onClick(event) })
                }
            }
        }
    }
}
