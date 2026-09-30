package com.eduapp.data

import java.math.BigDecimal
import java.time.LocalDateTime

/** Estados de una actividad (sección 9.3). */
enum class Estado(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    EN_PROGRESO("En progreso"),
    COMPLETADA("Completada"),
    VENCIDA("Vencida");

    companion object {
        fun desde(etiqueta: String): Estado =
            values().firstOrNull { it.etiqueta == etiqueta } ?: PENDIENTE
    }
}

data class Perfil(
    val alias: String = "Estudiante",
    val fotografia: String? = null,          // ruta relativa a filesDir (p. ej. "media/perfil_123.jpg")
    val duracionEstudio: Int = 25,
    val duracionDescanso: Int = 5,
    val recordatoriosActivados: Boolean = true,
    val minutosAntelacion: Int = 30
)

data class Materia(
    val id: Long,
    val nombre: String,
    val descripcion: String
)

data class Actividad(
    val id: Long,
    val materiaId: Long,
    val materiaNombre: String,
    val nombre: String,
    val descripcion: String,
    val fecha: LocalDateTime,
    val porcentaje: Double,
    val estado: Estado,
    val calificacion: Double?               // null = sin calificación (NO es 0.0)
)

data class ParteMeta(
    val id: Long,
    val metaId: Long,
    val nombre: String,
    val porcentaje: Double,
    val completada: Boolean
)

data class Meta(
    val id: Long,
    val nombre: String,
    val descripcion: String,
    val partes: List<ParteMeta>
) {
    val progreso: BigDecimal get() = Calc.progresoMeta(partes)
    val estado: String get() = Calc.estadoMeta(progreso)
}

/** Datos que llegan desde el formulario de una meta. */
data class ParteInput(
    val id: Long,
    val nombre: String,
    val porcentaje: Double?,
    val completada: Boolean
)
