# EDUAPP 1.0 (Android)

Aplicación de gestión académica: Kotlin + Jetpack Compose + SQLite (SQLiteOpenHelper). Local, sin Internet.

## Cómo abrirla
1. Abre **Android Studio** (Koala / 2024.1 o más nuevo) → *Open* → selecciona esta carpeta `EDUAPP`.
2. Espera el *Gradle Sync* (necesita Internet solo la primera vez). Si pide actualizar el plugin de Android (AGP), acepta.
3. Conecta un teléfono con *Depuración USB* o crea un emulador (Android 8.0+) y pulsa ▶ *Run*.
4. Para el APK: *Build → Build Bundle(s) / APK(s) → Build APK(s)*.
5. Pruebas de cálculo: clic derecho en `app/src/test` → *Run Tests*.

## Estructura
- `data/` → `Db.kt` (esquema SQLite), `Repo.kt` (reglas de negocio + transacciones), `Calc.kt` (promedios), `ImageStore.kt`.
- `AppViewModel.kt` → estado, Pomodoro, recordatorios. `Notifier.kt` → notificaciones.
- `ui/` → una pantalla por archivo (PANT-01 a PANT-10).
