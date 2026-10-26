package com.kbretro.babyregistry.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.domain.DaySummary
import com.kbretro.babyregistry.domain.Prediction
import com.kbretro.babyregistry.domain.Predictions
import com.kbretro.babyregistry.ui.common.AnimatedText
import com.kbretro.babyregistry.ui.common.Fmt
import com.kbretro.babyregistry.ui.common.PulsingDot
import com.kbretro.babyregistry.ui.common.detail
import com.kbretro.babyregistry.ui.common.label
import com.kbretro.babyregistry.ui.common.pressScale
import com.kbretro.babyregistry.ui.common.style
import com.kbretro.babyregistry.ui.common.title

@Composable
fun QuickActions(types: List<EventType>, activeTypes: Set<EventType>, onClick: (EventType) -> Unit) {
    if (types.isEmpty()) {
        Text(
            "Todos los registros diarios están ocultos. Toca la inicial del bebé para mostrarlos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        types.forEach { type ->
            QuickActionTile(type, active = type in activeTypes, onClick = { onClick(type) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun QuickActionTile(type: EventType, active: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val style = type.style()
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        enabled = !active,
        interactionSource = interaction,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.pressScale(interaction, pressed = 0.9f),
    ) {
        Column(
            Modifier.padding(vertical = 14.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(46.dp)
                    .background(
                        Brush.linearGradient(listOf(style.color.copy(alpha = 0.95f), style.color.copy(alpha = 0.65f))),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(style.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(8.dp))
            if (active) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PulsingDot(style.color, size = 6.dp)
                    Spacer(Modifier.width(4.dp))
                    Text("En curso", style = MaterialTheme.typography.labelSmall, color = style.color, maxLines = 1)
                }
            } else {
                Text(style.label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** "Desde la última vez": tiempo desde la última toma, pañal y sueño. */
@Composable
fun SinceRow(state: HomeUiState, now: Long) {
    val feed = state.lastFeed
    val diaper = state.lastDiaper
    val sleep = state.lastSleep
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SinceTile(
            emoji = "🍼",
            label = "Última toma",
            value = when {
                feed == null -> "—"
                feed.isOngoing -> "Ahora"
                else -> Fmt.duration(now - feed.startTime)
            },
            caption = feed?.let { feedCaption(it) } ?: "Sin registros",
            color = EventType.BOTTLE.style().color,
            modifier = Modifier.weight(1f),
        )
        SinceTile(
            emoji = "🧷",
            label = "Último pañal",
            value = diaper?.let { Fmt.duration(now - it.startTime) } ?: "—",
            caption = diaper?.let { d -> listOfNotNull(d.diaperKind?.label, Fmt.time(d.startTime)).joinToString(" · ") } ?: "Sin registros",
            color = EventType.DIAPER.style().color,
            modifier = Modifier.weight(1f),
        )
        val sleepEnd = sleep?.endTime
        SinceTile(
            emoji = if (sleep != null && sleepEnd == null) "😴" else "🌙",
            label = when {
                sleep == null -> "Sueño"
                sleepEnd == null -> "Durmiendo"
                else -> "Último sueño"
            },
            value = when {
                sleep == null -> "—"
                sleepEnd == null -> Fmt.duration(now - sleep.startTime)
                else -> Fmt.duration(sleepEnd - sleep.startTime)
            },
            caption = when {
                sleep == null -> "Sin registros"
                sleepEnd == null -> "desde las ${Fmt.time(sleep.startTime)}"
                else -> "despertó ${Fmt.time(sleepEnd)}"
            },
            color = EventType.SLEEP.style().color,
            modifier = Modifier.weight(1f),
        )
    }
}

/** "120 ml · 14:05" para un biberón, "15 min · 14:05" para el pecho. */
private fun feedCaption(feed: EventEntity): String {
    val amount = when (feed.type) {
        EventType.BOTTLE -> feed.amountMl?.let { "$it ml" }
        EventType.BREAST -> (feed.leftMillis + feed.rightMillis).takeIf { it > 0 }?.let { Fmt.duration(it) }
        else -> null
    }
    return listOfNotNull(amount, Fmt.time(feed.startTime)).joinToString(" · ")
}

@Composable
private fun SinceTile(emoji: String, label: String, value: String, caption: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.11f)),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(emoji, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            AnimatedText(value, style = MaterialTheme.typography.titleMedium)
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                type = EventType.BOTTLE,
                title = "Tomas",
                value = summary.feeds.toString(),
                caption = buildList {
                    if (summary.bottleMl > 0) add("${summary.bottleMl} ml")
                    if (summary.breastMillis > 0) add("${Fmt.duration(summary.breastMillis)} pecho")
                }.joinToString(" · ").ifEmpty { "Ninguna aún" },
                modifier = Modifier.weight(1f),
            )
            StatTile(
                type = EventType.SLEEP,
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
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                type = EventType.DIAPER,
                title = "Pañales",
                value = summary.diapers.toString(),
                caption = "Pis ${summary.wet} · Caca ${summary.dirty}",
                modifier = Modifier.weight(1f),
            )
            StatTile(
                type = EventType.SOLID,
                title = "Sólidos",
                value = summary.solids.toString(),
                caption = if (summary.solids == 1) "comida" else "comidas",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatTile(type: EventType, title: String, value: String, caption: String, modifier: Modifier = Modifier) {
    val style = type.style()
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(26.dp)
                        .background(style.color.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(style.icon, contentDescription = null, tint = style.color, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(8.dp))
            AnimatedText(value, style = MaterialTheme.typography.headlineSmall)
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

/** Actividad reciente como línea de tiempo: punto de color por tipo unido por una línea. */
@Composable
fun RecentTimeline(events: List<EventEntity>, now: Long, onClick: (EventEntity) -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        if (events.isEmpty()) {
            Text(
                "Todavía no hay registros. Usa los botones de arriba para empezar ✨",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp),
            )
            return@Card
        }
        Column(Modifier.padding(vertical = 8.dp)) {
            events.forEachIndexed { index, event ->
                TimelineRow(event, now, first = index == 0, last = index == events.lastIndex, onClick = { onClick(event) })
            }
        }
    }
}

@Composable
private fun TimelineRow(event: EventEntity, now: Long, first: Boolean, last: Boolean, onClick: () -> Unit) {
    val style = event.type.style()
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(36.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.width(2.dp).weight(1f).background(if (first) Color.Transparent else lineColor))
                Box(Modifier.width(2.dp).weight(1f).background(if (last) Color.Transparent else lineColor))
            }
            Box(
                Modifier
                    .size(32.dp)
                    .background(style.color.copy(alpha = 0.16f), CircleShape)
                    .padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(style.icon, contentDescription = null, tint = style.color, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(event.title(), style = MaterialTheme.typography.titleSmall)
            Text(
                event.detail(now),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(Fmt.time(event.startTime), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** "Próximamente": estimación de la próxima toma y del próximo sueño según los últimos días. */
@Composable
fun PredictionsCard(state: HomeUiState, now: Long) {
    val feed = remember(state.window) { Predictions.nextFeed(state.window, System.currentTimeMillis()) }
    val sleep = remember(state.window) { Predictions.nextSleep(state.window, System.currentTimeMillis()) }
    if (feed == null && sleep == null) return
    val colors = MaterialTheme.colorScheme
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            Modifier
                .background(Brush.linearGradient(listOf(colors.tertiaryContainer, colors.primaryContainer)))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("🔮 Próximamente", style = MaterialTheme.typography.titleSmall, color = colors.onTertiaryContainer)
            feed?.let { PredictionLine("🍼", "Próxima toma", it, now) }
            sleep?.let { PredictionLine("😴", "Próximo sueño", it, now) }
            Text(
                "Según los patrones de los últimos 3 días",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onTertiaryContainer.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun PredictionLine(emoji: String, label: String, prediction: Prediction, now: Long) {
    val diff = prediction.at - now
    val relative = when {
        diff > 60_000 -> "en ${Fmt.duration(diff)}"
        diff < -60_000 -> "hace ${Fmt.duration(-diff)}"
        else -> "ahora"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)) {
            Text(
                "~${Fmt.time(prediction.at)} · $relative",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}
