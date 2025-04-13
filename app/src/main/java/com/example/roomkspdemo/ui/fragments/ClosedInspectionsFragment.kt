package com.example.roomkspdemo.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.roomkspdemo.CAEXInspectionApp
import com.example.roomkspdemo.databinding.FragmentClosedInspectionsBinding
import com.example.roomkspdemo.ui.adapters.InspectionAdapter
import com.example.roomkspdemo.ui.viewmodels.InspeccionViewModel

/**
 * Fragment that displays a list of closed inspections.
 */
class ClosedInspectionsFragment : Fragment() {

    private var _binding: FragmentClosedInspectionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var inspeccionViewModel: InspeccionViewModel
    private lateinit var inspectionAdapter: InspectionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClosedInspectionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        val application = requireActivity().application as CAEXInspectionApp
        inspeccionViewModel = ViewModelProvider(
            this,
            InspeccionViewModel.InspeccionViewModelFactory(application.inspeccionRepository)
        )[InspeccionViewModel::class.java]

        // Set up RecyclerView and adapter
        setupRecyclerView()

        // Observe inspections data
        observeInspectionsData()
    }

    private fun setupRecyclerView() {
        inspectionAdapter = InspectionAdapter { inspeccion ->
            // Handle click on view inspection button
            Toast.makeText(
                requireContext(),
                "Ver inspección #${inspeccion.inspeccion.inspeccionId}",
                Toast.LENGTH_SHORT
            ).show()
            // TODO: Navigate to inspection detail screen
        }

        binding.inspectionsRecyclerView.apply {
            adapter = inspectionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeInspectionsData() {
        // Show loading indicator
        binding.loadingProgressBar.visibility = View.VISIBLE

        inspeccionViewModel.inspeccionesCerradasConCAEX.observe(viewLifecycleOwner) { inspecciones ->
            // Hide loading indicator
            binding.loadingProgressBar.visibility = View.GONE

            if (inspecciones.isEmpty()) {
                // Show empty state
                binding.emptyStateTextView.visibility = View.VISIBLE
                binding.inspectionsRecyclerView.visibility = View.GONE
            } else {
                // Show list
                binding.emptyStateTextView.visibility = View.GONE
                binding.inspectionsRecyclerView.visibility = View.VISIBLE

                // Update adapter with new data
                inspectionAdapter.submitList(inspecciones)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = ClosedInspectionsFragment()
    }
}