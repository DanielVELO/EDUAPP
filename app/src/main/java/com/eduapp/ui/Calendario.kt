package com.eduapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eduapp.UiState
import com.eduapp.data.Actividad
import com.eduapp.data.Estado
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarioScreen(state: UiState, abrirActividad: (Actividad) -> Unit) {
    var mes by remember { mutableStateOf(YearMonth.now()) }
    var seleccionado by remember { mutableStateOf(LocalDate.now()) }
    val es = Locale.forLanguageTag("es")

    val porDia = state.actividades.groupBy { it.fecha.toLocalDate() }
    val delDia = porDia[seleccionado].orEmpty()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { mes = mes.minusMonths(1) }) { Text("<") }
            Text(
                mes.month.getDisplayName(TextStyle.FULL, es).replaceFirstChar { it.uppercase() } + " " + mes.year,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { mes = mes.plusMonths(1) }) { Text(">") }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("L", "M", "X", "J", "V", "S", "D").forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
            }
        }
        val desplazamiento = mes.atDay(1).dayOfWeek.value - 1
        val celdas = desplazamiento + mes.lengthOfMonth()
        val filas = (celdas + 6) / 7
        for (f in 0 until filas) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val dia = f * 7 + c - desplazamiento + 1
                    Box(Modifier.weight(1f).height(46.dp), contentAlignment = Alignment.Center) {
                        if (dia in 1..mes.lengthOfMonth()) {
                            val fecha = mes.atDay(dia)
                            val acts = porDia[fecha].orEmpty()
                            val sel = fecha == seleccionado
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (sel) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)
                                    .clickable { seleccionado = fecha }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("$dia", style = MaterialTheme.typography.bodyMedium)
                                if (acts.isNotEmpty()) {
                                    val color = if (acts.any { it.estado == Estado.VENCIDA }) colorEstado(Estado.VENCIDA)
                                    else MaterialTheme.colorScheme.primary
                                    Box(Modifier.size(6.dp).clip(CircleShape).background(color))
                                } else Spacer(Modifier.size(6.dp))
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Titulo("Actividades del ${seleccionado.format(FORMATO_FECHA)}")
        if (delDia.isEmpty()) VacioTexto("No hay actividades para este día.")
        delDia.forEach { FilaActividad(it, true) { abrirActividad(it) } }
    }
}
