package com.kbretro.babyregistry.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kbretro.babyregistry.data.model.BabyEntity
import com.kbretro.babyregistry.ui.common.BabyAvatar
import com.kbretro.babyregistry.ui.common.Fmt

/** Lista de bebés para cambiar entre ellos, editarlos o añadir uno nuevo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BabySwitcherSheet(
    babies: List<BabyEntity>,
    selectedId: Long?,
    onSelect: (BabyEntity) -> Unit,
    onEdit: (BabyEntity) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                "Bebés",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            babies.forEach { baby ->
                ListItem(
                    modifier = Modifier.clickable { onSelect(baby) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { BabyAvatar(baby.name, baby.photoPath, 44.dp) },
                    headlineContent = { Text(baby.name) },
                    supportingContent = { Text(Fmt.age(baby.birthDate)) },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (baby.id == selectedId) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = "Seleccionado", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onEdit(baby) }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Editar")
                            }
                        }
                    },
                )
            }
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAdd),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = { Icon(Icons.Rounded.Add, contentDescription = null) },
                headlineContent = { Text("Añadir bebé") },
            )
        }
    }
}
