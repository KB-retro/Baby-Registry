package com.kbretro.babyregistry.ui.common

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kbretro.babyregistry.data.photos.PhotoFiles
import kotlinx.coroutines.launch

/** Carga una foto guardada; null mientras carga o si no existe. */
@Composable
fun rememberPhoto(name: String?): ImageBitmap? {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, name) {
        value = name?.let { PhotoFiles.load(context, it)?.asImageBitmap() }
    }
    return bitmap
}

@Composable
fun PhotoImage(name: String?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val bitmap = rememberPhoto(name)
    if (bitmap != null) {
        Image(bitmap, contentDescription = null, contentScale = contentScale, modifier = modifier)
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

/** Avatar del bebé: su foto o, si no tiene, la inicial. */
@Composable
fun BabyAvatar(
    name: String,
    photo: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val bitmap = rememberPhoto(photo)
    val content: @Composable () -> Unit = {
        Box(contentAlignment = Alignment.Center) {
            if (bitmap != null) {
                Image(bitmap, contentDescription = name, contentScale = ContentScale.Crop, modifier = Modifier.size(size).clip(CircleShape))
            } else {
                Text(
                    name.firstOrNull()?.uppercase() ?: "?",
                    fontSize = (size.value * 0.42f).sp,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
    val border = Modifier.border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
    if (onClick != null) {
        Surface(onClick = onClick, shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, shadowElevation = 2.dp, modifier = modifier.size(size).then(border)) { content() }
    } else {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = modifier.size(size).then(border)) { content() }
    }
}

/** Avatar editable: al tocarlo se abre el selector de fotos. */
@Composable
fun PhotoPickerAvatar(name: String, photo: String?, size: Dp, onPicked: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { runCatching { PhotoFiles.save(context, uri) }.onSuccess(onPicked) }
    }
    Box {
        BabyAvatar(name, photo, size, onClick = {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        })
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(size * 0.34f)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.PhotoCamera, contentDescription = "Cambiar foto", tint = Color.White, modifier = Modifier.size(size * 0.2f))
        }
    }
}

/** Devuelve una función que abre el selector de fotos y entrega el nombre de la foto guardada. */
@Composable
fun rememberPhotoPicker(onPicked: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { runCatching { PhotoFiles.save(context, uri) }.onSuccess(onPicked) }
    }
    return { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}
