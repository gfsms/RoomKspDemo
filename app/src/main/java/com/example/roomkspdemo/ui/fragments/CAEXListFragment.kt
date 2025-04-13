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
import com.example.roomkspdemo.databinding.FragmentCaexListBinding
import com.example.roomkspdemo.ui.adapters.CAEXAdapter
import com.example.roomkspdemo.ui.viewmodels.CAEXViewModel

/**
 * Fragment that displays a list of CAEX equipment.
 */
class CAEXListFragment : Fragment() {

    private var _binding: FragmentCaexListBinding? = null
    private val binding get() = _binding!!

    private lateinit var caexViewModel: CAEXViewModel
    private lateinit var caexAdapter: CAEXAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCaexListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        val application = requireActivity().application as CAEXInspectionApp
        caexViewModel = ViewModelProvider(
            this,
            CAEXViewModel.CAEXViewModelFactory(application.caexRepository)
        )[CAEXViewModel::class.java]

        // Set up RecyclerView and adapter
        setupRecyclerView()

        // Observe CAEX data
        observeCAEXData()
    }

    private fun setupRecyclerView() {
        caexAdapter = CAEXAdapter { caex ->
            // Handle click on new inspection button
            Toast.makeText(
                requireContext(),
                "Nueva inspección para CAEX ${caex.modelo} #${caex.numeroIdentificador}",
                Toast.LENGTH_SHORT
            ).show()
            // TODO: Navigate to create inspection screen
        }

        binding.caexRecyclerView.apply {
            adapter = caexAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeCAEXData() {
        // Show loading indicator
        binding.loadingProgressBar.visibility = View.VISIBLE

        caexViewModel.allCAEX.observe(viewLifecycleOwner) { caexList ->
            // Hide loading indicator
            binding.loadingProgressBar.visibility = View.GONE

            if (caexList.isEmpty()) {
                // Show empty state
                binding.emptyStateTextView.visibility = View.VISIBLE
                binding.caexRecyclerView.visibility = View.GONE
            } else {
                // Show list
                binding.emptyStateTextView.visibility = View.GONE
                binding.caexRecyclerView.visibility = View.VISIBLE

                // Update adapter with new data
                caexAdapter.submitList(caexList)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = CAEXListFragment()
    }
}