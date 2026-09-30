package com.eduapp.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Error de validación con mensaje pensado para mostrarse al usuario. */
class ValidationException(message: String) : Exception(message)

/** Error de base de datos: se hizo ROLLBACK y no quedó nada guardado (sección 38). */
class OperationException(message: String, cause: Throwable? = null) : Exception(message, cause)

class Repo(context: Context) {
    private val helper = EduDb(context.applicationContext)
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    private val cien = BigDecimal(100)

    // ---------- utilidades ----------

    /** Validar → Ejecutar → COMMIT; ante cualquier error, ROLLBACK (sección 50). */
    private fun <T> tx(bloque: (SQLiteDatabase) -> T): T {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val r = bloque(db)
            db.setTransactionSuccessful()
            return r
        } catch (e: ValidationException) {
            throw e
        } catch (e: Exception) {
            throw OperationException("La operación no pudo completarse. No se guardó ningún cambio.", e)
        } finally {
            db.endTransaction()
        }
    }

    private fun r2(v: Double): Double = BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).toDouble()

    private fun ContentValues.putTexto(clave: String, valor: String?) {
        if (valor.isNullOrEmpty()) putNull(clave) else put(clave, valor)
    }

    private fun pasada(fecha: LocalDateTime, ahora: LocalDateTime) =
        fecha.isBefore(ahora.truncatedTo(ChronoUnit.MINUTES))

    /** RI-11 y RI-12: completada nunca pasa a vencida; no completada con fecha pasada es vencida. */
    private fun normalizarEstado(estado: Estado, fecha: LocalDateTime, ahora: LocalDateTime): Estado = when {
        estado == Estado.COMPLETADA -> Estado.COMPLETADA
        pasada(fecha, ahora) -> Estado.VENCIDA
        estado == Estado.VENCIDA -> Estado.PENDIENTE
        else -> estado
    }

    // ---------- perfil ----------

    fun perfil(): Perfil = helper.readableDatabase.rawQuery(
        "SELECT alias, fotografia, duracion_estudio, duracion_descanso, " +
            "recordatorios_activados, minutos_antelacion FROM perfil WHERE id = 1", null
    ).use { c ->
        c.moveToFirst()
        Perfil(
            alias = c.getString(0),
            fotografia = if (c.isNull(1)) null else c.getString(1),
            duracionEstudio = c.getInt(2),
            duracionDescanso = c.getInt(3),
            recordatoriosActivados = c.getInt(4) == 1,
            minutosAntelacion = c.getInt(5)
        )
    }

    fun guardarPerfil(alias: String, recordatorios: Boolean, minutos: Int?) {
        val a = alias.trim()
        if (a.isEmpty()) throw ValidationException("El alias es obligatorio.")
        if (a.length > 50) throw ValidationException("El alias no puede superar 50 caracteres.")
        if (minutos == null || minutos < 1 || minutos > 1440)
            throw ValidationException("La antelación debe ser un número de minutos entre 1 y 1440.")
        tx { db ->
            val cv = ContentValues().apply {
                put("alias", a)
                put("recordatorios_activados", if (recordatorios) 1 else 0)
                put("minutos_antelacion", minutos)
            }
            db.update("perfil", cv, "id = 1", null)
        }
    }

    fun guardarFoto(rutaRelativa: String?) {
        tx { db ->
            val cv = ContentValues().apply { putTexto("fotografia", rutaRelativa) }
            db.update("perfil", cv, "id = 1", null)
        }
    }

    fun guardarPomodoro(estudio: Int?, descanso: Int?) {
        if (estudio == null || estudio < 1 || estudio > 300)
            throw ValidationException("El tiempo de estudio debe estar entre 1 y 300 minutos.")
        if (descanso == null || descanso < 5)
            throw ValidationException("El descanso mínimo permitido es de 5 minutos.")
        if (descanso > 120)
            throw ValidationException("El descanso no puede superar 120 minutos.")
        tx { db ->
            val cv = ContentValues().apply {
                put("duracion_estudio", estudio)
                put("duracion_descanso", descanso)
            }
            db.update("perfil", cv, "id = 1", null)
        }
    }

    // ---------- materias ----------

    fun materias(): List<Materia> {
        val out = mutableListOf<Materia>()
        helper.readableDatabase.rawQuery(
            "SELECT id, nombre, descripcion FROM materia ORDER BY nombre COLLATE NOCASE", null
        ).use { c ->
            while (c.moveToNext()) out.add(Materia(c.getLong(0), c.getString(1), c.getString(2) ?: ""))
        }
        return out
    }

    fun guardarMateria(id: Long, nombre: String, descripcion: String) {
        val n = nombre.trim()
        if (n.isEmpty()) throw ValidationException("El nombre de la materia es obligatorio.")
        if (n.length > 100) throw ValidationException("El nombre no puede superar 100 caracteres.")
        tx { db ->
            val cv = ContentValues().apply {
                put("nombre", n)
                putTexto("descripcion", descripcion.trim())
            }
            if (id == 0L) db.insertOrThrow("materia", null, cv)
            else if (db.update("materia", cv, "id = ?", arrayOf(id.toString())) == 0)
                throw ValidationException("La materia ya no existe.")
            Unit
        }
    }

    /** Elimina materia → actividades → calificaciones en cascada, dentro de una sola transacción (8.1). */
    fun eliminarMateria(id: Long) {
        tx { db -> db.delete("materia", "id = ?", arrayOf(id.toString())) }
    }

    // ---------- actividades ----------

    fun actividades(): List<Actividad> {
        val out = mutableListOf<Actividad>()
        helper.readableDatabase.rawQuery(
            "SELECT a.id, a.materia_id, m.nombre, a.nombre, a.descripcion, a.fecha, " +
                "a.porcentaje, a.estado, a.calificacion " +
                "FROM actividad a JOIN materia m ON m.id = a.materia_id ORDER BY a.fecha", null
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    Actividad(
                        id = c.getLong(0),
                        materiaId = c.getLong(1),
                        materiaNombre = c.getString(2),
                        nombre = c.getString(3),
                        descripcion = c.getString(4) ?: "",
                        fecha = LocalDateTime.parse(c.getString(5), fmt),
                        porcentaje = c.getDouble(6),
                        estado = Estado.desde(c.getString(7)),
                        calificacion = if (c.isNull(8)) null else c.getDouble(8)
                    )
                )
            }
        }
        return out
    }

    private fun sumaPorcentajes(db: SQLiteDatabase, materiaId: Long, excluirId: Long): BigDecimal {
        var suma = BigDecimal.ZERO
        db.rawQuery(
            "SELECT porcentaje FROM actividad WHERE materia_id = ? AND id <> ?",
            arrayOf(materiaId.toString(), excluirId.toString())
        ).use { c ->
            while (c.moveToNext()) suma = suma.add(BigDecimal.valueOf(c.getDouble(0)))
        }
        return suma
    }

    fun guardarActividad(
        id: Long, materiaId: Long, nombre: String, descripcion: String,
        fecha: LocalDateTime?, porcentaje: Double?, estado: Estado,
        calificacion: Double?, ahora: LocalDateTime
    ) {
        val n = nombre.trim()
        if (n.isEmpty()) throw ValidationException("El nombre de la actividad es obligatorio.")
        if (n.length > 100) throw ValidationException("El nombre no puede superar 100 caracteres.")
        if (fecha == null) throw ValidationException("Selecciona la fecha y la hora límite.")
        if (porcentaje == null || !porcentaje.isFinite())
            throw ValidationException("Ingresa un porcentaje válido, mayor que 0 % y de hasta 100 %.")
        val pct = r2(porcentaje)
        if (pct <= 0.0 || pct > 100.0)
            throw ValidationException("El porcentaje debe ser mayor que 0 % y de hasta 100 %.")
        val nota = calificacion?.let { r2(it) }
        if (nota != null && (nota < 0.0 || nota > 5.0))
            throw ValidationException("La calificación debe estar entre 0.0 y 5.0.")

        tx { db ->
            val usado = sumaPorcentajes(db, materiaId, id)
            if (usado.add(BigDecimal.valueOf(pct)).compareTo(cien) > 0) {
                throw ValidationException(
                    "La suma de porcentajes de la materia no puede superar 100 %. " +
                        "Ya hay ${Calc.porcentaje(usado)} asignado; disponible: ${Calc.porcentaje(cien.subtract(usado))}."
                )
            }
            val estadoFinal = normalizarEstado(if (id == 0L) Estado.PENDIENTE else estado, fecha, ahora)
            val cv = ContentValues().apply {
                put("materia_id", materiaId)
                put("nombre", n)
                putTexto("descripcion", descripcion.trim())
                put("fecha", fmt.format(fecha))
                put("porcentaje", pct)
                put("estado", estadoFinal.etiqueta)
                if (nota == null) putNull("calificacion") else put("calificacion", nota)
            }
            if (id == 0L) db.insertOrThrow("actividad", null, cv)
            else if (db.update("actividad", cv, "id = ?", arrayOf(id.toString())) == 0)
                throw ValidationException("La actividad ya no existe.")
            Unit
        }
    }

    /** Eliminar la calificación NO elimina la actividad (sección 13, RI-09). */
    fun eliminarCalificacion(id: Long) {
        tx { db ->
            val cv = ContentValues().apply { putNull("calificacion") }
            db.update("actividad", cv, "id = ?", arrayOf(id.toString()))
        }
    }

    /** Eliminar la actividad elimina también su calificación (RI-10). */
    fun eliminarActividad(id: Long) {
        tx { db -> db.delete("actividad", "id = ?", arrayOf(id.toString())) }
    }

    /** Marca como Vencida toda actividad no completada cuya fecha límite ya pasó (hora local). */
    fun refrescarVencidas(ahora: LocalDateTime): Int = tx { db ->
        val cv = ContentValues().apply { put("estado", Estado.VENCIDA.etiqueta) }
        db.update(
            "actividad", cv,
            "estado IN ('Pendiente','En progreso') AND fecha < ?",
            arrayOf(fmt.format(ahora.truncatedTo(ChronoUnit.MINUTES)))
        )
    }

    // ---------- metas ----------

    fun metas(): List<Meta> {
        val db = helper.readableDatabase
        val partes = mutableMapOf<Long, MutableList<ParteMeta>>()
        db.rawQuery("SELECT id, meta_id, nombre, porcentaje, completada FROM parte_meta ORDER BY id", null).use { c ->
            while (c.moveToNext()) {
                partes.getOrPut(c.getLong(1)) { mutableListOf() }.add(
                    ParteMeta(c.getLong(0), c.getLong(1), c.getString(2), c.getDouble(3), c.getInt(4) == 1)
                )
            }
        }
        val out = mutableListOf<Meta>()
        db.rawQuery("SELECT id, nombre, descripcion FROM meta ORDER BY id", null).use { c ->
            while (c.moveToNext()) {
                out.add(Meta(c.getLong(0), c.getString(1), c.getString(2) ?: "", partes[c.getLong(0)] ?: emptyList()))
            }
        }
        return out
    }

    /** Crea o actualiza una meta y sus partes en UNA transacción (sección 21). */
    fun guardarMeta(id: Long, nombre: String, descripcion: String, partes: List<ParteInput>) {
        val n = nombre.trim()
        if (n.isEmpty()) throw ValidationException("El nombre de la meta es obligatorio.")
        if (partes.isEmpty()) throw ValidationException("Una meta debe tener al menos una parte.")
        var suma = BigDecimal.ZERO
        val limpias = partes.map { p ->
            val pn = p.nombre.trim()
            if (pn.isEmpty()) throw ValidationException("Todas las partes deben tener nombre.")
            val v = p.porcentaje
            if (v == null || !v.isFinite()) throw ValidationException("Cada parte necesita un porcentaje válido.")
            val r = r2(v)
            if (r <= 0.0 || r > 100.0)
                throw ValidationException("El porcentaje de cada parte debe ser mayor que 0 % y de hasta 100 %.")
            suma = suma.add(BigDecimal.valueOf(r))
            ParteInput(p.id, pn, r, p.completada)
        }
        if (suma.compareTo(cien) != 0)
            throw ValidationException(
                "Los porcentajes de las partes suman ${Calc.porcentaje(suma)}. Deben sumar exactamente 100 %."
            )
        val progreso = limpias.filter { it.completada }
            .fold(BigDecimal.ZERO) { a, p -> a.add(BigDecimal.valueOf(p.porcentaje!!)) }
        val estado = Calc.estadoMeta(progreso)

        tx { db ->
            val cvMeta = ContentValues().apply {
                put("nombre", n)
                putTexto("descripcion", descripcion.trim())
                put("estado", estado)
            }
            val metaId: Long = if (id == 0L) {
                db.insertOrThrow("meta", null, cvMeta)
            } else {
                if (db.update("meta", cvMeta, "id = ?", arrayOf(id.toString())) == 0)
                    throw ValidationException("La meta ya no existe.")
                db.delete("parte_meta", "meta_id = ?", arrayOf(id.toString()))
                id
            }
            for (p in limpias) {
                val cv = ContentValues().apply {
                    put("meta_id", metaId)
                    put("nombre", p.nombre)
                    put("porcentaje", p.porcentaje!!)
                    put("completada", if (p.completada) 1 else 0)
                }
                db.insertOrThrow("parte_meta", null, cv)
            }
        }
    }

    fun marcarParte(parteId: Long, completada: Boolean) {
        tx { db ->
            val cv = ContentValues().apply { put("completada", if (completada) 1 else 0) }
            db.update("parte_meta", cv, "id = ?", arrayOf(parteId.toString()))
            val metaId = db.rawQuery("SELECT meta_id FROM parte_meta WHERE id = ?", arrayOf(parteId.toString()))
                .use { c -> if (c.moveToFirst()) c.getLong(0) else throw ValidationException("La parte ya no existe.") }
            var progreso = BigDecimal.ZERO
            db.rawQuery(
                "SELECT porcentaje FROM parte_meta WHERE meta_id = ? AND completada = 1",
                arrayOf(metaId.toString())
            ).use { c -> while (c.moveToNext()) progreso = progreso.add(BigDecimal.valueOf(c.getDouble(0))) }
            val cvMeta = ContentValues().apply { put("estado", Calc.estadoMeta(progreso)) }
            db.update("meta", cvMeta, "id = ?", arrayOf(metaId.toString()))
        }
    }

    fun eliminarMeta(id: Long) {
        tx { db -> db.delete("meta", "id = ?", arrayOf(id.toString())) }
    }
}
