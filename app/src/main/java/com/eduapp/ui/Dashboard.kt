package com.eduapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eduapp.UiState
import com.eduapp.data.Actividad
import com.eduapp.data.Calc
import com.eduapp.data.Estado
import java.time.LocalDateTime

@Composable
fun DashboardScreen(state: UiState, irA: (String) -> Unit, abrirActividad: (Actividad) -> Unit) {
    val ahora = LocalDateTime.now()
    val perfil = state.perfil
    val pendientes = state.actividades.filter { it.estado == Estado.PENDIENTE || it.estado == Estado.EN_PROGRESO }
    val vencidas = state.actividades.count { it.estado == Estado.VENCIDA }
    val proximas = pendientes.filter { !it.fecha.isBefore(ahora) }.sortedBy { it.fecha }.take(5)
    val recordatorios = if (perfil.recordatoriosActivados)
        pendientes.filter { !it.fecha.isBefore(ahora) && !it.fecha.isAfter(ahora.plusMinutes(perfil.minutosAntelacion.toLong())) }
    else emptyList()
    val completadas = state.actividades.count { it.estado == Estado.COMPLETADA }
    val total = state.actividades.size

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(perfil.fotografia, perfil.alias, 64.dp)
            Column {
                Text("Hola,", style = MaterialTheme.typography.bodyMedium)
                Text(perfil.alias, style = MaterialTheme.typography.headlineSmall)
            }
        }

        Tarjeta {
            Text("Promedio general", style = MaterialTheme.typography.labelLarge)
            Text(Calc.formatear(state.promedioGeneral), style = MaterialTheme.typography.headlineMedium)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { Tarjeta { Text("Materias"); Text("${state.materias.size}", style = MaterialTheme.typography.titleLarge) } }
            Box(Modifier.weight(1f)) { Tarjeta { Text("Pendientes"); Text("${pendientes.size}", style = MaterialTheme.typography.titleLarge) } }
            Box(Modifier.weight(1f)) { Tarjeta { Text("Vencidas"); Text("$vencidas", style = MaterialTheme.typography.titleLarge) } }
        }

        Tarjeta {
            Text("Progreso académico", style = MaterialTheme.typography.labelLarge)
            if (total == 0) VacioTexto("Aún no hay actividades registradas.")
            else {
                Barra(completadas.toFloat() / total)
                Text("$completadas de $total actividades completadas", style = MaterialTheme.typography.bodySmall)
            }
        }

        if (recordatorios.isNotEmpty()) {
            Titulo("Recordatorios pendientes")
            recordatorios.forEach { FilaActividad(it, true) { abrirActividad(it) } }
        }

        Titulo("Próximas actividades")
        if (proximas.isEmpty()) VacioTexto("No tienes actividades próximas.")
        proximas.forEach { FilaActividad(it, true) { abrirActividad(it) } }

        Titulo("Metas")
        if (state.metas.isEmpty()) VacioTexto("Aún no has creado metas.")
        state.metas.take(3).forEach { m ->
            Tarjeta {
                Text(m.nombre, style = MaterialTheme.typography.titleSmall)
                Barra((m.progreso.toFloat() / 100f))
                Text("${Calc.porcentaje(m.progreso)} · ${m.estado}", style = MaterialTheme.typography.bodySmall)
            }
        }

        Titulo("Accesos")
        listOf(
            listOf("materias" to "Materias", "calendario" to "Calendario"),
            listOf("metas" to "Metas", "estadisticas" to "Estadísticas"),
            listOf("pomodoro" to "Pomodoro", "perfil" to "Perfil")
        ).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { (ruta, texto) ->
                    Button(onClick = { irA(ruta) }, modifier = Modifier.weight(1f)) { Text(texto) }
                }
            }
        }
    }
}
