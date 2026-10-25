package com.kbretro.babyregistry.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BabyChangingStation
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.kbretro.babyregistry.data.model.AppointmentKind
import com.kbretro.babyregistry.data.model.BottleContent
import com.kbretro.babyregistry.data.model.BreastSide
import com.kbretro.babyregistry.data.model.BreastTimer
import com.kbretro.babyregistry.data.model.DiaperKind
import com.kbretro.babyregistry.data.model.EventEntity
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.data.model.Sex
import com.kbretro.babyregistry.data.model.StoolColor

data class EventStyle(val label: String, val icon: ImageVector, val color: Color)

fun EventType.style(): EventStyle = when (this) {
    EventType.BREAST -> EventStyle("Pecho", Icons.Rounded.Favorite, Color(0xFFE5739D))
    EventType.BOTTLE -> EventStyle("Biberón", Icons.Rounded.LocalDrink, Color(0xFF4F8EDC))
    EventType.SOLID -> EventStyle("Sólidos", Icons.Rounded.Restaurant, Color(0xFFF0954A))
    EventType.SLEEP -> EventStyle("Sueño", Icons.Rounded.Bedtime, Color(0xFF7E6BD6))
    EventType.DIAPER -> EventStyle("Pañal", Icons.Rounded.BabyChangingStation, Color(0xFF34A88A))
    EventType.TEMPERATURE -> EventStyle("Temperatura", Icons.Rounded.Thermostat, Color(0xFFE0603A))
    EventType.MEDICINE -> EventStyle("Medicamento", Icons.Rounded.Medication, Color(0xFF2E9BB0))
}

/** Los temporizadores en curso no se editan: primero hay que terminarlos. */
val EventEntity.isEditable: Boolean get() = !isOngoing

val AppointmentKind.label: String
    get() = when (this) {
        AppointmentKind.DOCTOR -> "Pediatra"
        AppointmentKind.VACCINE -> "Vacuna"
        AppointmentKind.OTHER -> "Otra cita"
    }

enum class FeverLevel(val label: String) { NORMAL("Normal"), LOW_GRADE("Febrícula"), FEVER("Fiebre"), LOW("Baja") }

fun feverLevel(celsius: Double): FeverLevel = when {
    celsius >= 38.0 -> FeverLevel.FEVER
    celsius >= 37.5 -> FeverLevel.LOW_GRADE
    celsius < 35.5 -> FeverLevel.LOW
    else -> FeverLevel.NORMAL
}

val BreastSide.label: String get() = if (this == BreastSide.LEFT) "Izquierdo" else "Derecho"

val BottleContent.label: String
    get() = when (this) {
        BottleContent.FORMULA -> "Fórmula"
        BottleContent.BREAST_MILK -> "Leche materna"
    }

val DiaperKind.label: String
    get() = when (this) {
        DiaperKind.WET -> "Pis"
        DiaperKind.DIRTY -> "Caca"
        DiaperKind.MIXED -> "Pis y caca"
    }

val Sex.label: String
    get() = when (this) {
        Sex.BOY -> "Niño"
        Sex.GIRL -> "Niña"
        Sex.UNSPECIFIED -> "Sin indicar"
    }

val StoolColor.label: String
    get() = when (this) {
        StoolColor.YELLOW -> "Amarillo"
        StoolColor.MUSTARD -> "Mostaza"
        StoolColor.BROWN -> "Marrón"
        StoolColor.GREEN -> "Verde"
        StoolColor.BLACK -> "Negro"
        StoolColor.RED -> "Rojo"
        StoolColor.WHITE -> "Blanco"
    }

val StoolColor.swatch: Color
    get() = when (this) {
        StoolColor.YELLOW -> Color(0xFFF2D24B)
        StoolColor.MUSTARD -> Color(0xFFC9A227)
        StoolColor.BROWN -> Color(0xFF8B5A2B)
        StoolColor.GREEN -> Color(0xFF6B8E23)
        StoolColor.BLACK -> Color(0xFF222222)
        StoolColor.RED -> Color(0xFFC62828)
        StoolColor.WHITE -> Color(0xFFF1EFE8)
    }

private fun isNight(millis: Long): Boolean = Fmt.dateTime(millis).hour.let { it >= 19 || it < 7 }

fun EventEntity.title(): String = when (type) {
    EventType.BREAST -> "Toma de pecho"
    EventType.SLEEP -> if (isNight(startTime)) "Sueño nocturno" else "Siesta"
    else -> type.style().label
}

fun EventEntity.detail(now: Long): String = when (type) {
    EventType.BREAST -> {
        val left = BreastTimer.sideMillis(this, BreastSide.LEFT, now)
        val right = BreastTimer.sideMillis(this, BreastSide.RIGHT, now)
        buildList {
            if (left > 0) add("Izq ${Fmt.duration(left)}")
            if (right > 0) add("Der ${Fmt.duration(right)}")
            if (isOngoing) add("En curso")
        }.joinToString(" · ").ifEmpty { "Sin duración" }
    }
    EventType.BOTTLE -> listOfNotNull(amountMl?.let { "$it ml" }, bottleContent?.label).joinToString(" · ")
    EventType.SOLID -> listOfNotNull(
        food,
        "Primera vez".takeIf { isNewFood },
        reaction?.takeIf { it.isNotBlank() },
    ).joinToString(" · ")
    EventType.SLEEP -> endTime?.let { Fmt.duration(it - startTime) }
        ?: "En curso · ${Fmt.duration(now - startTime)}"
    EventType.DIAPER -> listOfNotNull(diaperKind?.label, stoolColor?.label).joinToString(" · ")
    EventType.TEMPERATURE -> temperatureC?.let { "${Fmt.temperature(it)} · ${feverLevel(it).label}" } ?: ""
    EventType.MEDICINE -> listOfNotNull(
        medicineName,
        dose?.takeIf { it.isNotBlank() },
        reminderAt?.takeIf { it > now }?.let { "Próxima: ${Fmt.dayTime(it)}" },
    ).joinToString(" · ")
}
