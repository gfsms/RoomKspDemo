package com.example.roomkspdemo

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.roomkspdemo.databinding.ActivityMainBinding
import com.example.roomkspdemo.ui.adapters.MainViewPagerAdapter
import com.example.roomkspdemo.ui.viewmodels.CAEXViewModel
import com.example.roomkspdemo.ui.viewmodels.CategoriaPreguntaViewModel
import com.example.roomkspdemo.ui.viewmodels.InspeccionViewModel
import com.example.roomkspdemo.util.PdfGenerator
import com.google.android.material.tabs.TabLayoutMediator

/**
 * Main activity for the CAEX Inspection application.
 *
 * This activity serves as the entry point and manages navigation between
 * the different sections of the application using TabLayout and ViewPager2.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // ViewModels
    private lateinit var caexViewModel: CAEXViewModel
    private lateinit var categoriaPreguntaViewModel: CategoriaPreguntaViewModel
    private lateinit var inspeccionViewModel: InspeccionViewModel

    // ViewPager adapter
    private lateinit var viewPagerAdapter: MainViewPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configure Toolbar
        setSupportActionBar(binding.toolbar)

        // Initialize ViewModels
        initViewModels()

        // Set up ViewPager and TabLayout
        setupViewPagerAndTabs()

        // Configure FAB
        setupFloatingActionButton()
    }

    /**
     * Initializes ViewModels using appropriate factories.
     */
    private fun initViewModels() {
        // Get application instance
        val application = application as CAEXInspectionApp

        // Initialize CAEX ViewModel
        caexViewModel = ViewModelProvider(
            this,
            CAEXViewModel.CAEXViewModelFactory(application.caexRepository)
        )[CAEXViewModel::class.java]

        // Initialize Categoria-Pregunta ViewModel
        categoriaPreguntaViewModel = ViewModelProvider(
            this,
            CategoriaPreguntaViewModel.CategoriaPreguntaViewModelFactory(
                application.categoriaRepository,
                application.preguntaRepository
            )
        )[CategoriaPreguntaViewModel::class.java]

        // Initialize Inspección ViewModel
        inspeccionViewModel = ViewModelProvider(
            this,
            InspeccionViewModel.InspeccionViewModelFactory(application.inspeccionRepository)
        )[InspeccionViewModel::class.java]
    }

    /**
     * Sets up ViewPager2 with TabLayout for navigation.
     */
    private fun setupViewPagerAndTabs() {
        // Initialize the ViewPager adapter
        viewPagerAdapter = MainViewPagerAdapter(this)
        binding.viewPager.adapter = viewPagerAdapter

        // Connect TabLayout with ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                MainViewPagerAdapter.TAB_CAEX -> "Equipos"
                MainViewPagerAdapter.TAB_OPEN_INSPECTIONS -> "Inspecciones Abiertas"
                MainViewPagerAdapter.TAB_CLOSED_INSPECTIONS -> "Inspecciones Cerradas"
                else -> "Tab $position"
            }
        }.attach()
    }

    /**
     * Sets up the Floating Action Button behavior.
     */
    private fun setupFloatingActionButton() {
        binding.addFab.setOnClickListener {
            // The behavior depends on the current tab
            when (binding.viewPager.currentItem) {
                MainViewPagerAdapter.TAB_CAEX -> {
                    // Add new CAEX
                    showAddCAEXDialog()
                }
                MainViewPagerAdapter.TAB_OPEN_INSPECTIONS,
                MainViewPagerAdapter.TAB_CLOSED_INSPECTIONS -> {
                    // Create new inspection
                    showCreateInspectionDialog()
                }
            }
        }

        // Update FAB behavior when tabs change
        binding.viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateFabForTab(position)
            }
        })

        // Initial update based on default tab
        updateFabForTab(binding.viewPager.currentItem)
    }

    /**
     * Updates the FAB icon and behavior based on the selected tab.
     */
    private fun updateFabForTab(tabPosition: Int) {
        // You can customize FAB appearance and behavior for each tab
        binding.addFab.contentDescription = when (tabPosition) {
            MainViewPagerAdapter.TAB_CAEX -> "Agregar nuevo CAEX"
            MainViewPagerAdapter.TAB_OPEN_INSPECTIONS,
            MainViewPagerAdapter.TAB_CLOSED_INSPECTIONS -> "Crear nueva inspección"
            else -> "Agregar"
        }

        // Could also change icon if desired:
        // binding.addFab.setImageResource(R.drawable.some_icon)
    }

    /**
     * Shows dialog to add a new CAEX.
     * Currently just shows a placeholder message.
     */
    private fun showAddCAEXDialog() {
        Toast.makeText(this, "Funcionalidad para agregar CAEX en desarrollo", Toast.LENGTH_SHORT).show()
        // TODO: Implement dialog to add new CAEX
    }

    /**
     * Shows dialog to create a new inspection.
     * Currently just shows a placeholder message.
     */
    private fun showCreateInspectionDialog() {
        Toast.makeText(this, "Funcionalidad para crear inspección en desarrollo", Toast.LENGTH_SHORT).show()
        // TODO: Implement dialog to create new inspection
    }

    /**
     * Creates the options menu.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    /**
     * Handles option menu selections.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export_historial -> {
                generateHistorialPdf()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Generates a PDF with the history of inspections.
     */
    private fun generateHistorialPdf() {
        // Show loading message
        Toast.makeText(this, "Generando PDF del historial...", Toast.LENGTH_SHORT).show()

        // Get all inspections
        inspeccionViewModel.allInspeccionesConCAEX.observe(this) { inspecciones ->
            // Generate PDF
            val success = PdfGenerator.generateHistorialPdf(this, inspecciones)

            if (success) {
                Toast.makeText(this, "PDF generado correctamente", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error al generar el PDF", Toast.LENGTH_SHORT).show()
            }

            // Remove observer to prevent multiple executions
            inspeccionViewModel.allInspeccionesConCAEX.removeObservers(this)
        }
    }
}