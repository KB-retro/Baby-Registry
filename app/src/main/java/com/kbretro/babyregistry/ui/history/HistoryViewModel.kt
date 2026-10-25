package com.kbretro.babyregistry.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.data.repository.TrackerRepository
import com.kbretro.babyregistry.ui.common.Fmt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HistoryUiState(
    val filter: EventType? = null,
    val days: Map<LocalDate, List<EventEntity>> = emptyMap(),
    val loaded: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repo: TrackerRepository,
) : ViewModel() {

    private val filter = MutableStateFlow<EventType?>(null)

    private val events = repo.currentBaby.flatMapLatest { baby ->
        if (baby == null) flowOf(emptyList()) else repo.all(baby.id)
    }

    val state: StateFlow<HistoryUiState> = combine(events, filter) { all, type ->
        HistoryUiState(
            filter = type,
            days = all.filter { type == null || it.type == type }.groupBy { Fmt.localDate(it.startTime) },
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setFilter(type: EventType?) {
        filter.value = type
    }

    fun delete(event: EventEntity) = viewModelScope.launch { repo.delete(event) }
}
