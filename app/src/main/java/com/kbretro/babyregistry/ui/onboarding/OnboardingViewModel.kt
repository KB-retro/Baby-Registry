package com.kbretro.babyregistry.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kbretro.babyregistry.data.model.BabyEntity
import com.kbretro.babyregistry.data.model.Sex
import com.kbretro.babyregistry.data.repository.TrackerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repo: TrackerRepository,
) : ViewModel() {

    fun save(name: String, birthDate: LocalDate, sex: Sex) = viewModelScope.launch {
        repo.addBaby(BabyEntity(name = name.trim(), birthDate = birthDate, sex = sex))
    }
}
