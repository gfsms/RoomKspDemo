package com.example.roomkspdemo.ui.inspection

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.roomkspdemo.CAEXInspectionApp
import com.example.roomkspdemo.R
import com.example.roomkspdemo.data.relations.CategoriaConPreguntas
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

    // Initialize with an empty adapter immediately to avoid null pointer
    private lateinit var categoryPagerAdapter: CategoryPagerAdapter
    private var swipeCallback: ViewPager2.OnPageChangeCallback? = null

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

        // Initialize with an empty state - this prevents NullPointerException
        setupInitialState()

        // Cargar datos de la inspección
        loadInspectionData()
    }

    /**
     * Set up initial empty state to prevent crashes before data loads
     */
    private fun setupInitialState() {
        // Set temp message
        binding.categoriaActualTextView.text = "Categoría: Cargando..."

        // Set up initial navigation buttons state
        binding.prevButton.isEnabled = false
        binding.nextButton.isEnabled = false

        // Register basic callback to update current position
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
     * Inicializa los ViewModels utilizando las factorías apropiadas.
     */
    private fun initViewModels() {
        val application = application as CAEXInspectionApp

        // Inicializar ViewModel de Inspección
        inspeccionViewModel = ViewModelProvider(
            this,
            InspeccionViewModel.InspeccionViewModelFactory(
                application.inspeccionRepository
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
     * Método para que los fragmentos puedan obtener el ViewModel de respuestas.
     */
    fun getRespuestaViewModel(): RespuestaViewModel {
        return respuestaViewModel
    }

    /**
     * Carga los datos de la inspección y configura el ViewPager.
     */
    private fun loadInspectionData() {
        // Show loading progress
        binding.loadingProgressBar?.visibility = View.VISIBLE

        inspeccionViewModel.getInspeccionConCAEXById(inspeccionId).observe(this) { inspeccion ->
            if (inspeccion == null) {
                binding.loadingProgressBar?.visibility = View.GONE
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
     * Carga las categorías y preguntas para el modelo de CAEX especificado.
     */
    private fun cargarCategoriasYPreguntas(modeloCAEX: String) {
        categoriaPreguntaViewModel.getCategoriasConPreguntasByModelo(modeloCAEX)
            .observe(this) { categorias ->
                // Hide loading progress
                binding.loadingProgressBar?.visibility = View.GONE

                // Filtrar categorías que tienen preguntas para este modelo
                val categoriasConPreguntas = categorias.filter {
                    it.tienePreguntas(modeloCAEX)
                }.sortedBy { it.categoria.orden }

                if (categoriasConPreguntas.isEmpty()) {
                    Toast.makeText(
                        this,
                        "No hay preguntas definidas para este modelo de CAEX",
                        Toast.LENGTH_LONG
                    ).show()
                    return@observe
                }

                totalCategorias = categoriasConPreguntas.size

                // Remove existing callback if it exists
                if (swipeCallback != null) {
                    binding.categoryViewPager.unregisterOnPageChangeCallback(swipeCallback!!)
                    swipeCallback = null
                }

                // Configurar el adaptador del ViewPager
                try {
                    categoryPagerAdapter = CategoryPagerAdapter(
                        this,
                        categoriasConPreguntas,
                        modeloCAEX,
                        inspeccionId,
                        respuestaViewModel
                    )
                    binding.categoryViewPager.adapter = categoryPagerAdapter

                    // Now that the adapter is fully initialized, set up everything else
                    setupNavigationButtons()
                    actualizarInfoProgreso(0, calcularTotalPreguntas(categorias, modeloCAEX))

                    // Reset to first page
                    categoriaActual = 0
                    binding.categoryViewPager.currentItem = 0
                    actualizarEstadoBotones()
                    actualizarInfoCategoria()
                } catch (e: Exception) {
                    Log.e("InspectionActivity", "Error creating adapter: ${e.message}", e)
                    Toast.makeText(
                        this,
                        "Error al cargar las preguntas: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
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
     * Completely disable ViewPager2 swiping
     */
    private fun disableViewPagerSwiping() {
        try {
            // Access the internal RecyclerView
            val viewPager = binding.categoryViewPager
            val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
            recyclerViewField.isAccessible = true
            val recyclerView = recyclerViewField.get(viewPager) as RecyclerView

            // Disable touch events on the RecyclerView
            recyclerView.setOnTouchListener { _, _ -> true }
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling swiping: ${e.message}", e)
        }
    }
    /**
     * Configura los botones de navegación para moverse entre categorías.
     */
    private fun setupNavigationButtons() {

        disableViewPagerSwiping()
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
                    // Guardar el estado de la inspección
                    // y luego mostrar pantalla de resumen para items No Conformes
                    mostrarPantallaResumenNoConformes()
                } else {
                    Toast.makeText(
                        this,
                        "Debe responder todas las preguntas antes de continuar",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        // Create and store the callback so we can unregister it later if needed
        swipeCallback = object : ViewPager2.OnPageChangeCallback() {
            private var lastPosition = 0

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                // Update current position
                categoriaActual = position

                // If we're trying to move forward, check if current category is complete
                if (position > lastPosition) {
                    if (!verificarRespuestasCategoria(lastPosition)) {
                        // If not complete, revert back
                        binding.categoryViewPager.setCurrentItem(lastPosition, false)
                        Toast.makeText(
                            this@InspectionQuestionnaireActivity,
                            "Debe responder todas las preguntas antes de continuar",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
                }

                // Update last position if we successfully changed pages
                lastPosition = position

                // Update UI for new position
                actualizarEstadoBotones()
                actualizarInfoCategoria()
            }
        }

        // Register the callback
        binding.categoryViewPager.registerOnPageChangeCallback(swipeCallback!!)
    }

    /**
     * Calcula el número total de preguntas para un modelo de CAEX.
     */
    private fun calcularTotalPreguntas(
        categorias: List<CategoriaConPreguntas>,
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
        if (!this::categoryPagerAdapter.isInitialized) {
            binding.categoriaActualTextView.text = "Categoría: Cargando..."
            return
        }

        val categoriaActualNombre = categoryPagerAdapter.getCategoriaNombre(categoriaActual)
        binding.categoriaActualTextView.text = "Categoría: $categoriaActualNombre"
    }

    /**
     * Verifica que todas las preguntas en una categoría estén respondidas.
     */
    private fun verificarRespuestasCategoria(posicionCategoria: Int): Boolean {
        // Si el adaptador no está inicializado, retornar true para evitar errores
        if (!this::categoryPagerAdapter.isInitialized) {
            return true
        }

        // Make sure position is valid
        if (posicionCategoria < 0 || posicionCategoria >= totalCategorias) {
            return true
        }

        return categoryPagerAdapter.verificarRespuestasCategoria(posicionCategoria)
    }

    /**
     * Muestra la pantalla de resumen de elementos No Conformes.
     */

    private fun mostrarPantallaResumenNoConformes() {
        try {
            // Create and start the NoConformeSummaryActivity
            val intent = Intent(this, NoConformeSummaryActivity::class.java)
            intent.putExtra(NoConformeSummaryActivity.EXTRA_INSPECCION_ID, inspeccionId)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching summary activity: ${e.message}", e)
            Toast.makeText(
                this,
                "Error al abrir pantalla de resumen: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Implementa la toma de fotografías.
     */
    fun tomarFotografia(respuestaId: Long) {
        // TODO: Implementar la toma de fotografías usando la cámara
        Toast.makeText(this, "Funcionalidad de cámara en desarrollo", Toast.LENGTH_SHORT).show()
    }

    /**
     * Implementa la visualización de fotografías.
     */
    fun verFotografias(respuestaId: Long) {
        // TODO: Implementar la visualización de fotografías
        Toast.makeText(this, "Funcionalidad de galería en desarrollo", Toast.LENGTH_SHORT).show()
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
            R.id.action_export_pdf -> {
                // Exportar la inspección a PDF
                Toast.makeText(this, "Exportación a PDF en desarrollo", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Unregister callbacks to prevent memory leaks
        if (swipeCallback != null) {
            binding.categoryViewPager.unregisterOnPageChangeCallback(swipeCallback!!)
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

    companion object {
        private const val TAG = "InspectionActivity"
    }
}