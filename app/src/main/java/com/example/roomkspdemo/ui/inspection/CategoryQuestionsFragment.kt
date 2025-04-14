package com.example.roomkspdemo.ui.inspection

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.roomkspdemo.data.entities.Categoria
import com.example.roomkspdemo.data.entities.Pregunta
import com.example.roomkspdemo.data.relations.CategoriaConPreguntas
import com.example.roomkspdemo.databinding.FragmentCategoryQuestionsBinding
import com.example.roomkspdemo.ui.adapters.QuestionAdapter
import com.example.roomkspdemo.ui.viewmodels.RespuestaViewModel
import kotlinx.parcelize.Parcelize

/**
 * Fragmento que muestra las preguntas de una categoría específica en una inspección.
 */
class CategoryQuestionsFragment : Fragment() {

    private var _binding: FragmentCategoryQuestionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var questionAdapter: QuestionAdapter

    private var categoriaConPreguntas: CategoriaConPreguntasParcelable? = null
    private var modeloCAEX: String = ""
    private var inspeccionId: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryQuestionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtener argumentos
        arguments?.let { args ->
            // Obtener categoría serializada (si es posible)
            categoriaConPreguntas = args.getParcelable(ARG_CATEGORIA)

            // Obtener otros argumentos
            modeloCAEX = args.getString(ARG_MODELO_CAEX, "")
            inspeccionId = args.getLong(ARG_INSPECCION_ID, 0)

            // Configurar título de categoría
            binding.categoryTitleTextView.text = categoriaConPreguntas?.categoria?.nombre ?: "Categoría"

            // Configurar RecyclerView para preguntas
            setupQuestionsRecyclerView()
        }
    }

    /**
     * Configura el RecyclerView para mostrar las preguntas de esta categoría.
     */
    private fun setupQuestionsRecyclerView() {
        // Obtener el ViewModel desde la actividad
        val respuestaViewModel = (activity as? InspectionQuestionnaireActivity)?.getRespuestaViewModel()

        if (respuestaViewModel == null) {
            Log.e(TAG, "Error: No se pudo obtener el ViewModel de respuestas")
            return
        }

        categoriaConPreguntas?.let { categoria ->
            // Obtener preguntas para este modelo
            val preguntas = categoria.getPreguntasParaModelo(modeloCAEX)

            if (preguntas.isEmpty()) {
                // Mostrar estado vacío
                binding.emptyStateTextView.visibility = View.VISIBLE
                binding.questionsRecyclerView.visibility = View.GONE
            } else {
                // Configurar adaptador
                questionAdapter = QuestionAdapter(
                    inspeccionId = inspeccionId,
                    respuestaViewModel = respuestaViewModel,
                    onTakePicture = { respuestaId ->
                        // Implementar toma de fotografía
                        (activity as? InspectionQuestionnaireActivity)?.tomarFotografia(respuestaId)
                    },
                    onViewPictures = { respuestaId ->
                        // Implementar visualización de fotografías
                        (activity as? InspectionQuestionnaireActivity)?.verFotografias(respuestaId)
                    }
                )

                binding.questionsRecyclerView.apply {
                    adapter = questionAdapter
                    layoutManager = LinearLayoutManager(context)
                }

                // Establecer lista de preguntas
                questionAdapter.submitList(preguntas)

                // Cargar respuestas existentes
                questionAdapter.cargarRespuestasExistentes()

                // Mostrar RecyclerView
                binding.emptyStateTextView.visibility = View.GONE
                binding.questionsRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Verifica si todas las preguntas en esta categoría han sido respondidas.
     *
     * @return true si todas las preguntas tienen respuesta, false en caso contrario
     */
    fun verificarTodasRespuestas(): Boolean {
        // Si no hay adaptador, devolver true por defecto
        if (!::questionAdapter.isInitialized) return true

        return questionAdapter.todasPreguntasRespondidas()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Clase Parcelable para transferir una CategoriaConPreguntas entre fragmentos.
     */
    @Parcelize
    data class CategoriaConPreguntasParcelable(
        val categoria: CategoriaParcelable,
        val preguntas: List<PreguntaParcelable>
    ) : Parcelable {
        /**
         * Filtra las preguntas para un modelo específico de CAEX.
         *
         * @param modelo El modelo de CAEX ("797F" o "798AC")
         * @return Una lista de preguntas aplicables al modelo especificado
         */
        fun getPreguntasParaModelo(modelo: String): List<Pregunta> {
            return preguntas
                .filter {
                    it.modeloAplicable == modelo ||
                            it.modeloAplicable == Pregunta.MODELO_TODOS
                }
                .map { it.toPregunta() }
                .sortedBy { it.orden }
        }

        companion object {
            /**
             * Convierte un CategoriaConPreguntas a su versión Parcelable.
             */
            fun fromCategoriaConPreguntas(categoriaConPreguntas: CategoriaConPreguntas): CategoriaConPreguntasParcelable {
                return CategoriaConPreguntasParcelable(
                    categoria = CategoriaParcelable.fromCategoria(categoriaConPreguntas.categoria),
                    preguntas = categoriaConPreguntas.preguntas.map { PreguntaParcelable.fromPregunta(it) }
                )
            }
        }
    }

    /**
     * Versión Parcelable de la entidad Categoria.
     */
    @Parcelize
    data class CategoriaParcelable(
        val categoriaId: Long,
        val nombre: String,
        val orden: Int,
        val modeloAplicable: String,
        val fechaCreacion: Long
    ) : Parcelable {
        companion object {
            fun fromCategoria(categoria: Categoria): CategoriaParcelable {
                return CategoriaParcelable(
                    categoriaId = categoria.categoriaId,
                    nombre = categoria.nombre,
                    orden = categoria.orden,
                    modeloAplicable = categoria.modeloAplicable,
                    fechaCreacion = categoria.fechaCreacion
                )
            }
        }
    }

    /**
     * Versión Parcelable de la entidad Pregunta.
     */
    @Parcelize
    data class PreguntaParcelable(
        val preguntaId: Long,
        val texto: String,
        val categoriaId: Long,
        val orden: Int,
        val modeloAplicable: String,
        val fechaCreacion: Long
    ) : Parcelable {
        /**
         * Convierte a la entidad Pregunta original.
         */
        fun toPregunta(): Pregunta {
            return Pregunta(
                preguntaId = preguntaId,
                texto = texto,
                categoriaId = categoriaId,
                orden = orden,
                modeloAplicable = modeloAplicable,
                fechaCreacion = fechaCreacion
            )
        }

        companion object {
            fun fromPregunta(pregunta: Pregunta): PreguntaParcelable {
                return PreguntaParcelable(
                    preguntaId = pregunta.preguntaId,
                    texto = pregunta.texto,
                    categoriaId = pregunta.categoriaId,
                    orden = pregunta.orden,
                    modeloAplicable = pregunta.modeloAplicable,
                    fechaCreacion = pregunta.fechaCreacion
                )
            }
        }
    }

    companion object {
        private const val TAG = "CategoryQuestionsFragment"

        private const val ARG_CATEGORIA = "arg_categoria"
        private const val ARG_MODELO_CAEX = "arg_modelo_caex"
        private const val ARG_INSPECCION_ID = "arg_inspeccion_id"

        /**
         * Crea una instancia del fragmento con los argumentos especificados.
         */
        fun newInstance(
            categoria: CategoriaConPreguntas,
            modeloCAEX: String,
            inspeccionId: Long
        ): CategoryQuestionsFragment {
            val fragment = CategoryQuestionsFragment()
            Bundle().apply {
                putParcelable(ARG_CATEGORIA, CategoriaConPreguntasParcelable.fromCategoriaConPreguntas(categoria))
                putString(ARG_MODELO_CAEX, modeloCAEX)
                putLong(ARG_INSPECCION_ID, inspeccionId)
                fragment.arguments = this
            }
            return fragment
        }
    }
}