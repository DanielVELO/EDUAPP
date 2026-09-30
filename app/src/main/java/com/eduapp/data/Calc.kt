package com.eduapp.data

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Cálculos académicos (secciones 14 a 18 y 22 a 23).
 * Código puro de Kotlin: se puede probar sin Android.
 * Regla clave: NADA se redondea aquí; el redondeo a 1 decimal es solo visual (formatear).
 */
object Calc {
    private val MC = MathContext.DECIMAL128
    private val CIEN = BigDecimal(100)

    const val SIN_CALIFICACIONES = "Sin calificaciones"

    /** Promedio ponderado de una materia usando solo actividades con calificación != null. */
    fun promedioMateria(actividades: List<Actividad>): BigDecimal? {
        val calificadas = actividades.filter { it.calificacion != null }
        if (calificadas.isEmpty()) return null
        var numerador = BigDecimal.ZERO
        var denominador = BigDecimal.ZERO
        for (a in calificadas) {
            val p = BigDecimal.valueOf(a.porcentaje).divide(CIEN, MC)
            numerador = numerador.add(BigDecimal.valueOf(a.calificacion!!).multiply(p, MC), MC)
            denominador = denominador.add(p, MC)
        }
        return numerador.divide(denominador, MC)
    }

    /** Promedio simple entre materias con al menos una calificación, con valores internos sin redondear. */
    fun promedioGeneral(materias: List<Materia>, actividades: List<Actividad>): BigDecimal? {
        val promedios = materias.mapNotNull { m ->
            promedioMateria(actividades.filter { it.materiaId == m.id })
        }
        if (promedios.isEmpty()) return null
        val suma = promedios.fold(BigDecimal.ZERO) { acc, v -> acc.add(v, MC) }
        return suma.divide(BigDecimal(promedios.size), MC)
    }

    /** Redondeo VISUAL a una cifra decimal. */
    fun formatear(valor: BigDecimal?): String =
        valor?.setScale(1, RoundingMode.HALF_UP)?.toPlainString() ?: SIN_CALIFICACIONES

    fun formatearNota(nota: Double?): String =
        if (nota == null) "Sin nota" else BigDecimal.valueOf(nota).stripTrailingZeros().let {
            if (it.scale() < 1) it.setScale(1) else it
        }.toPlainString()

    fun porcentaje(valor: BigDecimal): String =
        (if (valor.signum() == 0) "0" else valor.stripTrailingZeros().toPlainString()) + " %"

    /** Suma de porcentajes de las partes completadas (sección 22). */
    fun progresoMeta(partes: List<ParteMeta>): BigDecimal =
        partes.filter { it.completada }
            .fold(BigDecimal.ZERO) { acc, p -> acc.add(BigDecimal.valueOf(p.porcentaje)) }

    /** Sección 23: 0 % No iniciada, 1–99 % En progreso, 100 % Alcanzada. */
    fun estadoMeta(progreso: BigDecimal): String = when {
        progreso.signum() == 0 -> "No iniciada"
        progreso.compareTo(CIEN) >= 0 -> "Alcanzada"
        else -> "En progreso"
    }
}
