package com.example.roomkspdemo.ui.adapters

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
                        respondida = respuesta.estado == Respuesta.ESTADO_CONFORME ||
                                (respuesta.estado == Respuesta.ESTADO_NO_CONFORME && respuesta.comentarios.isNotBlank())
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

        // TextWatcher para el campo de comentarios
        private var commentTextWatcher: TextWatcher? = null

        fun bind(pregunta: Pregunta) {
            // Mostrar texto de la pregunta
            binding.questionTextView.text = pregunta.texto

            // Obtener el estado actual de la respuesta para esta pregunta
            val respuestaUIState = respuestasMap.getOrPut(pregunta.preguntaId) {
                RespuestaUIState()
            }

            // Configurar estado de los radio buttons
            setupRadioButtons(pregunta, respuestaUIState)

            // Configure the comment field if No Conforme
            setupCommentField(pregunta, respuestaUIState)

            // Hide the fields that will be in the summary screen
            binding.tipoAccionRadioGroup.visibility = View.GONE
            val avisoOtLayout = binding.avisoOtEditText.parent as? ViewGroup
            avisoOtLayout?.visibility = View.GONE
            binding.takePictureButton.visibility = View.GONE
            binding.viewPicturesButton.visibility = View.GONE
        }

        private fun setupRadioButtons(pregunta: Pregunta, respuestaUIState: RespuestaUIState) {
            // Quitar listener temporalmente para evitar llamadas circulares
            binding.answerRadioGroup.setOnCheckedChangeListener(null)

            // Reset radio button state
            binding.answerRadioGroup.clearCheck()

            // Establecer estado según la respuesta
            when (respuestaUIState.estado) {
                Respuesta.ESTADO_CONFORME -> binding.conformeRadio.isChecked = true
                Respuesta.ESTADO_NO_CONFORME -> binding.noConformeRadio.isChecked = true
            }

            // Show or hide the No Conforme container based on current state
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
                        // Show No Conforme area for comments
                        respuestaUIState.estado = Respuesta.ESTADO_NO_CONFORME
                        binding.noConformeContainer.visibility = View.VISIBLE

                        // The question is only considered answered when comments are provided
                        respuestaUIState.respondida = respuestaUIState.comentarios.isNotBlank()

                        // If comments already exist, save it
                        if (respuestaUIState.comentarios.isNotBlank()) {
                            guardarRespuestaNoConformeSimplificada(
                                pregunta.preguntaId,
                                respuestaUIState.comentarios
                            )
                        }
                    }
                }
            }
        }

        private fun setupCommentField(pregunta: Pregunta, respuestaUIState: RespuestaUIState) {
            // Remove previous TextWatcher if it exists
            if (commentTextWatcher != null) {
                binding.commentEditText.removeTextChangedListener(commentTextWatcher)
            }

            // Set the current comment text
            binding.commentEditText.setText(respuestaUIState.comentarios)

            // Create new TextWatcher
            commentTextWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val newComment = s.toString()
                    respuestaUIState.comentarios = newComment

                    // The question is considered answered when comments are provided
                    val wasAnswered = respuestaUIState.respondida
                    respuestaUIState.respondida =
                        respuestaUIState.estado == Respuesta.ESTADO_CONFORME ||
                                (respuestaUIState.estado == Respuesta.ESTADO_NO_CONFORME && newComment.isNotBlank())

                    // Save to database when comment is entered
                    if (respuestaUIState.estado == Respuesta.ESTADO_NO_CONFORME && newComment.isNotBlank()) {
                        guardarRespuestaNoConformeSimplificada(pregunta.preguntaId, newComment)
                    }

                    // Notify about changes in answered status
                    if (wasAnswered != respuestaUIState.respondida) {
                        notifyItemChanged(adapterPosition)
                    }
                }
            }

            // Set the new TextWatcher
            binding.commentEditText.addTextChangedListener(commentTextWatcher)
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
                    Log.e("QuestionAdapter", "Error saving conforme response: ${e.message}")
                }
            }
        }

        private fun guardarRespuestaNoConformeSimplificada(preguntaId: Long, comentarios: String) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val respuestaId = respuestaViewModel.guardarRespuestaNoConformeSimplificada(
                        inspeccionId,
                        preguntaId,
                        comentarios
                    )

                    withContext(Dispatchers.Main) {
                        // Actualizar el UI si se obtuvo un ID válido
                        if (respuestaId > 0) {
                            respuestasMap[preguntaId]?.respuestaId = respuestaId
                        }
                    }
                } catch (e: Exception) {
                    // Manejar error
                    Log.e("QuestionAdapter", "Error saving no conforme response: ${e.message}")
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