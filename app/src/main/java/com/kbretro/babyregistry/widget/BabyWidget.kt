package com.kbretro.babyregistry.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.kbretro.babyregistry.MainActivity
import com.kbretro.babyregistry.ui.common.Fmt
import dagger.hilt.android.EntryPointAccessors

/** Widget de inicio: estado del bebé y accesos rápidos a sueño y pañal. */
class BabyWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).repository()
        val snapshot = repo.widgetSnapshot()
        provideContent {
            GlanceTheme {
                WidgetContent(snapshot, System.currentTimeMillis())
            }
        }
    }
}

class BabyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BabyWidget()
}

@Composable
private fun WidgetContent(snapshot: WidgetSnapshot, now: Long) {
    val colors = GlanceTheme.colors
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.widgetBackground)
            .cornerRadius(20.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        val baby = snapshot.baby
        if (baby == null) {
            Text("Abre la app para empezar", style = TextStyle(color = colors.onSurface))
            return@Column
        }
        Text(
            baby.name,
            style = TextStyle(color = colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold),
        )
        Spacer(GlanceModifier.height(4.dp))
        val breast = snapshot.activeBreast
        val sleep = snapshot.activeSleep
        when {
            breast != null -> Line("Tomando pecho desde las ${Fmt.time(breast.startTime)}")
            sleep != null -> Line("Durmiendo desde las ${Fmt.time(sleep.startTime)}")
            else -> snapshot.lastSleep?.endTime?.let { Line("Despierto desde las ${Fmt.time(it)}") }
        }
        snapshot.lastFeed?.takeIf { !it.isOngoing }?.let {
            Line("Última toma: ${Fmt.time(it.startTime)} (${Fmt.ago(it.startTime, now).lowercase()})")
        }
        snapshot.lastDiaper?.let {
            Line("Último pañal: ${Fmt.time(it.startTime)} (${Fmt.ago(it.startTime, now).lowercase()})")
        }
        Spacer(GlanceModifier.height(8.dp))
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Button(
                text = if (sleep != null) "Despertó" else "Dormir",
                onClick = actionRunCallback<ToggleSleepAction>(),
                modifier = GlanceModifier.defaultWeight(),
            )
            Spacer(GlanceModifier.width(8.dp))
            Button(
                text = "Pañal pis",
                onClick = actionRunCallback<WetDiaperAction>(),
                modifier = GlanceModifier.defaultWeight(),
            )
        }
    }
}

@Composable
private fun Line(text: String) {
    Text(text, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp), maxLines = 1)
}
