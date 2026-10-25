package com.kbretro.babyregistry.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kbretro.babyregistry.MainActivity
import com.kbretro.babyregistry.R
import com.kbretro.babyregistry.data.model.BabyEntity
import com.kbretro.babyregistry.data.model.BreastSide
import com.kbretro.babyregistry.data.model.BreastTimer
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.data.repository.TrackerRepository
import com.kbretro.babyregistry.ui.common.Fmt
import com.kbretro.babyregistry.ui.common.label
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Muestra una notificación fija con cronómetro mientras hay un temporizador en curso
 * (pecho o sueño), con botones para cambiar de lado, pausar o terminar sin abrir la app.
 */
@Singleton
class TimerNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: TrackerRepository,
) {
    private val posted = mutableSetOf<Int>()

    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(repo.allActive(), repo.babies) { active, babies -> active to babies.associateBy { it.id } }
                .collect { (active, babies) -> render(active, babies) }
        }
    }

    private fun render(active: List<EventEntity>, babies: Map<Long, BabyEntity>) {
        val manager = NotificationManagerCompat.from(context)
        val now = System.currentTimeMillis()
        val ids = active.map { notificationId(it.id) }.toSet()
        (posted - ids).forEach(manager::cancel)
        posted.retainAll(ids)
        if (!canNotify()) return
        active.forEach { event ->
            val name = babies[event.babyId]?.name.orEmpty()
            manager.notify(notificationId(event.id), build(event, name, now))
            posted += notificationId(event.id)
        }
    }

    private fun build(event: EventEntity, babyName: String, now: Long) =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_baby)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setContentIntent(openApp())
            .apply {
                when (event.type) {
                    EventType.BREAST -> breast(event, babyName, now)
                    else -> sleep(event, babyName)
                }
            }
            .build()

    private fun NotificationCompat.Builder.breast(event: EventEntity, babyName: String, now: Long) {
        val total = BreastTimer.totalMillis(event, now)
        val side = event.activeSide
        setContentTitle(listOf("Toma de pecho", babyName).filter { it.isNotBlank() }.joinToString(" · "))
        if (side != null) {
            setUsesChronometer(true)
            setShowWhen(true)
            setWhen(now - total)
            setContentText("${side.label} · en curso")
            addAction(0, "Cambiar a ${side.opposite.label.lowercase()}", action(event.id, ACTION_SIDE, side.opposite))
            addAction(0, "Pausar", action(event.id, ACTION_SIDE, side))
        } else {
            setUsesChronometer(false)
            setShowWhen(false)
            setContentText("En pausa · ${Fmt.clock(total)}")
            val resume = event.lastSide ?: BreastSide.LEFT
            addAction(0, "Seguir (${resume.label.lowercase()})", action(event.id, ACTION_SIDE, resume))
        }
        addAction(0, "Terminar", action(event.id, ACTION_STOP))
    }

    private fun NotificationCompat.Builder.sleep(event: EventEntity, babyName: String) {
        setContentTitle(listOf("Durmiendo", babyName).filter { it.isNotBlank() }.joinToString(" · "))
        setContentText("Desde las ${Fmt.time(event.startTime)}")
        setUsesChronometer(true)
        setShowWhen(true)
        setWhen(event.startTime)
        addAction(0, "Se despertó", action(event.id, ACTION_STOP))
    }

    private fun openApp() = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private fun action(eventId: Long, action: String, side: BreastSide? = null): PendingIntent {
        val intent = Intent(context, TimerActionReceiver::class.java)
            .setAction(action)
            .setData(android.net.Uri.parse("babyregistry://timer/$eventId/${side ?: ""}"))
            .putExtra(EXTRA_EVENT_ID, eventId)
            .putExtra(EXTRA_SIDE, side?.name)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun canNotify() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val CHANNEL_ID = "timers"
        const val ACTION_SIDE = "com.kbretro.babyregistry.TIMER_SIDE"
        const val ACTION_STOP = "com.kbretro.babyregistry.TIMER_STOP"
        const val EXTRA_EVENT_ID = "eventId"
        const val EXTRA_SIDE = "side"

        private fun notificationId(eventId: Long) = 100_000 + eventId.toInt()

        fun createChannel(context: Context) {
            val channel = NotificationChannel(CHANNEL_ID, "Temporizadores en curso", NotificationManager.IMPORTANCE_LOW)
            channel.description = "Cronómetro de tomas y sueño mientras están en marcha"
            channel.setShowBadge(false)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
