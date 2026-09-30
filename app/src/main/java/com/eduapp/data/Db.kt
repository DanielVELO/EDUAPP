package com.eduapp.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Base de datos SQLite local (sección 36). Las claves foráneas usan ON DELETE CASCADE. */
class EduDb(context: Context) : SQLiteOpenHelper(context, "eduapp.db", null, 1) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE perfil (
                id INTEGER PRIMARY KEY,
                alias TEXT NOT NULL,
                fotografia TEXT,
                duracion_estudio INTEGER NOT NULL DEFAULT 25,
                duracion_descanso INTEGER NOT NULL DEFAULT 5,
                recordatorios_activados INTEGER NOT NULL DEFAULT 1,
                minutos_antelacion INTEGER NOT NULL DEFAULT 30
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE materia (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                descripcion TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE actividad (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                materia_id INTEGER NOT NULL REFERENCES materia(id) ON DELETE CASCADE,
                nombre TEXT NOT NULL,
                descripcion TEXT,
                fecha TEXT NOT NULL,
                porcentaje REAL NOT NULL CHECK (porcentaje > 0 AND porcentaje <= 100),
                estado TEXT NOT NULL CHECK (estado IN ('Pendiente','En progreso','Completada','Vencida')),
                calificacion REAL CHECK (calificacion IS NULL OR (calificacion >= 0 AND calificacion <= 5))
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_actividad_materia ON actividad(materia_id)")
        db.execSQL("CREATE INDEX idx_actividad_fecha ON actividad(fecha)")
        db.execSQL(
            """
            CREATE TABLE meta (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                descripcion TEXT,
                estado TEXT NOT NULL DEFAULT 'No iniciada'
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE parte_meta (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                meta_id INTEGER NOT NULL REFERENCES meta(id) ON DELETE CASCADE,
                nombre TEXT NOT NULL,
                porcentaje REAL NOT NULL CHECK (porcentaje > 0 AND porcentaje <= 100),
                completada INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_parte_meta ON parte_meta(meta_id)")
        db.execSQL("INSERT INTO perfil (id, alias) VALUES (1, 'Estudiante')")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Versión 1.0: sin migraciones.
    }
}
