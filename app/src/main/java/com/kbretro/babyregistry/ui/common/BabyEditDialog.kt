package com.kbretro.babyregistry.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.kbretro.babyregistry.data.model.BabyEntity
import com.kbretro.babyregistry.data.model.Sex

@Composable
fun BabyEditDialog(
    baby: BabyEntity,
    onDismiss: () -> Unit,
    onSave: (BabyEntity) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(baby.name) }
    var confirmDelete by remember { mutableStateOf(false) }
    var birthDate by remember { mutableStateOf(baby.birthDate) }
    var sex by remember { mutableStateOf(baby.sex) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (baby.id == 0L) "Nuevo bebé" else "Perfil del bebé") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                if (onDelete != null) {
                    TextButton(onClick = { if (confirmDelete) onDelete() else confirmDelete = true }) {
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
                onClick = { onSave(baby.copy(name = name.trim(), birthDate = birthDate, sex = sex)) },
                enabled = name.isNotBlank(),
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
