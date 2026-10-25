package com.kbretro.babyregistry.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kbretro.babyregistry.data.model.BabyEntity
import com.kbretro.babyregistry.data.model.BreastSide
import com.kbretro.babyregistry.data.model.BreastTimer
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.data.repository.TrackerRepository
import com.kbretro.babyregistry.domain.DayRange
import com.kbretro.babyregistry.reminders.Reminders
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val baby: BabyEntity? = null,
    val active: List<EventEntity> = emptyList(),
    val today: List<EventEntity> = emptyList(),
    val dayStart: Long = 0,
    val dayEnd: Long = 0,
    val recent: List<EventEntity> = emptyList(),
    val lastFeed: EventEntity? = null,
    val lastDiaper: EventEntity? = null,
    val lastSleep: EventEntity? = null,
    val lastBreast: EventEntity? = null,
    val lastBottle: EventEntity? = null,
) {
    val activeTypes: Set<EventType> get() = active.mapTo(mutableSetOf()) { it.type }

    /** Lado por el que conviene empezar: el contrario al último usado. */
    val suggestedSide: BreastSide? get() = lastBreast?.takeIf { !it.isOngoing }?.lastSide?.opposite
}

private data class Latest(
    val feed: EventEntity?,
    val diaper: EventEntity?,
    val sleep: EventEntity?,
    val breast: EventEntity?,
    val bottle: EventEntity?,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: TrackerRepository,
    private val reminders: Reminders,
) : ViewModel() {

    /** Límites del día actual; se reevalúa periódicamente para cambiar a medianoche. */
    private val day: Flow<Pair<Long, Long>> = flow {
        while (true) {
            emit(DayRange.today())
            delay(30_000)
        }
    }.distinctUntilChanged()

    val state: StateFlow<HomeUiState> = repo.currentBaby.flatMapLatest { baby ->
        if (baby == null) return@flatMapLatest flowOf(HomeUiState())
        val today = day.flatMapLatest { (start, end) ->
            repo.overlapping(baby.id, start, end).map { Triple(start, end, it) }
        }
        val core = combine(repo.active(baby.id), today, repo.recent(baby.id, 8)) { active, (start, end, events), recent ->
            HomeUiState(
                baby = baby,
                active = active,
                today = events,
                dayStart = start,
                dayEnd = end,
                recent = recent,
            )
        }
        val latest = combine(
            repo.latest(baby.id, EventType.BREAST, EventType.BOTTLE),
            repo.latest(baby.id, EventType.DIAPER),
            repo.latest(baby.id, EventType.SLEEP),
            repo.latest(baby.id, EventType.BREAST),
            repo.latest(baby.id, EventType.BOTTLE),
            ::Latest,
        )
        combine(core, latest) { c, l ->
            c.copy(
                lastFeed = l.feed,
                lastDiaper = l.diaper,
                lastSleep = l.sleep,
                lastBreast = l.breast,
                lastBottle = l.bottle,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val babyId: Long? get() = state.value.baby?.id

    private fun now() = System.currentTimeMillis()

    fun startBreast(side: BreastSide) = withBaby { id ->
        if (state.value.active.none { it.type == EventType.BREAST }) {
            repo.insert(BreastTimer.start(id, side, now()))
        }
    }

    fun tapBreastSide(eventId: Long, side: BreastSide) = viewModelScope.launch {
        repo.getEvent(eventId)?.let { repo.update(BreastTimer.tapSide(it, side, now())) }
    }

    fun startSleep() = withBaby { id ->
        if (state.value.active.none { it.type == EventType.SLEEP }) {
            repo.insert(EventEntity(babyId = id, type = EventType.SLEEP, startTime = now()))
        }
    }

    fun stopTimer(eventId: Long) = viewModelScope.launch {
        val event = repo.getEvent(eventId) ?: return@launch
        val now = now()
        repo.update(if (event.type == EventType.BREAST) BreastTimer.stop(event, now) else event.copy(endTime = now))
    }

    fun delete(eventId: Long) = viewModelScope.launch {
        repo.getEvent(eventId)?.let {
            repo.delete(it)
            reminders.cancelEvent(it.id)
        }
    }

    fun updateBaby(baby: BabyEntity) = viewModelScope.launch { repo.updateBaby(baby) }

    private fun withBaby(block: suspend (Long) -> Unit) {
        val id = babyId ?: return
        viewModelScope.launch { block(id) }
    }
}
