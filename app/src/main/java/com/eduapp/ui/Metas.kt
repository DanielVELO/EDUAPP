package com.eduapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eduapp.AppViewModel
import com.eduapp.UiState
import com.eduapp.data.Calc
import com.eduapp.data.Meta
import com.eduapp.data.ParteInput
import java.math.BigDecimal

// ---------------------------------------------------------------- PANT-07 Metas
@Composable
fun MetasScreen(state: UiState, abrir: (Long) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        if (state.metas.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                VacioTexto("Aún no tienes metas. Toca + para crear la primera.")
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.metas, key = { it.id }) { m ->
                Tarjeta(onClick = { abrir(m.id) }) {
                    Text(m.nombre, style = MaterialTheme.typography.titleMedium)
                    Barra(m.progreso.toFloat() / 100f)
                    Text("${Calc.porcentaje(m.progreso)} · ${m.estado}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        FloatingActionButton(
            onClick = { abrir(0) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) { Icon(Icons.Default.Add, "Nueva meta") }
    }
}

private data class ParteEdit(val tid: Long, val id: Long, val nombre: String, val porcentaje: String, val completada: Boolean)

private var contadorTid = 1L

// ---------------------------------------------------------------- PANT-08 Detalle de meta
@Composable
fun MetaScreen(state: UiState, vm: AppViewModel, metaId: Long, volver: () -> Unit) {
    val meta = state.metas.find { it.id == metaId }
    if (metaId != 0L && meta == null) {
        Box(Modifier.fillMaxSize().padding(24.dp)) { VacioTexto("La meta ya no existe.") }
        return
    }
    var editando by remember(metaId) { mutableStateOf(metaId == 0L) }
    var confirmarBorrar by remember { mutableStateOf(false) }

    if (editando) {
        FormularioMeta(meta, vm, metaId,
            alCancelar = { if (metaId == 0L) volver() else editando = false },
            alGuardar = { if (metaId == 0L) volver() else editando = false })
    } else if (meta != null) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(meta.nombre, style = MaterialTheme.typography.headlineSmall)
            if (meta.descripcion.isNotBlank()) Text(meta.descripcion)
            Tarjeta {
                Barra(meta.progreso.toFloat() / 100f)
                Text("Progreso: ${Calc.porcentaje(meta.progreso)} · ${meta.estado}")
            }
            Titulo("Partes")
            meta.partes.forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = p.completada, onCheckedChange = { vm.marcarParte(p.id, it) })
                    Text(p.nombre, Modifier.weight(1f))
                    Text(Calc.porcentaje(BigDecimal.valueOf(p.porcentaje)))
                }
            }
            Button(onClick = { editando = true }, modifier = Modifier.fillMaxWidth()) { Text("Editar meta") }
            TextButton(onClick = { confirmarBorrar = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Eliminar meta", color = MaterialTheme.colorScheme.error)
            }
        }
    }
    if (confirmarBorrar && meta != null) {
        Confirmar(
            "Eliminar meta",
            "Se eliminará «${meta.nombre}» junto con todas sus partes. Esta acción no se puede deshacer.",
            onConfirmar = { confirmarBorrar = false; vm.eliminarMeta(meta.id, volver) },
            onCancelar = { confirmarBorrar = false }
        )
    }
}

@Composable
private fun FormularioMeta(meta: Meta?, vm: AppViewModel, metaId: Long, alCancelar: () -> Unit, alGuardar: () -> Unit) {
    var nombre by remember(metaId) { mutableStateOf(meta?.nombre ?: "") }
    var desc by remember(metaId) { mutableStateOf(meta?.descripcion ?: "") }
    val partes: SnapshotStateList<ParteEdit> = remember(metaId) {
        mutableStateListOf<ParteEdit>().apply {
            meta?.partes?.forEach {
                add(ParteEdit(contadorTid++, it.id, it.nombre, BigDecimal.valueOf(it.porcentaje).stripTrailingZeros().toPlainString(), it.completada))
            }
            if (isEmpty()) add(ParteEdit(contadorTid++, 0, "", "", false))
        }
    }
    val suma = partes.fold(BigDecimal.ZERO) { a, p -> a.add(p.porcentaje.aDecimal()?.let { BigDecimal.valueOf(it) } ?: BigDecimal.ZERO) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre de la meta") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(desc, { desc = it }, label = { Text("Descripción (opcional)") }, modifier = Modifier.fillMaxWidth())
        Titulo("Partes")
        partes.forEachIndexed { i, p ->
            key(p.tid) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        p.nombre, { partes[i] = p.copy(nombre = it) }, label = { Text("Parte") },
                        singleLine = true, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        p.porcentaje, { partes[i] = p.copy(porcentaje = it) }, label = { Text("%") },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(84.dp)
                    )
                    IconButton(onClick = { partes.removeAt(i) }) { Icon(Icons.Default.Delete, "Quitar parte") }
                }
            }
        }
        OutlinedButton(onClick = { partes.add(ParteEdit(contadorTid++, 0, "", "", false)) }, modifier = Modifier.fillMaxWidth()) {
            Text("Añadir parte")
        }
        val ok = suma.compareTo(BigDecimal(100)) == 0
        Text(
            "Suma actual: ${Calc.porcentaje(suma)} (debe ser exactamente 100 % al guardar)",
            color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                vm.guardarMeta(
                    metaId, nombre, desc,
                    partes.map { ParteInput(it.id, it.nombre, it.porcentaje.aDecimal(), it.completada) },
                    alGuardar
                )
            }
        ) { Text("Guardar meta") }
        TextButton(onClick = alCancelar, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
    }
}
