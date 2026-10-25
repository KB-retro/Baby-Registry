package com.kbretro.babyregistry.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settings by preferencesDataStore(name = "settings")

/** Recuerda qué bebé está seleccionado cuando hay varios. */
@Singleton
class SelectedBabyStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val key = longPreferencesKey("selected_baby_id")

    val selectedId: Flow<Long?> = context.settings.data.map { it[key] }

    suspend fun select(id: Long) {
        context.settings.edit { it[key] = id }
    }
}
