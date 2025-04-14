package com.example.roomkspdemo.ui.inspection

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.roomkspdemo.CAEXInspectionApp
import com.example.roomkspdemo.MainActivity
import com.example.roomkspdemo.R
import com.example.roomkspdemo.databinding.ActivityNoConformeSummaryBinding
import com.example.roomkspdemo.ui.adapters.NoConformeSummaryAdapter
import com.example.roomkspdemo.ui.viewmodels.InspeccionViewModel
import com.example.roomkspdemo.ui.viewmodels.RespuestaViewModel
import com.example.roomkspdemo.util.PdfGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Actividad que muestra el resumen de elementos No Conformes para su finalización.
 *
 * Permite al usuario:
 * - Ver todos los elementos marcados como No Conforme
 * - Completar los detalles para cada uno (comentarios, tipo de acción, ID)
 * - Generar un PDF con el resumen
 * - Finalizar y guardar la inspección
 */
class NoConformeSummaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoConformeSummaryBinding
    private lateinit var inspeccionViewModel: InspeccionViewModel
    private lateinit var respuestaViewModel: RespuestaViewModel
    private lateinit var adapter: NoConformeSummaryAdapter

    private var inspeccionId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar ViewBinding
        binding = ActivityNoConformeSummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Obtener ID de inspección
        inspeccionId = intent.getLongExtra(EXTRA_INSPECCION_ID, -1)
        if (inspeccionId == -1L) {
            Toast.makeText(this, "Error: ID de inspección no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Inicializar ViewModels
        initViewModels()

        // Configurar RecyclerView
        setupRecyclerView()

        // Configurar botones
        setupButtons()

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
                application.inspeccionRepository
            )
        )[InspeccionViewModel::class.java]

        // Inicializar ViewModel de Respuesta
        respuestaViewModel = ViewModelProvider(
            this,
            RespuestaViewModel.RespuestaViewModelFactory(
                application.respuestaRepository
            )
        )[RespuestaViewModel::class.java]
    }

    /**
     * Configura el RecyclerView con su adaptador.
     */
    private fun setupRecyclerView() {
        adapter = NoConformeSummaryAdapter(
            respuestaViewModel = respuestaViewModel,
            onViewPhotos = { respuestaId ->
                // Implementar visualización de fotos
                Toast.makeText(this, "Visualización de fotos en desarrollo", Toast.LENGTH_SHORT).show()
            },
            onDataChanged = {
                // Actualizar estado del botón Finalizar basado en si todos los campos están completos
                updateFinishButtonState()
            }
        )

        binding.noConformesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@NoConformeSummaryActivity)
            adapter = this@NoConformeSummaryActivity.adapter
        }
    }

    /**
     * Configura los botones de acción.
     */
    private fun setupButtons() {
        // Botón para generar PDF
        binding.generatePdfButton.setOnClickListener {
            // Validar que todos los datos estén completos
            if (!allFieldsComplete()) {
                Toast.makeText(this, "Complete todos los campos antes de generar el PDF", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Generar PDF
            generatePdf()
        }

        // Botón para finalizar
        binding.finishButton.setOnClickListener {
            // Validar que todos los datos estén completos
            if (!allFieldsComplete()) {
                Toast.makeText(this, "Complete todos los campos antes de finalizar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Confirmar y finalizar
            confirmAndFinish()
        }

        // Estado inicial del botón Finalizar (deshabilitado hasta que se completen todos los campos)
        binding.finishButton.isEnabled = false
    }

    /**
     * Actualiza el estado del botón Finalizar basado en si todos los campos requeridos están completos.
     */
    private fun updateFinishButtonState() {
        binding.finishButton.isEnabled = allFieldsComplete()
    }

    /**
     * Verifica si todos los campos requeridos están completos.
     *
     * @return true si todos los campos están completos, false caso contrario
     */
    private fun allFieldsComplete(): Boolean {
        return adapter.areAllItemsComplete()
    }

    /**
     * Carga los datos de la inspección y las respuestas No Conformes.
     */
    private fun loadInspectionData() {
        // Mostrar indicador de carga
        binding.loadingProgressBar.visibility = View.VISIBLE

        // Cargar detalles de la inspección
        inspeccionViewModel.getInspeccionConCAEXById(inspeccionId).observe(this) { inspeccion ->
            if (inspeccion == null) {
                Toast.makeText(this, "Error: Inspección no encontrada", Toast.LENGTH_SHORT).show()
                finish()
                return@observe
            }

            // Actualizar información en la UI
            updateInspectionInfo(inspeccion)

            // Cargar las respuestas No Conformes
            loadNoConformeResponses()
        }
    }

    /**
     * Actualiza la información de la inspección en la UI.
     */
    private fun updateInspectionInfo(inspeccion: com.example.roomkspdemo.data.relations.InspeccionConCAEX) {
        // Información del CAEX
        binding.caexInfoTextView.text = "CAEX ${inspeccion.caex.modelo} #${inspeccion.caex.numeroIdentificador}"

        // Información del inspector
        binding.inspectorTextView.text = "Inspector: ${inspeccion.inspeccion.nombreInspector}"

        // Fecha formateada
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val fechaFormateada = sdf.format(Date(inspeccion.inspeccion.fechaCreacion))
        binding.fechaTextView.text = "Fecha: $fechaFormateada"
    }

    /**
     * Carga las respuestas No Conformes para esta inspección.
     */
    private fun loadNoConformeResponses() {
        respuestaViewModel.getRespuestasConDetallesByInspeccionYEstado(
            inspeccionId,
            com.example.roomkspdemo.data.entities.Respuesta.ESTADO_NO_CONFORME
        ).observe(this) { respuestas ->
            // Ocultar indicador de carga
            binding.loadingProgressBar.visibility = View.GONE

            if (respuestas.isEmpty()) {
                // Mostrar mensaje de estado vacío
                binding.emptyStateTextView.visibility = View.VISIBLE
                binding.noConformesRecyclerView.visibility = View.GONE
            } else {
                // Mostrar lista de No Conformes
                binding.emptyStateTextView.visibility = View.GONE
                binding.noConformesRecyclerView.visibility = View.VISIBLE

                // Actualizar contador
                binding.noConformesCountTextView.text = "Total No Conformes: ${respuestas.size}"

                // Actualizar adaptador con nuevos datos
                adapter.submitList(respuestas)
            }
        }
    }

    /**
     * Genera un PDF con el resumen de elementos No Conformes.
     */
    private fun generatePdf() {
        // Mostrar mensaje de carga
        Toast.makeText(this, "Generando PDF...", Toast.LENGTH_SHORT).show()

        // Utilizar la clase PdfGenerator para crear el PDF
        val respuestas = adapter.getCurrentItems()
        if (respuestas.isNotEmpty()) {
            inspeccionViewModel.getInspeccionConCAEXById(inspeccionId).observe(this) { inspeccion ->
                if (inspeccion != null) {
                    val success = PdfGenerator.generateDetalleInspeccionPdf(
                        this,
                        inspeccion,
                        respuestas,
                        true  // Mostrar solo No Conformes
                    )

                    if (success) {
                        Toast.makeText(this, "PDF generado correctamente", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error al generar el PDF", Toast.LENGTH_SHORT).show()
                    }
                }
                // Quitar observer para prevenir múltiples ejecuciones
                inspeccionViewModel.getInspeccionConCAEXById(inspeccionId).removeObservers(this)
            }
        } else {
            Toast.makeText(this, "No hay elementos No Conformes para incluir en el PDF", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Muestra diálogo de confirmación y finaliza la inspección.
     */
    private fun confirmAndFinish() {
        AlertDialog.Builder(this)
            .setTitle("Finalizar Inspección")
            .setMessage("¿Está seguro que desea finalizar y cerrar esta inspección?")
            .setPositiveButton("Finalizar") { _, _ ->
                finishInspection()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Finaliza la inspección, guardando todos los cambios.
     */
    private fun finishInspection() {
        // Guardar los cambios finales de No Conformes
        adapter.saveAllChanges {
            // Cerrar la inspección una vez que se han guardado todos los cambios
            inspeccionViewModel.cerrarInspeccion(inspeccionId, "").observe(this) { success: Boolean ->
                if (success) {
                    Toast.makeText(this, "Inspección finalizada correctamente", Toast.LENGTH_SHORT).show()

                    // Volver a la pantalla principal
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Error al finalizar la inspección", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Crea el menú de opciones.
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_no_conforme_summary, menu)
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
                // Guardar el estado actual sin finalizar
                adapter.saveAllChanges {
                    Toast.makeText(this, "Cambios guardados", Toast.LENGTH_SHORT).show()
                }
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
            .setTitle("Salir sin finalizar")
            .setMessage("¿Está seguro que desea salir? Se guardarán los cambios realizados hasta ahora, pero la inspección quedará abierta.")
            .setPositiveButton("Salir") { _, _ ->
                adapter.saveAllChanges {
                    super.onBackPressed()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    companion object {
        const val EXTRA_INSPECCION_ID = "extra_inspeccion_id"
    }
}