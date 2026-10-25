package com.kbretro.babyregistry.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.kbretro.babyregistry.data.model.BreastSide
import com.kbretro.babyregistry.data.model.BreastTimer
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.ui.common.EventIcon
import com.kbretro.babyregistry.ui.common.Fmt
import com.kbretro.babyregistry.ui.common.label
import com.kbretro.babyregistry.ui.common.style

private val TabularNumbers: TextStyle
    @Composable get() = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum")

@Composable
fun BreastTimerCard(
    event: EventEntity,
    now: Long,
    onSide: (BreastSide) -> Unit,
    onStop: () -> Unit,
    onDiscard: () -> Unit,
) {
    val color = EventType.BREAST.style().color
    TimerCardFrame(
        type = EventType.BREAST,
        title = "Toma de pecho",
        subtitle = if (event.activeSide == null) "En pausa" else "Desde las ${Fmt.time(event.startTime)}",
        clock = Fmt.clock(BreastTimer.totalMillis(event, now)),
        stopLabel = "Terminar",
        stopIcon = Icons.Rounded.Stop,
        onStop = onStop,
        onDiscard = onDiscard,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BreastSide.entries.forEach { side ->
                SideButton(
                    side = side,
                    millis = BreastTimer.sideMillis(event, side, now),
                    active = event.activeSide == side,
                    color = color,
                    onClick = { onSide(side) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun SleepTimerCard(event: EventEntity, now: Long, onStop: () -> Unit, onDiscard: () -> Unit) {
    TimerCardFrame(
        type = EventType.SLEEP,
        title = "Durmiendo",
        subtitle = "Desde las ${Fmt.time(event.startTime)}",
        clock = Fmt.clock(now - event.startTime),
        stopLabel = "Se despertó",
        stopIcon = Icons.Rounded.WbSunny,
        onStop = onStop,
        onDiscard = onDiscard,
    )
}

@Composable
private fun TimerCardFrame(
    type: EventType,
    title: String,
    subtitle: String,
    clock: String,
    stopLabel: String,
    stopIcon: ImageVector,
    onStop: () -> Unit,
    onDiscard: () -> Unit,
    content: @Composable () -> Unit = {},
) {
    val color = type.style().color
    var confirmDiscard by remember { mutableStateOf(false) }
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = color.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EventIcon(type, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(clock, style = TabularNumbers, color = color)
            }
            Spacer(Modifier.height(16.dp))
            content()
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { confirmDiscard = true }) { Text("Descartar") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onStop, colors = ButtonDefaults.buttonColors(containerColor = color)) {
                    Icon(stopIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stopLabel)
                }
            }
        }
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("¿Descartar registro?") },
            text = { Text("Se eliminará este registro en curso sin guardarlo.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onDiscard()
                }) { Text("Descartar") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun SideButton(
    side: BreastSide,
    millis: Long,
    active: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (active) color else color.copy(alpha = 0.10f),
        contentColor = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    ) {
        Column(Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (active) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(side.label, style = MaterialTheme.typography.labelLarge)
            }
            Text(
                Fmt.clock(millis),
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
            )
        }
    }
}
