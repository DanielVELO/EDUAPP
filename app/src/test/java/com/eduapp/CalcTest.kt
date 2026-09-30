package com.eduapp

import com.eduapp.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class CalcTest {
    private fun act(id: Long, materia: Long, pct: Double, nota: Double?) = Actividad(
        id, materia, "M$materia", "A$id", "", LocalDateTime.of(2026, 10, 1, 12, 0),
        pct, Estado.PENDIENTE, nota
    )

    private fun materia(id: Long) = Materia(id, "M$id", "")

    @Test fun pf06_promedioPonderado() {
        val p = Calc.promedioMateria(listOf(act(1, 1, 30.0, 4.0), act(2, 1, 20.0, 3.5)))
        assertEquals("3.8", Calc.formatear(p))
    }

    @Test fun pf07_sinNotaNoCuentaComoCero() {
        val p = Calc.promedioMateria(listOf(act(1, 1, 30.0, 4.0), act(2, 1, 20.0, null)))
        assertEquals("4.0", Calc.formatear(p))
    }

    @Test fun pf08_sinRedondeoIntermedio() {
        val acts = listOf(act(1, 1, 100.0, 3.84), act(2, 2, 100.0, 3.25))
        val general = Calc.promedioGeneral(listOf(materia(1), materia(2)), acts)!!
        assertEquals(0, general.compareTo(BigDecimal("3.545")))
        assertEquals("3.5", Calc.formatear(general))
    }

    @Test fun pf09_pf10_sinCalificaciones() {
        assertNull(Calc.promedioMateria(listOf(act(1, 1, 50.0, null))))
        assertEquals("Sin calificaciones", Calc.formatear(Calc.promedioGeneral(listOf(materia(1)), listOf(act(1, 1, 50.0, null)))))
    }

    @Test fun promedioGeneralExcluyeMateriasSinNotas() {
        val acts = listOf(act(1, 1, 100.0, 4.0), act(2, 2, 100.0, null))
        assertEquals("4.0", Calc.formatear(Calc.promedioGeneral(listOf(materia(1), materia(2)), acts)))
    }

    @Test fun estadosDeMeta() {
        assertEquals("No iniciada", Calc.estadoMeta(BigDecimal.ZERO))
        assertEquals("En progreso", Calc.estadoMeta(BigDecimal("40")))
        assertEquals("Alcanzada", Calc.estadoMeta(BigDecimal("100")))
    }
}
