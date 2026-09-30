package com.eduapp

import android.app.Application
import android.media.RingtoneManager
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eduapp.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import kotlin.math.ceil

data class UiState(
    val cargado: Boolean = false,
    val perfil: Perfil = Perfil(),
    val materias: List<Materia> = emptyList(),
    val actividades: List<Actividad> = emptyList(),
    val metas: List<Meta> = emptyList()
) {
    fun promedioMateria(materiaId: Long) =
        Calc.promedioMateria(actividades.filter { it.materiaId == materiaId })

    val promedioGeneral get() = Calc.promedioGeneral(materias, actividades)
}

enum class ModoPomodoro { ESTUDIO, DESCANSO }
enum class FasePomodoro { REPOSO, CORRIENDO, PAUSADO }

data class PomodoroUi(
    val modo: ModoPomodoro = ModoPomodoro.ESTUDIO,
    val fase: FasePomodoro = FasePomodoro.REPOSO,
    val restanteSeg: Int = 0,
    val terminado: Boolean = false
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repo(app)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val mensajes: SharedFlow<String> = _mensajes.asSharedFlow()

    private val notificadas = mutableSetOf<String>()

    init {
        viewModelScope.launch(Dispatchers.IO) { cargar() }
    }

    /** Relee todo desde SQLite: garantiza que los promedios usan siempre datos actualizados (sección 18). */
    private fun cargar() {
        repo.refrescarVencidas(LocalDateTime.now())
        _state.value = UiState(
            cargado = true,
            perfil = repo.perfil(),
            materias = repo.materias(),
            actividades = repo.actividades(),
            metas = repo.metas()
        )
    }

    private fun ejecutar(exito: String? = null, alTerminar: () -> Unit = {}, accion: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                accion()
                cargar()
                if (exito != null) _mensajes.emit(exito)
                withContext(Dispatchers.Main) { alTerminar() }
            } catch (e: ValidationException) {
                _mensajes.emit(e.message ?: "Datos inválidos.")
            } catch (e: OperationException) {
                _mensajes.emit(e.message ?: "La operación no pudo completarse.")
            } catch (e: Exception) {
                _mensajes.emit("Ocurrió un error inesperado. No se guardó ningún cambio.")
            }
        }
    }

    fun mostrarMensaje(texto: String) {
        _mensajes.tryEmit(texto)
    }

    // ---- perfil ----
    fun guardarPerfil(alias: String, recordatorios: Boolean, minutos: Int?) =
        ejecutar("Cambios guardados.") { repo.guardarPerfil(alias, recordatorios, minutos) }

    fun cambiarFoto(uri: Uri) = ejecutar("Fotografía actualizada.") {
        val rel = ImageStore.importar(getApplication<Application>(), uri, _state.value.perfil.fotografia)
        repo.guardarFoto(rel)
    }

    // ---- materias ----
    fun guardarMateria(id: Long, nombre: String, descripcion: String) =
        ejecutar("Materia guardada.") { repo.guardarMateria(id, nombre, descripcion) }

    fun eliminarMateria(id: Long) =
        ejecutar("Materia eliminada junto con sus actividades y calificaciones.") { repo.eliminarMateria(id) }

    // ---- actividades ----
    fun guardarActividad(
        id: Long, materiaId: Long, nombre: String, descripcion: String, fecha: LocalDateTime?,
        porcentaje: Double?, estado: Estado, nota: Double?, alTerminar: () -> Unit
    ) = ejecutar("Actividad guardada.", alTerminar) {
        repo.guardarActividad(id, materiaId, nombre, descripcion, fecha, porcentaje, estado, nota, LocalDateTime.now())
    }

    fun eliminarCalificacion(id: Long, alTerminar: () -> Unit) =
        ejecutar("Calificación eliminada. La actividad permanece sin nota.", alTerminar) { repo.eliminarCalificacion(id) }

    fun eliminarActividad(id: Long, alTerminar: () -> Unit) =
        ejecutar("Actividad eliminada.", alTerminar) { repo.eliminarActividad(id) }

    // ---- metas ----
    fun guardarMeta(id: Long, nombre: String, descripcion: String, partes: List<ParteInput>, alTerminar: () -> Unit) =
        ejecutar("Meta guardada.", alTerminar) { repo.guardarMeta(id, nombre, descripcion, partes) }

    fun marcarParte(parteId: Long, completada: Boolean) =
        ejecutar { repo.marcarParte(parteId, completada) }

    fun eliminarMeta(id: Long, alTerminar: () -> Unit) =
        ejecutar("Meta eliminada.", alTerminar) { repo.eliminarMeta(id) }

    // ---- recordatorios (opción "a": solo con la app abierta) ----
    fun tick() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ahora = LocalDateTime.now()
                if (repo.refrescarVencidas(ahora) > 0) cargar()
                val perfil = repo.perfil()
                if (!perfil.recordatoriosActivados) return@launch
                val limite = ahora.plusMinutes(perfil.minutosAntelacion.toLong())
                repo.actividades()
                    .filter { (it.estado == Estado.PENDIENTE || it.estado == Estado.EN_PROGRESO) }
                    .filter { !it.fecha.isBefore(ahora) && !it.fecha.isAfter(limite) }
                    .forEach { a ->
                        if (notificadas.add("${a.id}|${a.fecha}")) Notifier.mostrar(getApplication<Application>(), a)
                    }
            } catch (_: Exception) {
            }
        }
    }

    // ---- Pomodoro (la sesión NO sobrevive al cierre de la app, sección 29) ----
    private val _pomo = MutableStateFlow(PomodoroUi())
    val pomodoro: StateFlow<PomodoroUi> = _pomo.asStateFlow()
    private var pomoJob: Job? = null

    private fun totalSeg(): Int {
        val p = _state.value.perfil
        return (if (_pomo.value.modo == ModoPomodoro.ESTUDIO) p.duracionEstudio else p.duracionDescanso) * 60
    }

    fun pomoModo(modo: ModoPomodoro) {
        if (_pomo.value.fase == FasePomodoro.REPOSO) _pomo.update { it.copy(modo = modo) }
    }

    fun pomoIniciar() {
        _pomo.update { it.copy(fase = FasePomodoro.CORRIENDO, restanteSeg = totalSeg(), terminado = false) }
        correr()
    }

    fun pomoPausar() {
        pomoJob?.cancel()
        _pomo.update { it.copy(fase = FasePomodoro.PAUSADO) }
    }

    fun pomoReanudar() {
        _pomo.update { it.copy(fase = FasePomodoro.CORRIENDO) }
        correr()
    }

    fun pomoFinalizar() {
        pomoJob?.cancel()
        _pomo.update { it.copy(fase = FasePomodoro.REPOSO, restanteSeg = 0, terminado = false) }
    }

    fun pomoReiniciarDesdeDialogo() = pomoIniciar()

    private fun correr() {
        pomoJob?.cancel()
        pomoJob = viewModelScope.launch {
            val fin = SystemClock.elapsedRealtime() + _pomo.value.restanteSeg * 1000L
            while (true) {
                val rest = ceil((fin - SystemClock.elapsedRealtime()) / 1000.0).toInt().coerceAtLeast(0)
                _pomo.update { it.copy(restanteSeg = rest) }
                if (rest <= 0) break
                delay(250)
            }
            _pomo.update { it.copy(fase = FasePomodoro.REPOSO, restanteSeg = 0, terminado = true) }
            sonar()
        }
    }

    private fun sonar() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(getApplication<Application>(), uri)?.play()
        } catch (_: Exception) {
        }
    }

    fun guardarPomodoro(estudio: Int?, descanso: Int?) =
        ejecutar("Configuración del Pomodoro guardada.") { repo.guardarPomodoro(estudio, descanso) }
}
