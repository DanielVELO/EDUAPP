package com.eduapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.eduapp.ui.AppRoot
import com.eduapp.ui.EduAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    private var actividadAbrir by mutableStateOf<Long?>(null)

    private val pedirPermiso = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifier.crearCanal(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)

        if (savedInstanceState == null) actividadAbrir = leerExtra(intent)

        // Revisión periódica de actividades próximas mientras la app está abierta.
        lifecycleScope.launch {
            while (isActive) {
                vm.tick()
                delay(30_000)
            }
        }

        setContent {
            EduAppTheme {
                AppRoot(vm, actividadAbrir) {
                    actividadAbrir = null
                    intent.removeExtra(Notifier.EXTRA_ACTIVIDAD)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        actividadAbrir = leerExtra(intent)
    }

    private fun leerExtra(i: Intent?): Long? =
        i?.getLongExtra(Notifier.EXTRA_ACTIVIDAD, -1L)?.takeIf { it > 0 }
}
