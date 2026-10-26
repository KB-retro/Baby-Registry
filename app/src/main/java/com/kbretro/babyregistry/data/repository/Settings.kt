package com.kbretro.babyregistry.data.repository

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/** Ajustes de la app (bebé seleccionado, copia automática…). */
internal val Context.settings by preferencesDataStore(name = "settings")
