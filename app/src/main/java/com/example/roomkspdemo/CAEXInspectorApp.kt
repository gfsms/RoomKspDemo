package com.example.roomkspdemo

import android.app.Application
import com.example.roomkspdemo.data.AppDatabase
import com.example.roomkspdemo.data.repository.CAEXRepository
import com.example.roomkspdemo.data.repository.CategoriaRepository
import com.example.roomkspdemo.data.repository.FotoRepository
import com.example.roomkspdemo.data.repository.InspeccionRepository
import com.example.roomkspdemo.data.repository.PreguntaRepository
import com.example.roomkspdemo.data.repository.RespuestaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Clase de aplicación personalizada para la app de inspecciones de CAEX.
 *
 * Esta clase proporciona acceso centralizado a los componentes de la aplicación,
 * como la base de datos y los repositorios, y se encarga de su inicialización.
 */
class CAEXInspectionApp : Application() {

    // Scope de la aplicación que se cancelará cuando se cierre la app
    private val applicationScope = CoroutineScope(SupervisorJob())

    // La base de datos se inicializa de forma perezosa
    val database by lazy { AppDatabase.getDatabase(this) }

    // Repositorios para acceder a los datos
    val caexRepository by lazy { CAEXRepository(database.caexDao()) }

    val categoriaRepository by lazy { CategoriaRepository(database.categoriaDao()) }

    val preguntaRepository by lazy { PreguntaRepository(database.preguntaDao()) }

    val respuestaRepository by lazy { RespuestaRepository(database.respuestaDao()) }

    val fotoRepository by lazy { FotoRepository(database.fotoDao(), applicationContext) }

    val inspeccionRepository by lazy {
        InspeccionRepository(
            database.inspeccionDao(),
            database.respuestaDao(),
            database.preguntaDao(),
            database.caexDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        // Aquí puedes inicializar otros componentes globales si es necesario
    }
}