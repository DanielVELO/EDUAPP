package com.eduapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eduapp.UiState
import com.eduapp.data.Calc
import com.eduapp.data.Estado

@Composable
fun EstadisticasScreen(state: UiState) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Tarjeta {
            Text("Promedio general", style = MaterialTheme.typography.labelLarge)
            Text(Calc.formatear(state.promedioGeneral), style = MaterialTheme.typography.headlineMedium)
        }

        Titulo("Promedio por materia")
        if (state.materias.isEmpty()) VacioTexto("Aún no hay materias registradas.")
        state.materias.forEach { m ->
            Tarjeta {
                Row {
                    Text(m.nombre, Modifier.weight(1f))
                    Text(Calc.formatear(state.promedioMateria(m.id)), style = MaterialTheme.typography.titleSmall)
                }
            }
        }

        Titulo("Actividades por estado")
        val total = state.actividades.size
        if (total == 0) VacioTexto("Aún no hay actividades registradas.")
        else Tarjeta {
            listOf(Estado.COMPLETADA, Estado.EN_PROGRESO, Estado.PENDIENTE, Estado.VENCIDA).forEach { e ->
                val n = state.actividades.count { it.estado == e }
                Row {
                    Text(e.etiqueta, Modifier.weight(1f))
                    Text("$n")
                }
                Barra(n.toFloat() / total, colorEstado(e))
                Spacer(Modifier.height(4.dp))
            }
        }

        Titulo("Progreso de metas")
        if (state.metas.isEmpty()) VacioTexto("Aún no hay metas registradas.")
        state.metas.forEach { m ->
            Tarjeta {
                Text(m.nombre)
                Barra(m.progreso.toFloat() / 100f)
                Text("${Calc.porcentaje(m.progreso)} · ${m.estado}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
