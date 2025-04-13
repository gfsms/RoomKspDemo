package com.example.roomkspdemo.data.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.example.roomkspdemo.data.entities.CAEX
import com.example.roomkspdemo.data.entities.Inspeccion

/**
 * Clase de relación que combina una Inspección con su CAEX asociado.
 *
 * Esta clase se utiliza para obtener información completa de una inspección,
 * incluyendo los detalles del equipo inspeccionado, en una sola consulta.
 */
data class InspeccionConCAEX(
    @Embedded val inspeccion: Inspeccion,

    @Relation(
        parentColumn = "caexId",
        entityColumn = "caexId"
    )
    val caex: CAEX
) {
    /**
     * Devuelve un título descriptivo para la inspección.
     *
     * @return Un string con información de la inspección y el CAEX
     */
    fun getTituloDescriptivo(): String {
        val tipoInspeccion = if (inspeccion.tipo == Inspeccion.TIPO_RECEPCION) "Recepción" else "Entrega"
        return "Inspección de $tipoInspeccion - ${caex.getNombreCompleto()}"
    }

    /**
     * Devuelve una descripción del estado de la inspección.
     *
     * @return Un string con el estado y la fecha de la inspección
     */
    fun getEstadoDescriptivo(): String {
        val estado = if (inspeccion.estado == Inspeccion.ESTADO_ABIERTA) "Abierta" else "Cerrada"
        return "Estado: $estado"
    }
}