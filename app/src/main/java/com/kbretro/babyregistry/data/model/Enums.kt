package com.kbretro.babyregistry.data.model

enum class Sex { BOY, GIRL, UNSPECIFIED }

enum class EventType {
    BREAST, BOTTLE, SOLID, SLEEP, DIAPER, TEMPERATURE, MEDICINE;

    companion object {
        /** Tipos del día a día que aparecen como accesos rápidos en Inicio. */
        val daily = listOf(BREAST, BOTTLE, SOLID, SLEEP, DIAPER)
        val health = listOf(TEMPERATURE, MEDICINE)
    }
}

enum class BreastSide {
    LEFT, RIGHT;

    val opposite: BreastSide get() = if (this == LEFT) RIGHT else LEFT
}

enum class BottleContent { FORMULA, BREAST_MILK }

enum class DiaperKind { WET, DIRTY, MIXED }

enum class StoolColor(val isWarning: Boolean = false) {
    YELLOW, MUSTARD, BROWN, GREEN, BLACK(isWarning = true), RED(isWarning = true), WHITE(isWarning = true)
}

enum class AppointmentKind { DOCTOR, VACCINE, OTHER }
