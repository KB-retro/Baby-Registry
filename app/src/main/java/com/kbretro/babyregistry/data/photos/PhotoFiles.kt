package com.kbretro.babyregistry.data.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Fotos de la app (bebés e hitos). Se copian reducidas a la carpeta privada de la app y en la
 * base de datos solo se guarda el nombre del archivo.
 */
object PhotoFiles {
    private const val MAX_SIDE = 1280
    private const val QUALITY = 85

    // Caché en memoria (~1/8 de la memoria de la app) para no decodificar al hacer scroll.
    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 8 / 1024).toInt()) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
    }

    private fun dir(context: Context) = File(context.filesDir, "photos").apply { mkdirs() }

    fun file(context: Context, name: String) = File(dir(context), name)

    /** Copia la imagen elegida, reducida y con la orientación correcta. Devuelve el nombre del archivo. */
    suspend fun save(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val bitmap = decodeScaled(context, uri)
        write(context, bitmap)
    }

    /** Guarda una foto que viene de una copia de seguridad (JPEG ya reducido). */
    suspend fun saveBytes(context: Context, bytes: ByteArray): String = withContext(Dispatchers.IO) {
        val name = "${UUID.randomUUID()}.jpg"
        file(context, name).writeBytes(bytes)
        name
    }

    fun readBytes(context: Context, name: String): ByteArray? =
        file(context, name).takeIf { it.exists() }?.readBytes()

    fun delete(context: Context, name: String?) {
        if (name.isNullOrBlank()) return
        cache.remove(name)
        file(context, name).delete()
    }

    /** Carga la foto (desde caché si ya se cargó antes). */
    suspend fun load(context: Context, name: String): Bitmap? {
        cache.get(name)?.let { return it }
        return withContext(Dispatchers.IO) {
            val f = file(context, name)
            if (!f.exists()) return@withContext null
            BitmapFactory.decodeFile(f.absolutePath)?.also { cache.put(name, it) }
        }
    }

    private fun write(context: Context, bitmap: Bitmap): String {
        val name = "${UUID.randomUUID()}.jpg"
        file(context, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
        cache.put(name, bitmap)
        return name
    }

    private fun decodeScaled(context: Context, uri: Uri): Bitmap {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder aplica la rotación EXIF automáticamente.
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val (w, h) = info.size.width to info.size.height
                val scale = MAX_SIDE.toFloat() / max(w, h)
                if (scale < 1f) decoder.setTargetSize((w * scale).roundToInt(), (h * scale).roundToInt())
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("No se pudo leer la imagen")
    }
}
