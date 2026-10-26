package com.kbretro.babyregistry.data.transfer

import com.kbretro.babyregistry.data.model.EventEntity

/**
 * Huella para detectar duplicados al importar: mismo tipo, misma hora y mismo contenido.
 * Dos medicamentos distintos a la misma hora no son duplicados.
 */
fun EventEntity.importKey(): List<Any?> =
    listOf(type, startTime, amountMl, diaperKind, medicineName, dose, food, temperatureC)
