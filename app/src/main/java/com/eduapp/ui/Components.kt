package com.eduapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eduapp.data.Actividad
import com.eduapp.data.Estado
import com.eduapp.data.ImageStore
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val FORMATO_FECHA_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun LocalDateTime.texto(): String = format(FORMATO_FECHA_HORA)

/** Acepta coma o punto decimal. Devuelve null si está vacío o no es un número finito. */
fun String.aDecimal(): Double? =
    trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

fun colorEstado(e: Estado): Color = when (e) {
    Estado.PENDIENTE -> Color(0xFFB26A00)
    Estado.EN_PROGRESO -> Color(0xFF1B4F9C)
    Estado.COMPLETADA -> Color(0xFF2E7D32)
    Estado.VENCIDA -> Color(0xFFB3261E)
}

@Composable
fun EstadoTag(estado: Estado) {
    val c = colorEstado(estado)
    Text(
        estado.etiqueta,
        color = c,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(c.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

@Composable
fun Tarjeta(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = contenido)
    }
}

@Composable
fun Titulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
fun VacioTexto(texto: String) {
    Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun Confirmar(
    titulo: String,
    texto: String,
    botonConfirmar: String = "Eliminar",
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = { TextButton(onClick = onConfirmar) { Text(botonConfirmar, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

@Composable
fun Avatar(rutaRelativa: String?, alias: String, tamano: Dp) {
    val ctx = LocalContext.current
    val bmp = remember(rutaRelativa) { rutaRelativa?.let { ImageStore.cargar(ctx, it, 256) } }
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Fotografía de perfil",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(tamano).clip(CircleShape)
        )
    } else {
        Box(
            Modifier.size(tamano).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                alias.take(1).uppercase(),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
fun FilaActividad(a: Actividad, mostrarMateria: Boolean, onClick: () -> Unit) {
    Tarjeta(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(a.nombre, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            EstadoTag(a.estado)
        }
        if (mostrarMateria) Text(a.materiaNombre, style = MaterialTheme.typography.bodySmall)
        Text(
            "Límite: ${a.fecha.texto()}  ·  ${com.eduapp.data.Calc.porcentaje(java.math.BigDecimal.valueOf(a.porcentaje))}  ·  " +
                "Nota: ${com.eduapp.data.Calc.formatearNota(a.calificacion)}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun Barra(fraccion: Float, color: Color = MaterialTheme.colorScheme.primary) {
    LinearProgressIndicator(
        progress = { fraccion.coerceIn(0f, 1f) },
        color = color,
        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
    )
}
