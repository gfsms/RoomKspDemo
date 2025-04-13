package com.example.roomkspdemo.ui.inspection

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.roomkspdemo.CAEXInspectionApp
import com.example.roomkspdemo.R
import com.example.roomkspdemo.data.relations.InspeccionConCAEX
import com.example.roomkspdemo.databinding.ActivityInspectionQuestionnaireBinding
import com.example.roomkspdemo.ui.adapters.CategoryPagerAdapter
import com.example.roomkspdemo.ui.viewmodels.CategoriaPreguntaViewModel
import com.example.roomkspdemo.ui.viewmodels.InspeccionViewModel
import com.example.roomkspdemo.ui.viewmodels.RespuestaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Actividad para responder el cuestionario de inspección.
 *
 * Permite navegar entre categorías de preguntas, responder a cada pregunta como
 * "Conforme" o "No Conforme", y proporcionar detalles adicionales para respuestas
 * no conformes (comentarios, fotos, etc.).
 */
class InspectionQuestionnaireActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInspectionQuestionnaireBinding
    private lateinit var inspeccionViewModel: InspeccionViewModel
    private lateinit var categoriaPreguntaViewModel: CategoriaPreguntaViewModel
    private lateinit var respuestaViewModel: RespuestaViewModel

    private var inspeccionId: Long = 0
    private var inspeccionConCAEX: InspeccionConCAEX? = null
    private var totalCategorias: Int = 0
    private var categoriaActual: Int = 0

    private lateinit var categoryPagerAdapter: CategoryPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar ViewBinding
        binding = ActivityInspectionQuestionnaireBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Obtener ID de inspección
        inspeccionId = intent.getLongExtra(CreateInspectionActivity.EXTRA_INSPECCION_ID, -1)
        if (inspeccionId == -1L) {
            Toast.makeText(this, "Error: ID de inspección no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Inicializar ViewModels
        initViewModels()

        // Configurar botones de navegación
        setupNavigationButtons()

        // Cargar datos de la inspección
        loadInspectionData()
    }

    /**
     * Inicializa los ViewModels utilizando las factorías apropiadas.
     */
    private fun initViewModels() {
        val application = application as CAEXInspectionApp

        // Inicializar ViewModel de Inspección
        inspeccionViewModel = ViewModelProvider(
            this,
            InspeccionViewModel.InspeccionViewModelFactory(
                application.inspeccionRepository,
                application.caexRepository
            )
        )[InspeccionViewModel::class.java]

        // Inicializar ViewModel de Categoría y Pregunta
        categoriaPreguntaViewModel = ViewModelProvider(
            this,
            CategoriaPreguntaViewModel.CategoriaPreguntaViewModelFactory(
                application.categoriaRepository,
                application.preguntaRepository
            )
        )[CategoriaPreguntaViewModel::class.java]

        // Inicializar ViewModel de Respuesta
        respuestaViewModel = ViewModelProvider(
            this,
            RespuestaViewModel.RespuestaViewModelFactory(
                application.respuestaRepository
            )
        )[RespuestaViewModel::class.java]
    }

    /**
     * Configura los botones de navegación para moverse entre categorías.
     */
    private fun setupNavigationButtons() {
        // Botón Anterior
        binding.prevButton.setOnClickListener {
            if (categoriaActual > 0) {
                binding.categoryViewPager.currentItem = categoriaActual - 1
            }
        }

        // Botón Siguiente
        binding.nextButton.setOnClickListener {
            if (categoriaActual < totalCategorias - 1) {
                // Verificar que todas las preguntas en la categoría actual estén respondidas
                if (verificarRespuestasCategoria(categoriaActual)) {
                    binding.categoryViewPager.currentItem = categoriaActual + 1
                } else {
                    Toast.makeText(
                        this,
                        "Debe responder todas las preguntas antes de continuar",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                // Estamos en la última categoría, verificar y finalizar la inspección
                if (verificarRespuestasCategoria(categoriaActual)) {
                    mostrarDialogoFinalizarInspeccion()
                } else {
                    Toast.makeText(
                        this,
                        "Debe responder todas las preguntas antes de continuar",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Callback para cambios de página
        binding.categoryViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                categoriaActual = position
                actualizarEstadoBotones()
                actualizarInfoCategoria()
            }
        })
    }

    /**
     * Carga los datos de la inspección y configura el ViewPager.
     */
    private fun loadInspectionData() {
        inspeccionViewModel.getInspeccionConCAEXById(inspeccionId).observe(this) { inspeccion ->
            if (inspeccion == null) {
                Toast.makeText(this, "Error: Inspección no encontrada", Toast.LENGTH_SHORT).show()
                finish()
                return@observe
            }

            // Guardar referencia a la inspección
            inspeccionConCAEX = inspeccion

            // Actualizar información en la UI
            actualizarInfoInspeccion(inspeccion)

            // Cargar categorías para el modelo de CAEX
            cargarCategoriasYPreguntas(inspeccion.caex.modelo)
        }
    }

    /**
     * Actualiza la información de la inspección en la UI.
     */
    private fun actualizarInfoInspeccion(inspeccion: InspeccionConCAEX) {
        // Información del CAEX
        binding.caexInfoTextView.text = "CAEX ${inspeccion.caex.modelo} #${inspeccion.caex.numeroIdentificador}"

        // Información del inspector
        binding.inspectorTextView.text = "Inspector: ${inspeccion.inspeccion.nombreInspector}"

        // Fecha formateada
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val fechaFormateada = sdf.format(Date(inspeccion.inspeccion.fechaCreacion))
        binding.fechaTextView.text = "Fecha: $fechaFormateada"

        // Título de la toolbar
        supportActionBar?.title = "Control Inicio Intervención"
    }

    /**
     * Carga las categorías y preguntas para el modelo de CAEX especificado.
     */
    private fun cargarCategoriasYPreguntas(modeloCAEX: String) {
        categoriaPreguntaViewModel.getCategoriasConPreguntasByModelo(modeloCAEX)
            .observe(this) { categorias ->

                // Filtrar categorías que tienen preguntas para este modelo
                val categoriasConPreguntas = categorias.filter {
                    it.tienePreguntas(modeloCAEX)
                }.sortedBy { it.categoria.orden }

                totalCategorias = categoriasConPreguntas.size

                // Configurar el adaptador del ViewPager
                categoryPagerAdapter = CategoryPagerAdapter(
                    this,
                    categoriasConPreguntas,
                    modeloCAEX,
                    inspeccionId,
                    respuestaViewModel
                )
                binding.categoryViewPager.adapter = categoryPagerAdapter

                // Actualizar información de progreso
                actualizarInfoProgreso(0, calcularTotalPreguntas(categorias, modeloCAEX))

                // Inicializar botones de navegación
                categoriaActual = 0
                actualizarEstadoBotones()
                actualizarInfoCategoria()
            }
    }

    /**
     * Calcula el número total de preguntas para un modelo de CAEX.
     */
    private fun calcularTotalPreguntas(
        categorias: List<com.example.roomkspdemo.data.relations.CategoriaConPreguntas>,
        modeloCAEX: String
    ): Int {
        return categorias.sumOf { categoria ->
            categoria.getPreguntasParaModelo(modeloCAEX).size
        }
    }

    /**
     * Actualiza la información de progreso en la UI.
     */
    private fun actualizarInfoProgreso(respondidas: Int, total: Int) {
        binding.progressTextView.text = "Progreso: $respondidas/$total preguntas"
    }

    /**
     * Actualiza el estado de los botones de navegación según la posición actual.
     */
    private fun actualizarEstadoBotones() {
        binding.prevButton.isEnabled = categoriaActual > 0

        if (categoriaActual == totalCategorias - 1) {
            binding.nextButton.text = "Finalizar"
        } else {
            binding.nextButton.text = "Siguiente"
        }
    }

    /**
     * Actualiza la información de la categoría actual en la UI.
     */
    private fun actualizarInfoCategoria() {
        val categoriaActualNombre = categoryPagerAdapter.getCategoriaNombre(categoriaActual)
        binding.categoriaActualTextView.text = "Categoría: $categoriaActualNombre"
    }

    /**
     * Verifica que todas las preguntas en una categoría estén respondidas.
     */
    private fun verificarRespuestasCategoria(posicionCategoria: Int): Boolean {
        // Por ahora, simplemente retornamos true para facilitar la navegación
        // Esta lógica se implementará en los fragmentos de categoría
        return categoryPagerAdapter.verificarRespuestasCategoria(posicionCategoria)
    }

    /**
     * Muestra un diálogo de confirmación para finalizar la inspección.
     */
    private fun mostrarDialogoFinalizarInspeccion() {
        AlertDialog.Builder(this)
            .setTitle("Finalizar Inspección")
            .setMessage("¿Está seguro que desea finalizar la inspección? Se guardará el estado actual.")
            .setPositiveButton("Finalizar") { _, _ ->
                // Finalizar la inspección (no cerrarla, solo guardarla y salir)
                Toast.makeText(this, "Inspección guardada", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Crea el menú de opciones.
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_inspection, menu)
        return true
    }

    /**
     * Maneja las selecciones del menú de opciones.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_save -> {
                // Guardar el estado actual de la inspección
                Toast.makeText(this, "Inspección guardada", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Maneja el evento de presionar el botón Back.
     */
    override fun onBackPressed() {
        // Mostrar diálogo de confirmación antes de salir
        AlertDialog.Builder(this)
            .setTitle("Salir de la Inspección")
            .setMessage("¿Está seguro que desea salir? Se guardarán las respuestas ingresadas hasta ahora.")
            .setPositiveButton("Salir") { _, _ ->
                super.onBackPressed()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}