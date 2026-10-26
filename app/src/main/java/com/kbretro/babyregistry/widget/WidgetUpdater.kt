package com.kbretro.babyregistry.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.kbretro.babyregistry.data.repository.TrackerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Refresca el widget cuando cambian los registros del bebé seleccionado. */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: TrackerRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    fun start(scope: CoroutineScope) {
        scope.launch {
            repo.currentBaby
                .flatMapLatest { baby ->
                    if (baby == null) flowOf(null) else repo.recent(baby.id, 20).map { baby to it }
                }
                .distinctUntilChanged()
                .debounce(500)
                .collect { runCatching { BabyWidget().updateAll(context) } }
        }
    }
}
