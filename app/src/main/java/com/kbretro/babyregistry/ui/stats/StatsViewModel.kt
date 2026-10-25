package com.kbretro.babyregistry.ui.stats

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.repository.TrackerRepository
import com.kbretro.babyregistry.domain.DayRange
import com.kbretro.babyregistry.export.ReportExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class StatsUiState(
    val rangeDays: Int = 7,
    val days: List<LocalDate> = emptyList(),
    val events: List<EventEntity> = emptyList(),
    val loaded: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repo: TrackerRepository,
    private val exporter: ReportExporter,
) : ViewModel() {

    private val range = MutableStateFlow(7)

    val state: StateFlow<StatsUiState> = combine(repo.currentBaby, range) { baby, days -> baby to days }
        .flatMapLatest { (baby, days) ->
            if (baby == null) return@flatMapLatest flowOf(StatsUiState(rangeDays = days))
            val today = LocalDate.now()
            val dates = (days - 1 downTo 0).map { today.minusDays(it.toLong()) }
            val from = DayRange.of(dates.first()).first
            val to = DayRange.of(today).second
            repo.overlapping(baby.id, from, to).map { events ->
                StatsUiState(rangeDays = days, days = dates, events = events, loaded = true)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun setRange(days: Int) {
        range.value = days
    }

    /** Genera el informe PDF del periodo seleccionado; devuelve su Uri para compartir. */
    suspend fun exportReport(): Uri? = exporter.export(range.value)
}
