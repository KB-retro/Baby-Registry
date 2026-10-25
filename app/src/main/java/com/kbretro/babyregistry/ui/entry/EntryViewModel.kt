package com.kbretro.babyregistry.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.repository.TrackerRepository
import com.kbretro.babyregistry.reminders.Reminders
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Guarda registros nuevos o editados desde las hojas de registro. */
@HiltViewModel
class EntryViewModel @Inject constructor(
    private val repo: TrackerRepository,
    private val reminders: Reminders,
) : ViewModel() {

    fun save(initial: EventEntity?, build: (babyId: Long) -> EventEntity) = viewModelScope.launch {
        val babyId = initial?.babyId ?: repo.currentBabyId() ?: return@launch
        val event = build(babyId)
        if (initial == null) {
            val id = repo.insert(event)
            reminders.sync(event.copy(id = id))
        } else {
            val updated = event.copy(id = initial.id)
            repo.update(updated)
            reminders.sync(updated)
        }
    }
}
