package com.example.roomkspdemo.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.roomkspdemo.R
import com.example.roomkspdemo.data.entities.Pregunta
import com.example.roomkspdemo.data.entities.Respuesta
import com.example.roomkspdemo.databinding.ItemQuestionBinding
import com.example.roomkspdemo.ui.viewmodels.RespuestaViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Adaptador para mostrar preguntas de inspección en un RecyclerView.
 *
 * @param inspeccionId ID de la inspección actual
 * @param respuestaViewModel ViewModel para gestionar las respuestas
 * @param onTakePicture Callback cuando se quiere tomar una foto
 * @param onViewPictures Callback cuando se quieren ver las fotos de una respuesta
 */
class QuestionAdapter(
    private val inspeccionId: Long,
    private val respuestaViewModel: RespuestaViewModel,
    private val onTakePicture: (Long) -> Unit,
    private val onViewPictures: (Long) -> Unit
) : ListAdapter<Pregunta, QuestionAdapter.QuestionViewHolder>(QuestionDiffCallback()) {

    // Mapa para almacenar las respuestas actuales
    private val respuestasMap = mutableMapOf<Long, RespuestaUIState>()

    // Clase para mantener el estado UI de cada respuesta
    data class RespuestaUIState(
        var respuestaId: Long = 0,
        var estado: String = "",
        var comentarios: String = "",
        var tipoAccion: String = "",
        var idAvisoOrdenTrabajo: String = "",
        var numFotos: Int = 0,
        var respondida: Boolean = false
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val binding = ItemQuestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return QuestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        val pregunta = getItem(position)
        holder.bind(pregunta)
    }

    /**
     * Verifica si todas las preguntas han sido respondidas.
     *
     * @return true si todas las preguntas tienen respuesta, false en caso contrario
     */
    fun todasPreguntasRespondidas(): Boolean {
        // Si no hay preguntas, retornar verdadero
        if (currentList.isEmpty()) return true

        // Verificar que todas las preguntas tengan una respuesta
        return currentList.all { pregunta ->
            respuestasMap[pregunta.preguntaId]?.respondida == true
        }
    }

    /**
     * Carga las respuestas existentes para las preguntas actuales.
     */
    fun cargarRespuestasExistentes() {
        if (currentList.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            // Para cada pregunta en la lista actual
            currentList.forEach { pregunta ->
                // Cargar la respuesta existente para esta pregunta en esta inspección
                val respuesta = respuestaViewModel.getRespuestaPorInspeccionYPregunta(inspeccionId, pregunta.preguntaId)
                respuesta?.let {
                    // Obtener el número de fotos para esta respuesta
                    val numFotos = respuestaViewModel.countFotosByRespuesta(respuesta.respuestaId)

                    // Crear estado UI para esta respuesta
                    val uiState = RespuestaUIState(
                        respuestaId = respuesta.respuestaId,
                        estado = respuesta.estado,
                        comentarios = respuesta.comentarios,
                        tipoAccion = respuesta.tipoAccion ?: "",
                        idAvisoOrdenTrabajo = respuesta.idAvisoOrdenTrabajo ?: "",
                        numFotos = numFotos,
                        respondida = true
                    )

                    // Actualizar el mapa de respuestas
                    respuestasMap[pregunta.preguntaId] = uiState

                    // Notificar el cambio en el UI thread
                    withContext(Dispatchers.Main) {
                        notifyItemChanged(currentList.indexOf(pregunta))
                    }
                }
            }
        }
    }

    inner class QuestionViewHolder(
        private val binding: ItemQuestionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(pregunta: Pregunta) {
            // Mostrar texto de la pregunta
            binding.questionTextView.text = pregunta.texto

            // Obtener el estado actual de la respuesta para esta pregunta
            val respuestaUIState = respuestasMap.getOrPut(pregunta.preguntaId) {
                RespuestaUIState()
            }

            // Configurar estado de los radio buttons
            setupRadioButtons(pregunta, respuestaUIState)

            // Configurar campos para No Conforme
            setupNoConformeFields(pregunta, respuestaUIState)

            // Configurar conteo de fotos
            updateFotoCount(respuestaUIState.numFotos)

            // Configurar botones para fotos
            setupPhotoButtons(pregunta, respuestaUIState)
        }

        private fun setupRadioButtons(pregunta: Pregunta, respuestaUIState: RespuestaUIState) {
            // Quitar listener temporalmente para evitar llamadas circulares
            binding.answerRadioGroup.setOnCheckedChangeListener(null)

            // Establecer estado según la respuesta
            when (respuestaUIState.estado) {
                Respuesta.ESTADO_CONFORME -> binding.conformeRadio.isChecked = true
                Respuesta.ESTADO_NO_CONFORME -> binding.noConformeRadio.isChecked = true
                else -> {
                    binding.conformeRadio.isChecked = false
                    binding.noConformeRadio.isChecked = false
                }
            }

            // Mostrar u ocultar área de No Conforme
            binding.noConformeContainer.visibility = if (respuestaUIState.estado == Respuesta.ESTADO_NO_CONFORME) {
                View.VISIBLE
            } else {
                View.GONE
            }

            // Configurar listener para cambios en la selección
            binding.answerRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.conformeRadio -> {
                        // Guardar respuesta Conforme
                        respuestaUIState.estado = Respuesta.ESTADO_CONFORME
                        respuestaUIState.respondida = true
                        binding.noConformeContainer.visibility = View.GONE

                        // Guardar en BD
                        guardarRespuestaConforme(pregunta.preguntaId)
                    }
                    R.id.noConformeRadio -> {
                        // Mostrar área No Conforme
                        respuestaUIState.estado = Respuesta.ESTADO_NO_CONFORME
                        respuestaUIState.respondida = validateNoConformeFields(respuestaUIState)
                        binding.noConformeContainer.visibility = View.VISIBLE

                        // Si ya tiene datos completos, guardar
                        if (respuestaUIState.respondida) {
                            guardarRespuestaNoConforme(
                                pregunta.preguntaId,
                                respuestaUIState.comentarios,
                                respuestaUIState.tipoAccion,
                                respuestaUIState.idAvisoOrdenTrabajo
                            )
                        }
                    }
                }
            }
        }

        private fun setupNoConformeFields(pregunta: Pregunta, respuestaUIState: RespuestaUIState) {
            // Configurar comentarios
            binding.commentEditText.setText(respuestaUIState.comentarios)
            binding.commentEditText.doAfterTextChanged { text ->
                respuestaUIState.comentarios = text.toString()
                respuestaUIState.respondida = validateNoConformeFields(respuestaUIState)

                // Si todos los campos están completos, guardar
                if (respuestaUIState.respondida && respuestaUIState.estado == Respuesta.ESTADO_NO_CONFORME) {
                    guardarRespuestaNoConforme(
                        pregunta.preguntaId,
                        respuestaUIState.comentarios,
                        respuestaUIState.tipoAccion,
                        respuestaUIState.idAvisoOrdenTrabajo
                    )
                }
            }

            // Configurar tipo de acción
            binding.tipoAccionRadioGroup.setOnCheckedChangeListener(null)
            when (respuestaUIState.tipoAccion) {
                Respuesta.ACCION_INMEDIATO -> binding.inmediatoRadio.isChecked = true
                Respuesta.ACCION_PROGRAMADO -> binding.programadoRadio.isChecked = true
                else -> {
                    binding.inmediatoRadio.isChecked = false
                    binding.programadoRadio.isChecked = false
                }
            }

            binding.tipoAccionRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.inmediatoRadio -> {
                        respuestaUIState.tipoAccion = Respuesta.ACCION_INMEDIATO
                    }
                    R.id.programadoRadio -> {
                        respuestaUIState.tipoAccion = Respuesta.ACCION_PROGRAMADO
                    }
                }

                respuestaUIState.respondida = validateNoConformeFields(respuestaUIState)

                // Si todos los campos están completos, guardar
                if (respuestaUIState.respondida && respuestaUIState.estado == Respuesta.ESTADO_NO_CONFORME) {
                    guardarRespuestaNoConforme(
                        pregunta.preguntaId,
                        respuestaUIState.comentarios,
                        respuestaUIState.tipoAccion,
                        respuestaUIState.idAvisoOrdenTrabajo
                    )
                }
            }

            // Configurar ID de aviso/OT
            binding.avisoOtEditText.setText(respuestaUIState.idAvisoOrdenTrabajo)
            binding.avisoOtEditText.doAfterTextChanged { text ->
                respuestaUIState.idAvisoOrdenTrabajo = text.toString()
                respuestaUIState.respondida = validateNoConformeFields(respuestaUIState)

                // Si todos los campos están completos, guardar
                if (respuestaUIState.respondida && respuestaUIState.estado == Respuesta.ESTADO_NO_CONFORME) {
                    guardarRespuestaNoConforme(
                        pregunta.preguntaId,
                        respuestaUIState.comentarios,
                        respuestaUIState.tipoAccion,
                        respuestaUIState.idAvisoOrdenTrabajo
                    )
                }
            }
        }

        private fun validateNoConformeFields(state: RespuestaUIState): Boolean {
            // Para que una respuesta NO_CONFORME sea válida, todos los campos deben estar completos
            return state.comentarios.isNotBlank() &&
                    state.tipoAccion.isNotBlank() &&
                    state.idAvisoOrdenTrabajo.isNotBlank()
        }

        private fun updateFotoCount(count: Int) {
            binding.viewPicturesButton.text = "Ver Fotos ($count)"
            binding.viewPicturesButton.isEnabled = count > 0
        }

        private fun setupPhotoButtons(pregunta: Pregunta, respuestaUIState: RespuestaUIState) {
            // Botón para tomar foto
            binding.takePictureButton.setOnClickListener {
                if (respuestaUIState.respuestaId > 0) {
                    onTakePicture(respuestaUIState.respuestaId)
                } else {
                    // Primero necesitamos guardar la respuesta para tener un ID
                    guardarRespuestaNoConforme(
                        pregunta.preguntaId,
                        respuestaUIState.comentarios,
                        respuestaUIState.tipoAccion,
                        respuestaUIState.idAvisoOrdenTrabajo
                    ) { respuestaId ->
                        if (respuestaId > 0) {
                            respuestaUIState.respuestaId = respuestaId
                            onTakePicture(respuestaId)
                        }
                    }
                }
            }

            // Botón para ver fotos
            binding.viewPicturesButton.setOnClickListener {
                if (respuestaUIState.respuestaId > 0) {
                    onViewPictures(respuestaUIState.respuestaId)
                }
            }
        }

        private fun guardarRespuestaConforme(preguntaId: Long) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val respuestaId = respuestaViewModel.guardarRespuestaConforme(inspeccionId, preguntaId)

                    withContext(Dispatchers.Main) {
                        // Actualizar el UI si se obtuvo un ID válido
                        if (respuestaId > 0) {
                            respuestasMap[preguntaId]?.respuestaId = respuestaId
                        }
                    }
                } catch (e: Exception) {
                    // Manejar error
                }
            }
        }

        private fun guardarRespuestaNoConforme(
            preguntaId: Long,
            comentarios: String,
            tipoAccion: String,
            idAvisoOrdenTrabajo: String,
            onSuccess: ((Long) -> Unit)? = null
        ) {
            // Validar campos
            if (comentarios.isBlank() || tipoAccion.isBlank() || idAvisoOrdenTrabajo.isBlank()) {
                return
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val respuestaId = respuestaViewModel.guardarRespuestaNoConforme(
                        inspeccionId,
                        preguntaId,
                        comentarios,
                        tipoAccion,
                        idAvisoOrdenTrabajo
                    )

                    withContext(Dispatchers.Main) {
                        // Actualizar el UI si se obtuvo un ID válido
                        if (respuestaId > 0) {
                            respuestasMap[preguntaId]?.respuestaId = respuestaId
                            onSuccess?.invoke(respuestaId)
                        }
                    }
                } catch (e: Exception) {
                    // Manejar error
                }
            }
        }
    }

    /**
     * DiffCallback para comparar preguntas de forma eficiente.
     */
    class QuestionDiffCallback : DiffUtil.ItemCallback<Pregunta>() {
        override fun areItemsTheSame(oldItem: Pregunta, newItem: Pregunta): Boolean {
            return oldItem.preguntaId == newItem.preguntaId
        }

        override fun areContentsTheSame(oldItem: Pregunta, newItem: Pregunta): Boolean {
            return oldItem == newItem
        }
    }
}