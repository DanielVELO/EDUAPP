package com.eduapp.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eduapp.AppViewModel
import com.eduapp.UiState

@Composable
fun PerfilScreen(state: UiState, vm: AppViewModel) {
    val perfil = state.perfil
    var alias by remember { mutableStateOf(perfil.alias) }
    var recordatorios by remember { mutableStateOf(perfil.recordatoriosActivados) }
    var minutos by remember { mutableStateOf(perfil.minutosAntelacion.toString()) }

    val selector = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.cambiarFoto(uri)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Avatar(perfil.fotografia, perfil.alias, 120.dp)
        OutlinedButton(onClick = { selector.launch("image/*") }) { Text("Cambiar fotografía") }
        Text("Formatos permitidos: .jpg, .jpeg, .png, .webp", style = MaterialTheme.typography.bodySmall)

        OutlinedTextField(
            alias, { alias = it }, label = { Text("Alias") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )

        Titulo("Recordatorios de actividades")
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Activar recordatorios", Modifier.weight(1f))
            Switch(checked = recordatorios, onCheckedChange = { recordatorios = it })
        }
        OutlinedTextField(
            minutos, { minutos = it }, label = { Text("Avisar con antelación (minutos)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            enabled = recordatorios, modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Los avisos se generan mientras la aplicación está abierta. Si no los recibes, revisa el permiso de " +
                "notificaciones de EDUAPP en los ajustes de Android.",
            style = MaterialTheme.typography.bodySmall
        )

        Button(
            onClick = { vm.guardarPerfil(alias, recordatorios, minutos.trim().toIntOrNull()) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Guardar cambios") }
    }
}
