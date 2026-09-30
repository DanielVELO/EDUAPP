package com.eduapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.eduapp.AppViewModel
import kotlinx.coroutines.launch

private data class Destino(val ruta: String, val titulo: String)

private val PRINCIPALES = listOf(
    Destino("dashboard", "Dashboard"),
    Destino("materias", "Materias"),
    Destino("calendario", "Calendario"),
    Destino("metas", "Metas"),
    Destino("estadisticas", "Estadísticas"),
    Destino("pomodoro", "Pomodoro"),
    Destino("perfil", "Perfil")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: AppViewModel, abrirActividadId: Long?, alAbrirActividad: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()

    if (!state.cargado) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val nav = rememberNavController()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val entrada by nav.currentBackStackEntryAsState()
    val ruta = entrada?.destination?.route

    LaunchedEffect(Unit) { vm.mensajes.collect { snack.showSnackbar(it) } }

    // Abrir el detalle de una actividad al tocar una notificación.
    LaunchedEffect(abrirActividadId) {
        if (abrirActividadId != null) {
            state.actividades.find { it.id == abrirActividadId }?.let {
                nav.navigate("actividad/${it.materiaId}/${it.id}")
            }
            alAbrirActividad()
        }
    }

    val esPrincipal = PRINCIPALES.any { it.ruta == ruta }
    val titulo = PRINCIPALES.find { it.ruta == ruta }?.titulo ?: when {
        ruta?.startsWith("materia/") == true -> "Detalle de materia"
        ruta?.startsWith("actividad/") == true -> "Actividad"
        ruta?.startsWith("meta/") == true -> "Meta"
        else -> "EDUAPP"
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        gesturesEnabled = esPrincipal,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "EDUAPP",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(24.dp)
                )
                PRINCIPALES.forEach { d ->
                    NavigationDrawerItem(
                        label = { Text(d.titulo) },
                        selected = ruta == d.ruta,
                        onClick = {
                            scope.launch { drawer.close() }
                            nav.navigate(d.ruta) {
                                popUpTo("dashboard")
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(titulo) },
                    navigationIcon = {
                        if (esPrincipal) {
                            IconButton(onClick = { scope.launch { drawer.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menú")
                            }
                        } else {
                            IconButton(onClick = { nav.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            snackbarHost = { SnackbarHost(snack) }
        ) { pad ->
            NavHost(nav, startDestination = "dashboard", modifier = Modifier.padding(pad)) {
                composable("dashboard") {
                    DashboardScreen(
                        state,
                        irA = { r -> nav.navigate(r) { popUpTo("dashboard"); launchSingleTop = true } },
                        abrirActividad = { nav.navigate("actividad/${it.materiaId}/${it.id}") }
                    )
                }
                composable("materias") {
                    MateriasScreen(state, vm) { nav.navigate("materia/$it") }
                }
                composable(
                    "materia/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { e ->
                    MateriaDetalleScreen(
                        state, e.arguments!!.getLong("id"),
                        nuevaActividad = { nav.navigate("actividad/$it/0") },
                        abrirActividad = { nav.navigate("actividad/${it.materiaId}/${it.id}") }
                    )
                }
                composable(
                    "actividad/{materiaId}/{actividadId}",
                    arguments = listOf(
                        navArgument("materiaId") { type = NavType.LongType },
                        navArgument("actividadId") { type = NavType.LongType }
                    )
                ) { e ->
                    ActividadScreen(
                        state, vm,
                        e.arguments!!.getLong("materiaId"),
                        e.arguments!!.getLong("actividadId")
                    ) { nav.popBackStack() }
                }
                composable("calendario") {
                    CalendarioScreen(state) { nav.navigate("actividad/${it.materiaId}/${it.id}") }
                }
                composable("metas") {
                    MetasScreen(state) { nav.navigate("meta/$it") }
                }
                composable(
                    "meta/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) { e ->
                    MetaScreen(state, vm, e.arguments!!.getLong("id")) { nav.popBackStack() }
                }
                composable("estadisticas") { EstadisticasScreen(state) }
                composable("pomodoro") { PomodoroScreen(state, vm) }
                composable("perfil") { PerfilScreen(state, vm) }
            }
        }
    }
}
