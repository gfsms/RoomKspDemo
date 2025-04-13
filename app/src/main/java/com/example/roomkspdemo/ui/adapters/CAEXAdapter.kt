package com.example.roomkspdemo.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.roomkspdemo.data.entities.CAEX
import com.example.roomkspdemo.databinding.ItemCaexBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for displaying CAEX items in a RecyclerView.
 *
 * @param onNewInspectionClick Callback for when the "New Inspection" button is clicked
 */
class CAEXAdapter(
    private val onNewInspectionClick: (CAEX) -> Unit
) : ListAdapter<CAEX, CAEXAdapter.CAEXViewHolder>(CAEXDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CAEXViewHolder {
        val binding = ItemCaexBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CAEXViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CAEXViewHolder, position: Int) {
        val caex = getItem(position)
        holder.bind(caex)
    }

    inner class CAEXViewHolder(
        private val binding: ItemCaexBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(caex: CAEX) {
            binding.apply {
                // Set CAEX model
                modeloTextView.text = caex.modelo

                // Set CAEX ID
                idTextView.text = "#${caex.numeroIdentificador}"

                // Format and set registration date
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val formattedDate = dateFormat.format(Date(caex.fechaRegistro))
                fechaRegistroTextView.text = formattedDate

                // Set button click listener
                newInspectionButton.setOnClickListener {
                    onNewInspectionClick(caex)
                }
            }
        }
    }

    /**
     * DiffCallback for efficient list updates.
     */
    class CAEXDiffCallback : DiffUtil.ItemCallback<CAEX>() {
        override fun areItemsTheSame(oldItem: CAEX, newItem: CAEX): Boolean {
            return oldItem.caexId == newItem.caexId
        }

        override fun areContentsTheSame(oldItem: CAEX, newItem: CAEX): Boolean {
            return oldItem == newItem
        }
    }
}