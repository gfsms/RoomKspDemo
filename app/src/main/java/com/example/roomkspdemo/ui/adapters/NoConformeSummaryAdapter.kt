package com.example.roomkspdemo.ui.adapters

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.roomkspdemo.R
import com.example.roomkspdemo.data.entities.Respuesta
import com.example.roomkspdemo.data.relations.RespuestaConDetalles
import com.example.roomkspdemo.databinding.ItemNoConformeSummaryBinding
import com.example.roomkspdemo.ui.viewmodels.RespuestaViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Adaptador para mostrar y gestionar los elementos No Conformes en la pantalla de resumen.
 *
 * @param respuestaViewModel ViewModel para gestionar las respuestas
 * @param onViewPhotos Callback para cuando se quiere ver las fotos
 * @param onDataChanged Callback para notificar cambios en los datos
 */
class NoConformeSummaryAdapter(
    private val respuestaViewModel: RespuestaViewModel,
    private val onViewPhotos: (Long) -> Unit,
    private val onDataChanged: () -> Unit
) : ListAdapter<RespuestaConDetalles, NoConformeSummaryAdapter.NoConformeViewHolder>(NoConformeDiffCallback()) {

    // Mapa para almacenar los estados UI actuales
    private val itemStates = mutableMapOf<Long, NoConformeItemState>()

    // Clase para mantener el estado UI de cada elemento
    data class NoConformeItemState(
        var respuestaId: Long,
        var comentarios: String = "",
        var tipoAccion: String = "",
        var idAvisoOrdenTrabajo: String = "",
        var numFotos: Int = 0,
        var isComplete: Boolean = false
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoConformeViewHolder {
        val binding = ItemNoConformeSummaryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoConformeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoConformeViewHolder, position: Int) {
        val respuestaConDetalles = getItem(position)
        holder.bind(respuestaConDetalles)
    }

    /**
     * Verifica si todos los elementos tienen los campos requeridos completos.
     *
     * @return true si todos los elementos están completos, false caso contrario
     */
    fun areAllItemsComplete(): Boolean {
        // Si no hay items, considerarlo como completo
        if (currentList.isEmpty()) return true

        // Verificar que todos los items tengan su estado completo
        return itemStates.values.all { it.isComplete }
    }

    /**
     * Obtiene la lista actual de respuestas con sus estados actualizados.
     *
     * @return Lista de respuestas con detalles
     */
    fun getCurrentItems(): List<RespuestaConDetalles> {
        return currentList
    }

    /**
     * Guarda todos los cambios pendientes y ejecuta la callback proporcionada al finalizar.
     *
     * @param onComplete Callback a ejecutar cuando todos los cambios se han guardado
     */
    fun saveAllChanges(onComplete: () -> Unit) {
        // Si no hay cambios que guardar
        if (itemStates.isEmpty()) {
            onComplete()
            return
        }

        // Contador para saber cuándo se han completado todas las operaciones
        var pendingOperations = itemStates.size
        var completedOperations = 0

        CoroutineScope(Dispatchers.Main).launch {
            itemStates.forEach { (respuestaId, state) ->
                // Solo guardar si están completos
                if (state.isComplete) {
                    try {
                        withContext(Dispatchers.IO) {
                            respuestaViewModel.actualizarRespuestaNoConforme(
                                respuestaId,
                                state.comentarios,
                                state.tipoAccion,
                                state.idAvisoOrdenTrabajo
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("NoConformeSummaryAdapter", "Error al guardar respuesta: ${e.message}")
                    }
                }

                // Incrementar contador de operaciones completadas
                completedOperations++

                // Si todas las operaciones se han completado, llamar a la callback
                if (completedOperations >= pendingOperations) {
                    onComplete()
                }
            }
        }
    }

    inner class NoConformeViewHolder(
        private val binding: ItemNoConformeSummaryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(respuestaConDetalles: RespuestaConDetalles) {
            val respuesta = respuestaConDetalles.respuesta
            val pregunta = respuestaConDetalles.pregunta

            // Obtener o crear el estado para este item
            val itemState = itemStates.getOrPut(respuesta.respuestaId) {
                NoConformeItemState(
                    respuestaId = respuesta.respuestaId,
                    comentarios = respuesta.comentarios,
                    tipoAccion = respuesta.tipoAccion ?: "",
                    idAvisoOrdenTrabajo = respuesta.idAvisoOrdenTrabajo ?: "",
                    numFotos = respuestaConDetalles.fotos.size
                )
            }

            // Actualizar número de fotos en el estado
            itemState.numFotos = respuestaConDetalles.fotos.size

            // Mostrar información de la categoría
            binding.categoriaTextView.text = "Categoría: ${getCategoriaName(pregunta.categoriaId)}"

            // Mostrar texto de la pregunta
            binding.preguntaTextView.text = pregunta.texto

            // Configurar campo de comentarios
            setupCommentField(itemState)

            // Configurar selector de tipo de acción
            setupActionTypeRadios(itemState)

            // Configurar campo de ID Aviso/OT
            setupIdField(itemState)

            // Configurar botón de fotos
            setupPhotosButton(itemState)

            // Verificar si todos los campos están completos
            updateItemCompletionState(itemState)
        }

        private fun setupCommentField(itemState: NoConformeItemState) {
            // Quitar listener previo
            val commentEditText = binding.commentEditText

            // Establecer el valor actual
            commentEditText.setText(itemState.comentarios)

            // Agregar nuevo listener
            commentEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    itemState.comentarios = s.toString()
                    updateItemCompletionState(itemState)
                }
            })
        }

        private fun setupActionTypeRadios(itemState: NoConformeItemState) {
            // Quitar listener previo
            binding.tipoAccionRadioGroup.setOnCheckedChangeListener(null)

            // Establecer el valor actual
            when (itemState.tipoAccion) {
                Respuesta.ACCION_INMEDIATO -> binding.inmediatoRadio.isChecked = true
                Respuesta.ACCION_PROGRAMADO -> binding.programadoRadio.isChecked = true
                else -> {
                    binding.inmediatoRadio.isChecked = false
                    binding.programadoRadio.isChecked = false
                }
            }

            // Agregar nuevo listener
            binding.tipoAccionRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                itemState.tipoAccion = when (checkedId) {
                    R.id.inmediatoRadio -> Respuesta.ACCION_INMEDIATO
                    R.id.programadoRadio -> Respuesta.ACCION_PROGRAMADO
                    else -> ""
                }
                updateItemCompletionState(itemState)
            }
        }

        private fun setupIdField(itemState: NoConformeItemState) {
            // Quitar listener previo
            val idEditText = binding.avisoOtEditText

            // Establecer el valor actual
            idEditText.setText(itemState.idAvisoOrdenTrabajo)

            // Agregar nuevo listener
            idEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    itemState.idAvisoOrdenTrabajo = s.toString()
                    updateItemCompletionState(itemState)
                }
            })
        }

        private fun setupPhotosButton(itemState: NoConformeItemState) {
            // Actualizar texto del botón con el número de fotos
            binding.viewPhotosButton.text = "Ver Fotos (${itemState.numFotos})"

            // Habilitar o deshabilitar según haya fotos
            binding.viewPhotosButton.isEnabled = itemState.numFotos > 0

            // Configurar listener
            binding.viewPhotosButton.setOnClickListener {
                onViewPhotos(itemState.respuestaId)
            }
        }

        private fun updateItemCompletionState(itemState: NoConformeItemState) {
            // Verificar que todos los campos obligatorios estén completos
            val isComplete = itemState.comentarios.isNotBlank() &&
                    itemState.tipoAccion.isNotBlank() &&
                    itemState.idAvisoOrdenTrabajo.isNotBlank()

            // Actualizar estado
            itemState.isComplete = isComplete

            // Notificar cambios
            onDataChanged()
        }

        private fun getCategoriaName(categoriaId: Long): String {
            // TODO: Implementar un método para obtener el nombre real de la categoría
            // Por ahora devolver un valor genérico
            return "Categoría $categoriaId"
        }
    }

    /**
     * DiffCallback para comparar elementos de forma eficiente.
     */
    class NoConformeDiffCallback : DiffUtil.ItemCallback<RespuestaConDetalles>() {
        override fun areItemsTheSame(oldItem: RespuestaConDetalles, newItem: RespuestaConDetalles): Boolean {
            return oldItem.respuesta.respuestaId == newItem.respuesta.respuestaId
        }

        override fun areContentsTheSame(oldItem: RespuestaConDetalles, newItem: RespuestaConDetalles): Boolean {
            return oldItem.respuesta == newItem.respuesta &&
                    oldItem.pregunta == newItem.pregunta &&
                    oldItem.fotos.size == newItem.fotos.size
        }
    }
}