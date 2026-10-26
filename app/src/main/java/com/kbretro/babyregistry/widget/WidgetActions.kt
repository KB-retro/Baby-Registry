package com.kbretro.babyregistry.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.kbretro.babyregistry.data.model.DiaperKind
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.data.repository.TrackerRepository
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

private fun repository(context: Context): TrackerRepository =
    EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).repository()

/** Empieza el sueño o, si ya está durmiendo, lo termina. */
class ToggleSleepAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val repo = repository(context)
        val babyId = repo.currentBabyId() ?: return
        val now = System.currentTimeMillis()
        val sleeping = repo.active(babyId).first().firstOrNull { it.type == EventType.SLEEP }
        if (sleeping != null) {
            repo.update(sleeping.copy(endTime = now))
        } else {
            repo.insert(EventEntity(babyId = babyId, type = EventType.SLEEP, startTime = now))
        }
        BabyWidget().update(context, glanceId)
    }
}

/** Registra un pañal de pis en este momento. */
class WetDiaperAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val repo = repository(context)
        val babyId = repo.currentBabyId() ?: return
        val now = System.currentTimeMillis()
        repo.insert(
            EventEntity(babyId = babyId, type = EventType.DIAPER, startTime = now, endTime = now, diaperKind = DiaperKind.WET),
        )
        BabyWidget().update(context, glanceId)
    }
}
