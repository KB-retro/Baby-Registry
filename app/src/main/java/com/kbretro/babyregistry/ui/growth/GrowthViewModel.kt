package com.kbretro.babyregistry.ui.growth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kbretro.babyregistry.data.model.BabyEntity
import com.kbretro.babyregistry.data.model.GrowthEntity
import com.kbretro.babyregistry.data.repository.TrackerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GrowthUiState(
    val baby: BabyEntity? = null,
    /** Medidas de la más reciente a la más antigua. */
    val entries: List<GrowthEntity> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GrowthViewModel @Inject constructor(
    private val repo: TrackerRepository,
) : ViewModel() {

    val state: StateFlow<GrowthUiState> = repo.currentBaby
        .flatMapLatest { baby ->
            if (baby == null) flowOf(GrowthUiState()) else repo.growth(baby.id).map { GrowthUiState(baby, it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GrowthUiState())

    fun save(entry: GrowthEntity) = viewModelScope.launch { repo.saveGrowth(entry) }

    fun delete(entry: GrowthEntity) = viewModelScope.launch { repo.deleteGrowth(entry) }
}
