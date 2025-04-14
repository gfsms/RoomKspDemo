package com.example.roomkspdemo.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.roomkspdemo.data.entities.CAEX
import com.example.roomkspdemo.data.entities.Inspeccion
import com.example.roomkspdemo.data.relations.InspeccionCompleta
import com.example.roomkspdemo.data.relations.InspeccionConCAEX
import com.example.roomkspdemo.data.repository.CAEXRepository
import com.example.roomkspdemo.data.repository.InspeccionRepository
import kotlinx.coroutines.launch

/**
 * ViewModel para la gestión de inspecciones.
 *
 * Esta clase expone datos observables y proporciona métodos para interactuar
 * con los datos de inspecciones de manera coherente con el ciclo de vida de la UI.
 */
class InspeccionViewModel(
    private val inspeccionRepository: InspeccionRepository,
    private val caexRepository: CAEXRepository? = null
) : ViewModel() {

    // LiveData para notificar eventos de operaciones
    private val _operationStatus = MutableLiveData<OperationStatus>()
    val operationStatus: LiveData<OperationStatus> = _operationStatus

    // LiveData con todas las inspecciones
    val allInspeccionesConCAEX: LiveData<List<InspeccionConCAEX>> =
        inspeccionRepository.allInspeccionesConCAEX.asLiveData()

    // LiveData con inspecciones abiertas
    val inspeccionesAbiertasConCAEX: LiveData<List<InspeccionConCAEX>> =
        inspeccionRepository.inspeccionesAbiertasConCAEX.asLiveData()

    // LiveData con inspecciones cerradas
    val inspeccionesCerradasConCAEX: LiveData<List<InspeccionConCAEX>> =
        inspeccionRepository.inspeccionesCerradasConCAEX.asLiveData()

    // LiveData con inspecciones de recepción abiertas
    val inspeccionesRecepcionAbiertasConCAEX: LiveData<List<InspeccionConCAEX>> =
        inspeccionRepository.inspeccionesRecepcionAbiertasConCAEX.asLiveData()

    /**
     * Busca un CAEX por su número identificador y modelo, lo crea si no existe,
     * y luego crea una inspección de recepción para ese CAEX.
     */
    fun buscarCAEXPorNumeroYCrearInspeccion(
        numeroIdentificador: Int,
        modelo: String,
        nombreInspector: String,
        nombreSupervisor: String
    ) = viewModelScope.launch {
        try {
            if (caexRepository == null) {
                _operationStatus.value = OperationStatus.Error("No se puede realizar esta operación sin el CAEXRepository")
                return@launch
            }

            // Buscar el CAEX por número identificador
            var caex = caexRepository.getCAEXByNumeroIdentificador(numeroIdentificador)

            // Si no existe, crearlo
            if (caex == null) {
                val nuevoCAEX = CAEX(
                    numeroIdentificador = numeroIdentificador,
                    modelo = modelo
                )

                // Validar que el identificador sea válido para el modelo
                if (!nuevoCAEX.esIdentificadorValido()) {
                    throw IllegalArgumentException("El número identificador $numeroIdentificador no es válido para el modelo $modelo")
                }

                // Insertar el nuevo CAEX y obtener su ID
                val caexId = caexRepository.insert(nuevoCAEX)
                caex = caexRepository.getCAEXById(caexId)
                    ?: throw IllegalStateException("Error al crear el CAEX")
            } else {
                // Verificar que el modelo coincida
                if (caex.modelo != modelo) {
                    throw IllegalArgumentException("El CAEX #$numeroIdentificador existe pero es de modelo ${caex.modelo}, no $modelo")
                }
            }

            // Crear la inspección de recepción
            val inspeccionId = inspeccionRepository.crearInspeccionRecepcion(
                caex.caexId,
                nombreInspector,
                nombreSupervisor
            )

            _operationStatus.value = OperationStatus.Success(
                "Inspección de recepción creada correctamente",
                inspeccionId
            )
        } catch (e: Exception) {
            _operationStatus.value = OperationStatus.Error(e.message ?: "Error desconocido")
        }
    }

    /**
     * Crea una nueva inspección de recepción.
     *
     * @param caexId ID del CAEX a inspeccionar
     * @param nombreInspector Nombre del inspector
     * @param nombreSupervisor Nombre del supervisor de taller
     */
    fun crearInspeccionRecepcion(
        caexId: Long,
        nombreInspector: String,
        nombreSupervisor: String
    ) = viewModelScope.launch {
        try {
            val inspeccionId = inspeccionRepository.crearInspeccionRecepcion(
                caexId,
                nombreInspector,
                nombreSupervisor
            )
            _operationStatus.value = OperationStatus.Success(
                "Inspección de recepción creada correctamente",
                inspeccionId
            )
        } catch (e: Exception) {
            _operationStatus.value = OperationStatus.Error(e.message ?: "Error desconocido")
        }
    }

    /**
     * Crea una nueva inspección de entrega basada en una inspección de recepción.
     *
     * @param inspeccionRecepcionId ID de la inspección de recepción
     * @param nombreInspector Nombre del inspector
     * @param nombreSupervisor Nombre del supervisor de taller
     */
    fun crearInspeccionEntrega(
        inspeccionRecepcionId: Long,
        nombreInspector: String,
        nombreSupervisor: String
    ) = viewModelScope.launch {
        try {
            val inspeccionId = inspeccionRepository.crearInspeccionEntrega(
                inspeccionRecepcionId,
                nombreInspector,
                nombreSupervisor
            )
            _operationStatus.value = OperationStatus.Success(
                "Inspección de entrega creada correctamente",
                inspeccionId
            )
        } catch (e: Exception) {
            _operationStatus.value = OperationStatus.Error(e.message ?: "Error desconocido")
        }
    }

    /**
     * Cierra una inspección.
     *
     * @param inspeccionId ID de la inspección a cerrar
     * @param comentariosGenerales Comentarios generales sobre la inspección
     */
    fun cerrarInspeccion(inspeccionId: Long, comentariosGenerales: String = "") = viewModelScope.launch {
        try {
            val result = inspeccionRepository.cerrarInspeccion(inspeccionId, comentariosGenerales)
            if (result) {
                _operationStatus.value = OperationStatus.Success(
                    "Inspección cerrada correctamente",
                    inspeccionId
                )
            } else {
                _operationStatus.value = OperationStatus.Error(
                    "No se puede cerrar la inspección. Asegúrate de responder todas las preguntas."
                )
            }
        } catch (e: Exception) {
            _operationStatus.value = OperationStatus.Error(e.message ?: "Error desconocido")
        }
    }

    /**
     * Obtiene las inspecciones para un modelo de CAEX, estado y tipo específicos.
     *
     * @param modeloCAEX Modelo del CAEX (797F o 798AC)
     * @param estado Estado de la inspección (ABIERTA o CERRADA)
     * @param tipo Tipo de inspección (RECEPCION o ENTREGA)
     * @return LiveData con la lista de inspecciones que cumplen los criterios
     */
    fun getInspeccionesByModeloEstadoYTipo(
        modeloCAEX: String,
        estado: String,
        tipo: String
    ): LiveData<List<InspeccionConCAEX>> {
        return inspeccionRepository.getInspeccionesByModeloEstadoYTipo(modeloCAEX, estado, tipo).asLiveData()
    }

    /**
     * Obtiene los detalles completos de una inspección.
     *
     * @param inspeccionId ID de la inspección
     */
    fun getInspeccionCompleta(inspeccionId: Long) = viewModelScope.launch {
        try {
            val inspeccion = inspeccionRepository.getInspeccionCompletaById(inspeccionId)
            if (inspeccion != null) {
                _inspeccionCompleta.value = inspeccion
            } else {
                _operationStatus.value = OperationStatus.Error("Inspección no encontrada")
            }
        } catch (e: Exception) {
            _operationStatus.value = OperationStatus.Error(e.message ?: "Error desconocido")
        }
    }

    // LiveData para almacenar la inspección completa actual
    private val _inspeccionCompleta = MutableLiveData<InspeccionCompleta>()
    val inspeccionCompleta: LiveData<InspeccionCompleta> = _inspeccionCompleta

    /**
     * Obtiene una inspección con CAEX por su ID.
     *
     * @param inspeccionId ID de la inspección
     * @return LiveData con la inspección o null si no existe
     */
    fun getInspeccionConCAEXById(inspeccionId: Long): LiveData<InspeccionConCAEX?> {
        val result = MutableLiveData<InspeccionConCAEX?>()
        viewModelScope.launch {
            try {
                val inspeccion = inspeccionRepository.getInspeccionConCAEXById(inspeccionId)
                result.value = inspeccion
            } catch (e: Exception) {
                _operationStatus.value = OperationStatus.Error(e.message ?: "Error desconocido")
                result.value = null
            }
        }
        return result
    }

    /**
     * Elimina una inspección.
     *
     * @param inspeccion La inspección a eliminar
     */
    fun deleteInspeccion(inspeccion: Inspeccion) = viewModelScope.launch {
        try {
            inspeccionRepository.delete(inspeccion)
            _operationStatus.value = OperationStatus.Success("Inspección eliminada correctamente")
        } catch (e: Exception) {
            _operationStatus.value = OperationStatus.Error(e.message ?: "Error desconocido")
        }
    }

    /**
     * Clase sellada para representar el estado de las operaciones.
     */
    sealed class OperationStatus {
        data class Success(val message: String, val id: Long = 0) : OperationStatus()
        data class Error(val message: String) : OperationStatus()
    }

    /**
     * Factory para crear instancias de InspeccionViewModel con el repositorio correcto.
     */
    class InspeccionViewModelFactory(
        private val inspeccionRepository: InspeccionRepository,
        private val caexRepository: CAEXRepository? = null
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(InspeccionViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return InspeccionViewModel(inspeccionRepository, caexRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}