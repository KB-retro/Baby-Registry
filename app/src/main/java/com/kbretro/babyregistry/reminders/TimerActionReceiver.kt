package com.kbretro.babyregistry.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kbretro.babyregistry.data.model.BreastSide
import com.kbretro.babyregistry.data.model.BreastTimer
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.data.repository.TrackerRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Botones de la notificación del temporizador. */
@AndroidEntryPoint
class TimerActionReceiver : BroadcastReceiver() {
    @Inject lateinit var repo: TrackerRepository

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(TimerNotifier.EXTRA_EVENT_ID, -1)
        if (eventId < 0) return
        val side = intent.getStringExtra(TimerNotifier.EXTRA_SIDE)?.let(BreastSide::valueOf)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val event = repo.getEvent(eventId) ?: return@launch
                if (!event.isOngoing) return@launch
                val now = System.currentTimeMillis()
                val updated = when {
                    intent.action == TimerNotifier.ACTION_SIDE && side != null && event.type == EventType.BREAST ->
                        BreastTimer.tapSide(event, side, now)
                    intent.action == TimerNotifier.ACTION_STOP && event.type == EventType.BREAST ->
                        BreastTimer.stop(event, now)
                    intent.action == TimerNotifier.ACTION_STOP -> event.copy(endTime = now)
                    else -> return@launch
                }
                repo.update(updated)
            } finally {
                pending.finish()
            }
        }
    }
}
