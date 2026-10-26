package com.kbretro.babyregistry.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.kbretro.babyregistry.data.model.BabyEntity
import com.kbretro.babyregistry.data.model.EventType
import com.kbretro.babyregistry.data.model.FeedingType
import com.kbretro.babyregistry.data.model.Sex
import com.kbretro.babyregistry.data.photos.PhotoFiles

val FeedingType.label: String
    get() = when (this) {
        FeedingType.BREAST -> "Materna"
        FeedingType.FORMULA -> "Artificial"
        FeedingType.MIXED -> "Mixta"
    }

/** Al cambiar el tipo de lactancia se quitan las ocultaciones automáticas del anterior y se aplican las del nuevo. */
fun applyFeedingType(hidden: Set<EventType>, previous: FeedingType?, next: FeedingType?): Set<EventType> =
    hidden - previous?.autoHidden.orEmpty() + next?.autoHidden.orEmpty()

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BabyEditDialog(
    baby: BabyEntity,
    onDismiss: () -> Unit,
    onSave: (BabyEntity) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(baby.name) }
    var birthDate by remember { mutableStateOf(baby.birthDate) }
    var sex by remember { mutableStateOf(baby.sex) }
    var feeding by remember { mutableStateOf(baby.feedingType) }
    var hidden by remember { mutableStateOf(baby.hidden) }
    var photo by remember { mutableStateOf(baby.photoPath) }
    var confirmDelete by remember { mutableStateOf(false) }
    val context = LocalContext.current
    // Las fotos elegidas y no guardadas se borran para no dejar archivos huérfanos.
    fun discardUnsavedPhoto() {
        if (photo != baby.photoPath) PhotoFiles.delete(context, photo)
    }
    val cancel = {
        discardUnsavedPhoto()
        onDismiss()
    }
    AlertDialog(
        onDismissRequest = cancel,
        title = { Text(if (baby.id == 0L) "Nuevo bebé" else "Perfil del bebé") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    PhotoPickerAvatar(name.ifBlank { "?" }, photo, 96.dp) { picked ->
                        discardUnsavedPhoto()
                        photo = picked
                    }
                    if (photo != null) {
                        TextButton(onClick = {
                            discardUnsavedPhoto()
                            photo = null
                        }) { Text("Quitar foto") }
                    } else {
                        Text("Toca para añadir una foto", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                DateField("Nacimiento", birthDate, { birthDate = it })
                Text("Sexo", style = MaterialTheme.typography.labelLarge)
                ChoiceRow(Sex.entries.map { it to it.label }, sex, { sex = it })

                Text("Lactancia", style = MaterialTheme.typography.labelLarge)
                ChoiceRow(
                    FeedingType.entries.map { it to it.label },
                    feeding,
                    { next ->
                        // Tocar la opción ya elegida la desmarca.
                        val value = if (next == feeding) null else next
                        hidden = applyFeedingType(hidden, feeding, value)
                        feeding = value
                    },
                )
                Text(
                    when (feeding) {
                        FeedingType.BREAST -> "Se oculta el biberón. Puedes volver a mostrarlo abajo."
                        FeedingType.FORMULA -> "Se oculta la toma de pecho. Puedes volver a mostrarla abajo."
                        else -> "Al elegir el tipo se ocultan los registros que no uses."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text("Registros visibles", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventType.entries.forEach { type ->
                        val style = type.style()
                        FilterChip(
                            selected = type !in hidden,
                            onClick = { hidden = if (type in hidden) hidden - type else hidden + type },
                            label = { Text(style.label) },
                            leadingIcon = {
                                Icon(style.icon, contentDescription = null, tint = style.color, modifier = Modifier.size(18.dp))
                            },
                        )
                    }
                }
                Text(
                    "Los registros ocultos no aparecen en los accesos rápidos. Lo ya guardado no se borra.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (onDelete != null) {
                    TextButton(onClick = {
                        if (confirmDelete) {
                            discardUnsavedPhoto()
                            onDelete()
                        } else {
                            confirmDelete = true
                        }
                    }) {
                        Text(
                            if (confirmDelete) "Se borrarán todos sus registros. ¿Seguro?" else "Eliminar bebé",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (photo != baby.photoPath) PhotoFiles.delete(context, baby.photoPath)
                    onSave(
                        baby.copy(name = name.trim(), birthDate = birthDate, sex = sex, feedingType = feeding, photoPath = photo)
                            .withHidden(hidden),
                    )
                },
                enabled = name.isNotBlank(),
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = cancel) { Text("Cancelar") } },
    )
}
