package com.nave.moto.data.backup

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Fotos y manuales viven en filesDir; la BD solo guarda rutas relativas. */
@Singleton
class FileStorage @Inject constructor(@ApplicationContext private val context: Context) {
    val root: File get() = context.filesDir

    fun file(rel: String): File = File(root, rel)

    fun savePhotoBytes(bytes: ByteArray, ext: String = "webp"): String {
        val rel = "photos/${UUID.randomUUID()}.$ext"
        file(rel).apply { parentFile?.mkdirs() }.writeBytes(bytes)
        return rel
    }

    /** Copia y reduce (máx. 1024 px, JPEG 85) una imagen elegida por el usuario. */
    fun savePhotoFromUri(uri: Uri): String? = runCatching {
        val src = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(src, 0, src.size, opts)
        var sample = 1
        while (opts.outWidth / sample > 2048) sample *= 2
        val bmp = BitmapFactory.decodeByteArray(src, 0, src.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null
        val scale = 1024f / maxOf(bmp.width, bmp.height)
        val out = if (scale < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true) else bmp
        val rel = "photos/${UUID.randomUUID()}.jpg"
        val f = file(rel).apply { parentFile?.mkdirs() }
        f.outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        rel
    }.getOrNull()

    /** Retrato: recorte cuadrado centrado, 512 px, JPEG 88. */
    fun savePortraitFromUri(uri: Uri): String? = runCatching {
        val src = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val bmp = BitmapFactory.decodeByteArray(src, 0, src.size, BitmapFactory.Options().apply { inSampleSize = 2 }.takeIf { src.size > 4_000_000 }) ?: return null
        val side = minOf(bmp.width, bmp.height)
        val sq = Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
        val out = if (side > 512) Bitmap.createScaledBitmap(sq, 512, 512, true) else sq
        val rel = "photos/${UUID.randomUUID()}.jpg"
        file(rel).apply { parentFile?.mkdirs() }.outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        rel
    }.getOrNull()

    /** Primeros [max] caracteres de un manual de texto (vista previa). */
    fun previewText(rel: String, max: Int = 1500): String? =
        runCatching { file(rel).bufferedReader().use { r -> CharArray(max).let { b -> String(b, 0, maxOf(0, r.read(b))) } } }.getOrNull()

    data class SavedManual(val rel: String, val displayName: String, val size: Long, val mime: String)

    fun saveManual(uri: Uri): SavedManual? = runCatching {
        var name = "manual"
        var size = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = c.getString(it) }
                c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { size = c.getLong(it) }
            }
        }
        val ext = name.substringAfterLast('.', "bin")
        val rel = "manuals/${UUID.randomUUID()}.$ext"
        val f = file(rel).apply { parentFile?.mkdirs() }
        context.contentResolver.openInputStream(uri)?.use { i -> f.outputStream().use { i.copyTo(it) } } ?: return null
        SavedManual(rel, name, f.length(), context.contentResolver.getType(uri) ?: "application/octet-stream")
    }.getOrNull()

    fun delete(rel: String?) { if (rel != null) runCatching { file(rel).delete() } }
}
