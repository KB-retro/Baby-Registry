package com.kbretro.babyregistry.ui.growth

import com.kbretro.babyregistry.data.model.MilestoneEntity
import com.kbretro.babyregistry.data.model.MilestoneKind

/** Cómo se muestra y se rellena cada tipo de hito. */
data class MilestoneStyle(
    val label: String,
    val emoji: String,
    /** Si no es null, el formulario muestra una casilla con esta etiqueta. */
    val detailLabel: String? = null,
    val detailRequired: Boolean = false,
    val suggestions: List<String> = emptyList(),
)

val MilestoneKind.style: MilestoneStyle
    get() = when (this) {
        MilestoneKind.UMBILICAL_CORD -> MilestoneStyle("Caída del cordón umbilical", "🎀")
        MilestoneKind.FIRST_SMILE -> MilestoneStyle("Primera sonrisa", "😊")
        MilestoneKind.FIRST_LAUGH -> MilestoneStyle("Primera carcajada", "😂")
        MilestoneKind.HOLDS_HEAD -> MilestoneStyle("Sostiene la cabeza", "💪")
        MilestoneKind.ROLLS_OVER -> MilestoneStyle("Se da la vuelta", "🔄")
        MilestoneKind.SITS -> MilestoneStyle("Se sienta solo", "🪑")
        MilestoneKind.CRAWLS -> MilestoneStyle("Gatea", "🐛")
        MilestoneKind.FIRST_TOOTH -> MilestoneStyle(
            "Primer diente",
            "🦷",
            detailLabel = "¿Cuál? (opcional)",
            suggestions = listOf("Incisivo inferior", "Incisivo superior", "Colmillo", "Muela"),
        )
        MilestoneKind.FIRST_WORD -> MilestoneStyle(
            "Primera palabra",
            "🗣️",
            detailLabel = "¿Qué palabra?",
            detailRequired = true,
            suggestions = listOf("Mamá", "Papá", "Caca", "Agua", "Teta", "Tata", "Abu", "No"),
        )
        MilestoneKind.FIRST_STEPS -> MilestoneStyle("Primeros pasos", "👣")
        MilestoneKind.FIRST_FOOD -> MilestoneStyle(
            "Primera comida sólida",
            "🥣",
            detailLabel = "¿Qué comió? (opcional)",
            suggestions = listOf("Fruta", "Verdura", "Cereales", "Pollo"),
        )
        MilestoneKind.SURGERY -> MilestoneStyle(
            "Operación",
            "🏥",
            detailLabel = "¿Qué operación?",
            detailRequired = true,
            suggestions = listOf("Frenectomía", "Sindactilia", "Hernia inguinal", "Fimosis", "Drenaje transtimpánico"),
        )
        MilestoneKind.OTHER -> MilestoneStyle(
            "Otro hito",
            "⭐",
            detailLabel = "¿Qué ha pasado?",
            detailRequired = true,
        )
    }

/** "Primera palabra: Mamá", "Operación: Frenectomía" u "Otro hito" → su descripción. */
fun MilestoneEntity.title(): String {
    val detail = detail?.takeIf { it.isNotBlank() } ?: return kind.style.label
    return if (kind == MilestoneKind.OTHER) detail else "${kind.style.label}: $detail"
}
