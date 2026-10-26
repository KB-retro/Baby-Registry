package com.kbretro.babyregistry.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kbretro.babyregistry.data.model.BreastSide
import com.kbretro.babyregistry.data.model.BreastTimer
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.ui.common.Fmt
import com.kbretro.babyregistry.ui.common.PulsingDot
import com.kbretro.babyregistry.ui.common.label
import com.kbretro.babyregistry.ui.common.pressScale
import com.kbretro.babyregistry.ui.common.rememberNow
import com.kbretro.babyregistry.ui.common.style

private const val HOUR = 3_600_000f

/**
 * Toma de pecho en curso. El reloj se actualiza cada segundo dentro de la tarjeta para no
 * redibujar toda la pantalla de inicio.
 */
@Composable
fun BreastTimerCard(
    event: EventEntity,
    onSide: (BreastSide) -> Unit,
    onStop: () -> Unit,
    onDiscard: () -> Unit,
) {
    val now = rememberNow()
    val color = EventType.BREAST.style().color
    val total = BreastTimer.totalMillis(event, now)
    TimerCardFrame(
        type = EventType.BREAST,
        title = "Toma de pecho",
        subtitle = if (event.activeSide == null) "En pausa" else "Desde las ${Fmt.time(event.startTime)}",
        elapsed = total,
        running = event.activeSide != null,
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
fun SleepTimerCard(event: EventEntity, onStop: () -> Unit, onDiscard: () -> Unit) {
    val now = rememberNow()
    TimerCardFrame(
        type = EventType.SLEEP,
        title = "Durmiendo",
        subtitle = "Desde las ${Fmt.time(event.startTime)}",
        elapsed = now - event.startTime,
        running = true,
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
    elapsed: Long,
    running: Boolean,
    stopLabel: String,
    stopIcon: ImageVector,
    onStop: () -> Unit,
    onDiscard: () -> Unit,
    content: @Composable () -> Unit = {},
) {
    val color = type.style().color
    var confirmDiscard by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TimerRing(elapsed = elapsed, color = color, modifier = Modifier.size(96.dp))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, style = MaterialTheme.typography.titleLarge)
                        if (running) {
                            Spacer(Modifier.width(8.dp))
                            PulsingDot(color, size = 8.dp)
                        }
                    }
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            content()
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { confirmDiscard = true }) { Text("Descartar") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onStop, colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White)) {
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

/** Anillo que se completa cada hora, con el cronómetro en el centro. */
@Composable
private fun TimerRing(elapsed: Long, color: Color, modifier: Modifier = Modifier) {
    val track = color.copy(alpha = 0.18f)
    val progress = (elapsed % HOUR.toLong()) / HOUR
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(track, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
            drawArc(
                color,
                -90f,
                360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            Fmt.clock(elapsed),
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
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
    val interaction = remember { MutableInteractionSource() }
    val background by animateColorAsState(if (active) color else color.copy(alpha = 0.10f), label = "sideBackground")
    val content by animateColorAsState(if (active) Color.White else MaterialTheme.colorScheme.onSurface, label = "sideContent")
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = RoundedCornerShape(20.dp),
        color = background,
        contentColor = content,
        modifier = modifier.pressScale(interaction),
    ) {
        Column(Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (active) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text(side.label, style = MaterialTheme.typography.labelLarge)
            }
            Text(Fmt.clock(millis), style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"))
        }
    }
}
