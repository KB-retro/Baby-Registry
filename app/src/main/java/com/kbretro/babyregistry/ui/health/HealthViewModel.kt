package com.kbretro.babyregistry.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kbretro.babyregistry.data.model.AppointmentEntity
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.data.repository.TrackerRepository
import com.kbretro.babyregistry.reminders.Reminders
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HealthUiState(
    val babyId: Long? = null,
    val hidden: Set<EventType> = emptySet(),
    val temperatures: List<EventEntity> = emptyList(),
    val medicines: List<EventEntity> = emptyList(),
    val appointments: List<AppointmentEntity> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HealthViewModel @Inject constructor(
    private val repo: TrackerRepository,
    private val reminders: Reminders,
) : ViewModel() {

    val state: StateFlow<HealthUiState> = repo.currentBaby
        .flatMapLatest { baby ->
            if (baby == null) return@flatMapLatest flowOf(HealthUiState())
            combine(
                repo.byTypes(baby.id, listOf(EventType.TEMPERATURE)),
                repo.byTypes(baby.id, listOf(EventType.MEDICINE)),
                repo.appointments(baby.id),
            ) { temps, meds, appointments -> HealthUiState(baby.id, baby.hidden, temps, meds, appointments) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HealthUiState())

    fun saveAppointment(appointment: AppointmentEntity) = viewModelScope.launch {
        val saved = if (appointment.id == 0L) {
            appointment.copy(id = repo.insertAppointment(appointment))
        } else {
            repo.updateAppointment(appointment)
            appointment
        }
        reminders.sync(saved)
    }

    fun setDone(appointment: AppointmentEntity, done: Boolean) = saveAppointment(appointment.copy(done = done))

    fun deleteAppointment(appointment: AppointmentEntity) = viewModelScope.launch {
        repo.deleteAppointment(appointment)
        reminders.cancelAppointment(appointment.id)
    }

    fun deleteEvent(event: EventEntity) = viewModelScope.launch {
        repo.delete(event)
        reminders.cancelEvent(event.id)
    }
}
