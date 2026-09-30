package com.eduapp.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.util.Locale

/** Copia la fotografía al almacenamiento interno (media/) para no depender de la ruta original (7.2). */
object ImageStore {
    private val EXTENSIONES = setOf("jpg", "jpeg", "png", "webp")

    fun importar(ctx: Context, uri: Uri, anteriorRel: String?): String {
        val cr = ctx.contentResolver
        val nombre = cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        val mime = cr.getType(uri)
        val ext = nombre?.substringAfterLast('.', "")?.lowercase(Locale.ROOT).orEmpty()
        val extMime = when (mime) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> null
        }
        val error = ValidationException("Formato de imagen no permitido. Usa .jpg, .jpeg, .png o .webp.")
        if (mime != null && mime.startsWith("image/") && extMime == null) throw error
        val extFinal: String = (when {
            ext in EXTENSIONES -> ext
            ext.isEmpty() && extMime != null -> extMime
            else -> null
        }) ?: throw error

        val dir = File(ctx.filesDir, "media").apply { mkdirs() }
        val destino = File(dir, "perfil_${System.currentTimeMillis()}.$extFinal")
        val entrada = cr.openInputStream(uri) ?: throw ValidationException("No se pudo leer la imagen seleccionada.")
        entrada.use { input -> destino.outputStream().use { input.copyTo(it) } }

        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(destino.path, opts)
        if (opts.outWidth <= 0) {
            destino.delete()
            throw ValidationException("El archivo seleccionado no es una imagen válida.")
        }
        anteriorRel?.let { File(ctx.filesDir, it).delete() }
        return "media/${destino.name}"
    }

    fun cargar(ctx: Context, rutaRelativa: String, maxLado: Int): Bitmap? {
        val f = File(ctx.filesDir, rutaRelativa)
        if (!f.exists()) return null
        val b = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, b)
        var s = 1
        while (b.outWidth / s > maxLado * 2 || b.outHeight / s > maxLado * 2) s *= 2
        return BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = s })
    }
}
