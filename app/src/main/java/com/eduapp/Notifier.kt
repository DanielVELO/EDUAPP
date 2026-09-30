package com.eduapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.eduapp.data.Actividad
import java.time.format.DateTimeFormatter

/** Recordatorios de actividades próximas (sección 52). */
object Notifier {
    private const val CANAL = "recordatorios"
    const val EXTRA_ACTIVIDAD = "actividad_id"

    fun crearCanal(ctx: Context) {
        val canal = NotificationChannel(CANAL, "Recordatorios de actividades", NotificationManager.IMPORTANCE_HIGH)
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    @SuppressLint("MissingPermission")
    fun mostrar(ctx: Context, a: Actividad) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTIVIDAD, a.id)
        }
        val pi = PendingIntent.getActivity(
            ctx, a.id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dia = a.fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        val hora = a.fecha.format(DateTimeFormatter.ofPattern("HH:mm"))
        val texto = "La actividad '${a.nombre}' de la materia '${a.materiaNombre}' vence el $dia a las $hora."
        val n = NotificationCompat.Builder(ctx, CANAL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("${a.nombre} · ${a.materiaNombre}")
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(a.id.toInt(), n)
        } catch (_: SecurityException) {
        }
    }
}
