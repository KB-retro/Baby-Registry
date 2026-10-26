package com.kbretro.babyregistry.ui.growth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kbretro.babyregistry.data.model.BabyEntity
import com.kbretro.babyregistry.data.model.GrowthEntity
import com.kbretro.babyregistry.data.model.MilestoneEntity
import com.kbretro.babyregistry.data.photos.PhotoFiles
import com.kbretro.babyregistry.data.repository.TrackerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GrowthUiState(
    val baby: BabyEntity? = null,
    /** Medidas de la más reciente a la más antigua. */
    val entries: List<GrowthEntity> = emptyList(),
    /** Hitos del más reciente al más antiguo. */
    val milestones: List<MilestoneEntity> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GrowthViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: TrackerRepository,
) : ViewModel() {

    val state: StateFlow<GrowthUiState> = repo.currentBaby
        .flatMapLatest { baby ->
            if (baby == null) {
                flowOf(GrowthUiState())
            } else {
                combine(repo.growth(baby.id), repo.milestones(baby.id)) { growth, milestones ->
                    GrowthUiState(baby, growth, milestones)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GrowthUiState())

    fun save(entry: GrowthEntity) = viewModelScope.launch { repo.saveGrowth(entry) }

    fun delete(entry: GrowthEntity) = viewModelScope.launch { repo.deleteGrowth(entry) }

    fun saveMilestone(milestone: MilestoneEntity) = viewModelScope.launch { repo.saveMilestone(milestone) }

    fun deleteMilestone(milestone: MilestoneEntity) = viewModelScope.launch {
        repo.deleteMilestone(milestone)
        PhotoFiles.delete(context, milestone.photoPath)
    }
}
