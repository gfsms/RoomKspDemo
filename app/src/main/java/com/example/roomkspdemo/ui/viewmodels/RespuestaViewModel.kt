package com.example.roomkspdemo.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.roomkspdemo.data.entities.Respuesta
import com.example.roomkspdemo.data.relations.RespuestaConDetalles
import com.example.roomkspdemo.data.repository.RespuestaRepository
import kotlinx.coroutines.launch

/**
 * ViewModel para la gestión de respuestas de inspección.
 *
 * Esta clase expone datos observables y proporciona métodos para interactuar
 * con los datos de respuestas de manera coherente con el ciclo de vida de la UI.
 */
class RespuestaViewModel(private val repository: RespuestaRepository) : ViewModel() {

    // LiveData para notificar eventos de operaciones
    private val _operationStatus = MutableLiveData<OperationStatus>()
    val operationStatus: LiveData<OperationStatus> = _operationStatus

    // LiveData para almacenar la respuesta actual con sus detalles
    private val _respuestaActual = MutableLiveData<RespuestaConDetalles>()
    val respuestaActual: LiveData<RespuestaConDetalles> = _respuestaActual

    /**
     * Obtiene todas las respuestas para una inspección específica.
     *
     * @param inspeccionId ID de la inspección
     * @return LiveData con la lista de respuestas
     */
    fun getRespuestasByInspeccion(inspeccionId: Long): LiveData<List<Respuesta>> {
        return repository.getRespuestasByInspeccion(inspeccionId).asLiveData()
    }

    /**
     * Obtiene todas las respuestas con sus detalles para una inspección específica.
     *
     * @param inspeccionId ID de la inspección
     * @return LiveData con la lista de respuestas con detalles
     */
    fun getRespuestasConDetallesByInspeccion(inspeccionId: Long): LiveData<List<RespuestaConDetalles>> {
        return repository.getRespuestasConDetallesByInspeccion(inspeccionId).asLiveData()
    }

    /**
     * Obtiene todas las respuestas con sus detalles para una inspección, ordenadas por categoría y orden.
     *
     * @param inspeccionId ID de la inspección
     * @return LiveData con la lista de respuestas con detalles ordenadas
     */
    fun getRespuestasConDetallesByInspeccionOrdenadas(inspeccionId: Long): LiveData<List<RespuestaConDetalles>> {
        return repository.getRespuestasConDetallesByInspeccionOrdenadas(inspeccionId).asLiveData()
    }

    /**
     * Obtiene las respuestas con sus detalles para una categoría específica de una inspección.
     *
     * @param inspeccionId ID de la inspección
     * @param categoriaId ID de la categoría
     * @return LiveData con la lista de respuestas con detalles
     */
    fun getRespuestasConDetallesByInspeccionYCategoria(
        inspeccionId: Long,
        categoriaId: Long
    ): LiveData<List<RespuestaConDetalles>> {
        return repository.getRespuestasConDetallesByInspeccionYCategoria(inspeccionId, categoriaId).asLiveData()
    }

    /**
     * Obtiene una respuesta específica para una inspección y pregunta.
     *
     * @param inspeccionId ID de la inspección
     * @param preguntaId ID de la pregunta
     * @return La respuesta o null si no existe
     */
    suspend fun getRespuestaPorInspeccionYPregunta(inspeccionId: Long, preguntaId: Long): Respuesta? {
        try {
            return repository.getRespuestaPorInspeccionYPregunta(inspeccionId, preguntaId)
        } catch (e: Exception) {
            _operationStatus.postValue(OperationStatus.Error(e.message ?: "Error al obtener respuesta"))
            return null
        }
    }

    /**
     * Cuenta el número de fotos para una respuesta.
     *
     * @param respuestaId ID de la respuesta
     * @return El número de fotos
     */
    suspend fun countFotosByRespuesta(respuestaId: Long): Int {
        try {
            return repository.countFotosByRespuesta(respuestaId)
        } catch (e: Exception) {
            _operationStatus.postValue(OperationStatus.Error(e.message ?: "Error al contar fotos"))
            return 0
        }
    }

    /**
     * Guarda una respuesta "Conforme" para una pregunta en una inspección.
     *
     * @param inspeccionId ID de la inspección
     * @param preguntaId ID de la pregunta
     * @return ID de la respuesta guardada
     */
    suspend fun guardarRespuestaConforme(inspeccionId: Long, preguntaId: Long): Long {
        try {
            return repository.guardarRespuestaConforme(inspeccionId, preguntaId)
        } catch (e: Exception) {
            _operationStatus.postValue(OperationStatus.Error(e.message ?: "Error al guardar respuesta conforme"))
            return 0
        }
    }

    /**
     * Guarda una respuesta "No Conforme" para una pregunta en una inspección.
     *
     * @param inspeccionId ID de la inspección
     * @param preguntaId ID de la pregunta
     * @param comentarios Comentarios explicativos sobre el problema
     * @param tipoAccion Tipo de acción (INMEDIATO o PROGRAMADO)
     * @param idAvisoOrdenTrabajo ID del aviso o la orden de trabajo asociada
     * @return ID de la respuesta guardada
     */
    suspend fun guardarRespuestaNoConforme(
        inspeccionId: Long,
        preguntaId: Long,
        comentarios: String,
        tipoAccion: String,
        idAvisoOrdenTrabajo: String
    ): Long {
        try {
            return repository.guardarRespuestaNoConforme(
                inspeccionId,
                preguntaId,
                comentarios,
                tipoAccion,
                idAvisoOrdenTrabajo
            )
        } catch (e: Exception) {
            _operationStatus.postValue(OperationStatus.Error(e.message ?: "Error al guardar respuesta no conforme"))
            return 0
        }
    }

    /**
     * Carga una respuesta con todos sus detalles por su ID.
     *
     * @param respuestaId ID de la respuesta
     */
    fun loadRespuestaConDetalles(respuestaId: Long) = viewModelScope.launch {
        try {
            val respuesta = repository.getRespuestaConDetallesById(respuestaId)
            if (respuesta != null) {
                _respuestaActual.value = respuesta
            } else {
                _operationStatus.value = OperationStatus.Error("Respuesta no encontrada")
            }
        } catch (e: Exception) {
            _operationStatus.value = OperationStatus.Error(e.message ?: "Error desconocido")
        }
    }

    /**
     * Cuenta el número de respuestas para una inspección.
     *
     * @param inspeccionId ID de la inspección
     * @return El número de respuestas (en un LiveData)
     */
    fun countRespuestasByInspeccion(inspeccionId: Long): LiveData<Int> {
        val result = MutableLiveData<Int>()
        viewModelScope.launch {
            try {
                val count = repository.countRespuestasByInspeccion(inspeccionId)
                result.value = count
            } catch (e: Exception) {
                _operationStatus.value = OperationStatus.Error(e.message ?: "Error desconocido")
                result.value = 0
            }
        }
        return result
    }

    /**
     * Cuenta el número de respuestas no conformes para una inspección.
     *
     * @param inspeccionId ID de la inspección
     * @return El número de respuestas no conformes (en un LiveData)
     */
    fun countRespuestasNoConformes(inspeccionId: Long): LiveData<Int> {
        val result = MutableLiveData<Int>()
        viewModelScope.launch {
            try {
                val count = repository.countRespuestasByInspeccionYEstado(
                    inspeccionId,
                    Respuesta.ESTADO_NO_CONFORME
                )
                result.value = count
            } catch (e: Exception) {
                _operationStatus.value = OperationStatus.Error(e.message ?: "Error desconocido")
                result.value = 0
            }
        }
        return result
    }

    /**
     * Clase sellada para representar el estado de las operaciones.
     */
    sealed class OperationStatus {
        data class Success(val message: String, val id: Long = 0) : OperationStatus()
        data class Error(val message: String) : OperationStatus()
    }

    /**
     * Factory para crear instancias de RespuestaViewModel con el repositorio correcto.
     */
    class RespuestaViewModelFactory(private val repository: RespuestaRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RespuestaViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return RespuestaViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}