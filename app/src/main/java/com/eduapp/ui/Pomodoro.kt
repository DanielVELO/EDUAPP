package com.eduapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eduapp.AppViewModel
import com.eduapp.FasePomodoro
import com.eduapp.ModoPomodoro
import com.eduapp.UiState

@Composable
fun PomodoroScreen(state: UiState, vm: AppViewModel) {
    val p by vm.pomodoro.collectAsStateWithLifecycle()
    val perfil = state.perfil
    var estudio by remember(perfil.duracionEstudio) { mutableStateOf(perfil.duracionEstudio.toString()) }
    var descanso by remember(perfil.duracionDescanso) { mutableStateOf(perfil.duracionDescanso.toString()) }

    val segundos = when {
        p.terminado -> 0
        p.fase == FasePomodoro.REPOSO ->
            (if (p.modo == ModoPomodoro.ESTUDIO) perfil.duracionEstudio else perfil.duracionDescanso) * 60
        else -> p.restanteSeg
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val enReposo = p.fase == FasePomodoro.REPOSO
            if (p.modo == ModoPomodoro.ESTUDIO) {
                Button(onClick = {}, enabled = enReposo) { Text("Estudio") }
                OutlinedButton(onClick = { vm.pomoModo(ModoPomodoro.DESCANSO) }, enabled = enReposo) { Text("Descanso") }
            } else {
                OutlinedButton(onClick = { vm.pomoModo(ModoPomodoro.ESTUDIO) }, enabled = enReposo) { Text("Estudio") }
                Button(onClick = {}, enabled = enReposo) { Text("Descanso") }
            }
        }

        Text(
            "%02d:%02d".format(segundos / 60, segundos % 60),
            fontSize = 72.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (p.fase) {
                FasePomodoro.REPOSO -> Button(onClick = { vm.pomoIniciar() }) { Text("Iniciar") }
                FasePomodoro.CORRIENDO -> Button(onClick = { vm.pomoPausar() }) { Text("Pausar") }
                FasePomodoro.PAUSADO -> Button(onClick = { vm.pomoReanudar() }) { Text("Reanudar") }
            }
            OutlinedButton(onClick = { vm.pomoIniciar() }, enabled = p.fase != FasePomodoro.REPOSO) { Text("Reiniciar") }
            OutlinedButton(onClick = { vm.pomoFinalizar() }, enabled = p.fase != FasePomodoro.REPOSO) { Text("Finalizar") }
        }

        HorizontalDivider()
        Titulo("Configuración (minutos)")
        OutlinedTextField(
            estudio, { estudio = it }, label = { Text("Estudio") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            descanso, { descanso = it }, label = { Text("Descanso (mínimo 5)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { vm.guardarPomodoro(estudio.trim().toIntOrNull(), descanso.trim().toIntOrNull()) },
            enabled = p.fase == FasePomodoro.REPOSO,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Guardar configuración") }
    }

    if (p.terminado) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Pomodoro") },
            text = { Text("El período ha terminado. ¿Deseas reiniciar el temporizador o dar por terminada la sesión?") },
            confirmButton = { TextButton(onClick = { vm.pomoReiniciarDesdeDialogo() }) { Text("Reiniciar") } },
            dismissButton = { TextButton(onClick = { vm.pomoFinalizar() }) { Text("Dar por terminado") } }
        )
    }
}
