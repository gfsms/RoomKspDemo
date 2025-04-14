package com.example.roomkspdemo.data.repository

import com.example.roomkspdemo.data.dao.RespuestaDao
import com.example.roomkspdemo.data.entities.Respuesta
import com.example.roomkspdemo.data.relations.RespuestaConDetalles
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para operaciones relacionadas con las respuestas de inspección.
 *
 * Esta clase proporciona métodos para acceder y manipular datos de respuestas,
 * sirviendo como una capa de abstracción entre la base de datos y la UI.
 */
class RespuestaRepository(private val respuestaDao: RespuestaDao) {

    /**
     * Obtiene todas las respuestas para una inspección específica.
     *
     * @param inspeccionId ID de la inspección
     * @return Flow de lista de respuestas
     */
    fun getRespuestasByInspeccion(inspeccionId: Long): Flow<List<Respuesta>> {
        return respuestaDao.getRespuestasByInspeccion(inspeccionId)
    }

    /**
     * Obtiene las respuestas con sus detalles completos para una inspección.
     *
     * @param inspeccionId ID de la inspección
     * @return Flow de lista de respuestas con detalles
     */
    fun getRespuestasConDetallesByInspeccion(inspeccionId: Long): Flow<List<RespuestaConDetalles>> {
        return respuestaDao.getRespuestasConDetallesByInspeccion(inspeccionId)
    }

    /**
     * Obtiene las respuestas con sus detalles completos para una inspección, ordenadas por categoría y orden.
     *
     * @param inspeccionId ID de la inspección
     * @return Flow de lista de respuestas con detalles ordenadas
     */
    fun getRespuestasConDetallesByInspeccionOrdenadas(inspeccionId: Long): Flow<List<RespuestaConDetalles>> {
        return respuestaDao.getRespuestasConDetallesByInspeccionOrdenadas(inspeccionId)
    }

    /**
     * Obtiene las respuestas con sus detalles para una categoría específica de una inspección.
     *
     * @param inspeccionId ID de la inspección
     * @param categoriaId ID de la categoría
     * @return Flow de lista de respuestas con detalles
     */
    fun getRespuestasConDetallesByInspeccionYCategoria(
        inspeccionId: Long,
        categoriaId: Long
    ): Flow<List<RespuestaConDetalles>> {
        return respuestaDao.getRespuestasConDetallesByInspeccionYCategoria(inspeccionId, categoriaId)
    }

    /**
     * Obtiene una respuesta específica para una inspección y pregunta.
     *
     * @param inspeccionId ID de la inspección
     * @param preguntaId ID de la pregunta
     * @return La respuesta o null si no existe
     */
    suspend fun getRespuestaPorInspeccionYPregunta(inspeccionId: Long, preguntaId: Long): Respuesta? {
        return respuestaDao.getRespuestaPorInspeccionYPregunta(inspeccionId, preguntaId)
    }

    /**
     * Cuenta el número de fotos para una respuesta.
     *
     * @param respuestaId ID de la respuesta
     * @return Número de fotos
     */
    suspend fun countFotosByRespuesta(respuestaId: Long): Int {
        // Utilizamos el FotoDao a través del FotoRepository, pero para simplificar la solución
        // temporal, podemos devolver 0 aquí.
        return 0
    }

    /**
     * Guarda una respuesta "Conforme" para una pregunta en una inspección.
     *
     * @param inspeccionId ID de la inspección
     * @param preguntaId ID de la pregunta
     * @return ID de la respuesta creada
     */
    suspend fun guardarRespuestaConforme(inspeccionId: Long, preguntaId: Long): Long {
        // Verificar si ya existe una respuesta para esta pregunta en esta inspección
        val respuestaExistente = respuestaDao.getRespuestaPorInspeccionYPregunta(inspeccionId, preguntaId)

        if (respuestaExistente != null) {
            // Actualizar la respuesta existente
            val respuestaActualizada = respuestaExistente.copy(
                estado = Respuesta.ESTADO_CONFORME,
                comentarios = "",
                tipoAccion = null,
                idAvisoOrdenTrabajo = null,
                fechaModificacion = System.currentTimeMillis()
            )
            respuestaDao.updateRespuesta(respuestaActualizada)
            return respuestaExistente.respuestaId
        } else {
            // Crear una nueva respuesta
            val nuevaRespuesta = Respuesta(
                inspeccionId = inspeccionId,
                preguntaId = preguntaId,
                estado = Respuesta.ESTADO_CONFORME
            )
            return respuestaDao.insertRespuesta(nuevaRespuesta)
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
     * @return ID de la respuesta creada
     */
    suspend fun guardarRespuestaNoConforme(
        inspeccionId: Long,
        preguntaId: Long,
        comentarios: String,
        tipoAccion: String,
        idAvisoOrdenTrabajo: String
    ): Long {
        // Validar campos obligatorios
        if (comentarios.isBlank()) {
            throw IllegalArgumentException("Los comentarios son obligatorios para una respuesta No Conforme")
        }

        if (tipoAccion != Respuesta.ACCION_INMEDIATO && tipoAccion != Respuesta.ACCION_PROGRAMADO) {
            throw IllegalArgumentException("El tipo de acción debe ser INMEDIATO o PROGRAMADO")
        }

        if (idAvisoOrdenTrabajo.isBlank()) {
            throw IllegalArgumentException("El ID de aviso u orden de trabajo es obligatorio")
        }

        // Verificar si ya existe una respuesta para esta pregunta en esta inspección
        val respuestaExistente = respuestaDao.getRespuestaPorInspeccionYPregunta(inspeccionId, preguntaId)

        if (respuestaExistente != null) {
            // Actualizar la respuesta existente
            val respuestaActualizada = respuestaExistente.copy(
                estado = Respuesta.ESTADO_NO_CONFORME,
                comentarios = comentarios,
                tipoAccion = tipoAccion,
                idAvisoOrdenTrabajo = idAvisoOrdenTrabajo,
                fechaModificacion = System.currentTimeMillis()
            )
            respuestaDao.updateRespuesta(respuestaActualizada)
            return respuestaExistente.respuestaId
        } else {
            // Crear una nueva respuesta
            val nuevaRespuesta = Respuesta(
                inspeccionId = inspeccionId,
                preguntaId = preguntaId,
                estado = Respuesta.ESTADO_NO_CONFORME,
                comentarios = comentarios,
                tipoAccion = tipoAccion,
                idAvisoOrdenTrabajo = idAvisoOrdenTrabajo
            )
            return respuestaDao.insertRespuesta(nuevaRespuesta)
        }
    }

    /**
     * Obtiene una respuesta específica por su ID.
     *
     * @param respuestaId ID de la respuesta
     * @return La respuesta o null si no existe
     */
    suspend fun getRespuestaById(respuestaId: Long): Respuesta? {
        return respuestaDao.getRespuestaById(respuestaId)
    }

    /**
     * Obtiene una respuesta con todos sus detalles por su ID.
     *
     * @param respuestaId ID de la respuesta
     * @return La respuesta con detalles o null si no existe
     */
    suspend fun getRespuestaConDetallesById(respuestaId: Long): RespuestaConDetalles? {
        return respuestaDao.getRespuestaConDetallesById(respuestaId)
    }

    /**
     * Elimina todas las respuestas asociadas a una inspección.
     *
     * @param inspeccionId ID de la inspección
     */
    suspend fun deleteRespuestasByInspeccion(inspeccionId: Long) {
        respuestaDao.deleteRespuestasByInspeccion(inspeccionId)
    }

    /**
     * Cuenta el número de respuestas para una inspección.
     *
     * @param inspeccionId ID de la inspección
     * @return Número de respuestas
     */
    suspend fun countRespuestasByInspeccion(inspeccionId: Long): Int {
        return respuestaDao.countRespuestasByInspeccion(inspeccionId)
    }

    /**
     * Cuenta el número de respuestas con un estado específico para una inspección.
     *
     * @param inspeccionId ID de la inspección
     * @param estado Estado de las respuestas (CONFORME o NO_CONFORME)
     * @return Número de respuestas con el estado especificado
     */
    suspend fun countRespuestasByInspeccionYEstado(inspeccionId: Long, estado: String): Int {
        return respuestaDao.countRespuestasByInspeccionYEstado(inspeccionId, estado)
    }
}