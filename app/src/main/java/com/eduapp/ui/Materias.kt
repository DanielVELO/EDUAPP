package com.eduapp.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eduapp.AppViewModel
import com.eduapp.UiState
import com.eduapp.data.Actividad
import com.eduapp.data.Calc
import com.eduapp.data.Estado
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// ---------------------------------------------------------------- PANT-03 Materias
@Composable
fun MateriasScreen(state: UiState, vm: AppViewModel, abrir: (Long) -> Unit) {
    var editando by remember { mutableStateOf<com.eduapp.data.Materia?>(null) }
    var creando by remember { mutableStateOf(false) }
    var eliminar by remember { mutableStateOf<com.eduapp.data.Materia?>(null) }

    Box(Modifier.fillMaxSize()) {
        if (state.materias.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                VacioTexto("Aún no tienes materias. Toca + para crear la primera.")
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.materias, key = { it.id }) { m ->
                val cant = state.actividades.count { it.materiaId == m.id }
                Tarjeta(onClick = { abrir(m.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(m.nombre, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Promedio: ${Calc.formatear(state.promedioMateria(m.id))}  ·  $cant actividades",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(onClick = { editando = m }) { Icon(Icons.Default.Edit, "Editar materia") }
                        IconButton(onClick = { eliminar = m }) { Icon(Icons.Default.Delete, "Eliminar materia") }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { creando = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) { Icon(Icons.Default.Add, "Nueva materia") }
    }

    if (creando || editando != null) {
        val m = editando
        MateriaDialogo(
            inicialNombre = m?.nombre ?: "", inicialDescripcion = m?.descripcion ?: "",
            onGuardar = { n, d -> vm.guardarMateria(m?.id ?: 0L, n, d); creando = false; editando = null },
            onCerrar = { creando = false; editando = null }
        )
    }
    eliminar?.let { m ->
        Confirmar(
            "Eliminar materia",
            "Se eliminará «${m.nombre}» junto con todas sus actividades y calificaciones. " +
                "Los promedios se recalcularán. Esta acción no se puede deshacer.",
            onConfirmar = { vm.eliminarMateria(m.id); eliminar = null },
            onCancelar = { eliminar = null }
        )
    }
}

@Composable
private fun MateriaDialogo(
    inicialNombre: String, inicialDescripcion: String,
    onGuardar: (String, String) -> Unit, onCerrar: () -> Unit
) {
    var nombre by remember { mutableStateOf(inicialNombre) }
    var desc by remember { mutableStateOf(inicialDescripcion) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (inicialNombre.isEmpty()) "Nueva materia" else "Editar materia") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    nombre, { nombre = it; error = null }, label = { Text("Nombre") }, singleLine = true,
                    isError = error != null, supportingText = { error?.let { Text(it) } }
                )
                OutlinedTextField(desc, { desc = it }, label = { Text("Descripción (opcional)") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    nombre.isBlank() -> error = "El nombre es obligatorio."
                    nombre.trim().length > 100 -> error = "Máximo 100 caracteres."
                    else -> onGuardar(nombre, desc)
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}

// ---------------------------------------------------------------- PANT-04 Detalle de materia
@Composable
fun MateriaDetalleScreen(
    state: UiState, materiaId: Long,
    nuevaActividad: (Long) -> Unit, abrirActividad: (Actividad) -> Unit
) {
    val materia = state.materias.find { it.id == materiaId }
    if (materia == null) {
        Box(Modifier.fillMaxSize().padding(24.dp)) { VacioTexto("La materia ya no existe.") }
        return
    }
    val acts = state.actividades.filter { it.materiaId == materiaId }
    val asignado = acts.fold(BigDecimal.ZERO) { a, x -> a.add(BigDecimal.valueOf(x.porcentaje)) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(materia.nombre, style = MaterialTheme.typography.headlineSmall)
        if (materia.descripcion.isNotBlank()) Text(materia.descripcion)
        Tarjeta {
            Text("Promedio de la materia", style = MaterialTheme.typography.labelLarge)
            Text(Calc.formatear(state.promedioMateria(materiaId)), style = MaterialTheme.typography.headlineMedium)
            Text("Porcentaje asignado: ${Calc.porcentaje(asignado)} de 100 %", style = MaterialTheme.typography.bodySmall)
        }
        Button(onClick = { nuevaActividad(materiaId) }, modifier = Modifier.fillMaxWidth()) { Text("Nueva actividad") }
        Titulo("Actividades")
        if (acts.isEmpty()) VacioTexto("Esta materia aún no tiene actividades.")
        acts.forEach { FilaActividad(it, false) { abrirActividad(it) } }
    }
}

// ---------------------------------------------------------------- PANT-05 Actividad
@Composable
fun ActividadScreen(state: UiState, vm: AppViewModel, materiaId: Long, actividadId: Long, volver: () -> Unit) {
    val existente = state.actividades.find { it.id == actividadId }
    if (actividadId != 0L && existente == null) {
        Box(Modifier.fillMaxSize().padding(24.dp)) { VacioTexto("La actividad ya no existe.") }
        return
    }
    val materia = state.materias.find { it.id == materiaId }
    val ctx = LocalContext.current

    var nombre by remember(actividadId) { mutableStateOf(existente?.nombre ?: "") }
    var desc by remember(actividadId) { mutableStateOf(existente?.descripcion ?: "") }
    var fecha by remember(actividadId) { mutableStateOf(existente?.fecha?.toLocalDate()) }
    var hora by remember(actividadId) { mutableStateOf(existente?.fecha?.toLocalTime() ?: LocalTime.of(23, 59)) }
    var porcentaje by remember(actividadId) {
        mutableStateOf(existente?.let { BigDecimal.valueOf(it.porcentaje).stripTrailingZeros().toPlainString() } ?: "")
    }
    var estado by remember(actividadId) { mutableStateOf(existente?.estado ?: Estado.PENDIENTE) }
    var nota by remember(actividadId) {
        mutableStateOf(existente?.calificacion?.let { BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() } ?: "")
    }
    var confirmarNota by remember { mutableStateOf(false) }
    var confirmarBorrar by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(materia?.nombre ?: existente?.materiaNombre ?: "", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(desc, { desc = it }, label = { Text("Descripción (opcional)") }, modifier = Modifier.fillMaxWidth())

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    val f = fecha ?: LocalDate.now()
                    DatePickerDialog(ctx, { _, y, m, d -> fecha = LocalDate.of(y, m + 1, d) }, f.year, f.monthValue - 1, f.dayOfMonth).show()
                },
                modifier = Modifier.weight(1f)
            ) { Text(fecha?.format(FORMATO_FECHA) ?: "Fecha límite") }
            OutlinedButton(
                onClick = {
                    TimePickerDialog(ctx, { _, h, min -> hora = LocalTime.of(h, min) }, hora.hour, hora.minute, true).show()
                },
                modifier = Modifier.weight(1f)
            ) { Text("%02d:%02d".format(hora.hour, hora.minute)) }
        }

        OutlinedTextField(
            porcentaje, { porcentaje = it }, label = { Text("Porcentaje (> 0 y ≤ 100)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()
        )

        if (existente != null) {
            Text("Estado", style = MaterialTheme.typography.labelLarge)
            if (estado == Estado.VENCIDA) {
                Text("Vencida (se asignó automáticamente porque la fecha límite pasó)", color = colorEstado(Estado.VENCIDA))
            }
            listOf(Estado.PENDIENTE, Estado.EN_PROGRESO, Estado.COMPLETADA).forEach { e ->
                Row(Modifier.fillMaxWidth().clickable { estado = e }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = estado == e, onClick = { estado = e })
                    Text(e.etiqueta)
                }
            }
        }

        OutlinedTextField(
            nota, { nota = it }, label = { Text("Calificación (0.0 – 5.0, opcional)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val tenia = existente?.calificacion != null
                if (tenia && nota.isBlank()) {
                    vm.mostrarMensaje("Para quitar la calificación usa el botón «Eliminar calificación».")
                    return@Button
                }
                if (nota.isNotBlank() && nota.aDecimal() == null) {
                    vm.mostrarMensaje("La calificación debe ser un número entre 0.0 y 5.0.")
                    return@Button
                }
                if (porcentaje.isNotBlank() && porcentaje.aDecimal() == null) {
                    vm.mostrarMensaje("El porcentaje debe ser un número válido.")
                    return@Button
                }
                vm.guardarActividad(
                    actividadId, materiaId, nombre, desc,
                    fecha?.let { LocalDateTime.of(it, hora) },
                    porcentaje.aDecimal(), estado, nota.aDecimal(), volver
                )
            }
        ) { Text("Guardar") }

        if (existente != null) {
            if (existente.calificacion != null) {
                OutlinedButton(onClick = { confirmarNota = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Eliminar calificación")
                }
            }
            TextButton(onClick = { confirmarBorrar = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Eliminar actividad", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (confirmarNota && existente != null) {
        Confirmar(
            "Eliminar calificación",
            "La actividad permanecerá, pero quedará sin nota y dejará de contar en los promedios.",
            onConfirmar = { confirmarNota = false; nota = ""; vm.eliminarCalificacion(existente.id) {} },
            onCancelar = { confirmarNota = false }
        )
    }
    if (confirmarBorrar && existente != null) {
        Confirmar(
            "Eliminar actividad",
            "Se eliminará «${existente.nombre}» y también su calificación. Los promedios se recalcularán.",
            onConfirmar = { confirmarBorrar = false; vm.eliminarActividad(existente.id, volver) },
            onCancelar = { confirmarBorrar = false }
        )
    }
}
