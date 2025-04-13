package com.example.roomkspdemo.ui.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.roomkspdemo.R
import com.example.roomkspdemo.data.entities.Inspeccion
import com.example.roomkspdemo.data.relations.InspeccionConCAEX
import com.example.roomkspdemo.databinding.ItemInspectionBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for displaying Inspection items in a RecyclerView.
 *
 * @param onViewInspectionClick Callback for when the "View Inspection" button is clicked
 */
class InspectionAdapter(
    private val onViewInspectionClick: (InspeccionConCAEX) -> Unit
) : ListAdapter<InspeccionConCAEX, InspectionAdapter.InspectionViewHolder>(InspectionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InspectionViewHolder {
        val binding = ItemInspectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InspectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InspectionViewHolder, position: Int) {
        val inspeccion = getItem(position)
        holder.bind(inspeccion)
    }

    inner class InspectionViewHolder(
        private val binding: ItemInspectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(inspeccionConCAEX: InspeccionConCAEX) {
            val inspeccion = inspeccionConCAEX.inspeccion
            val caex = inspeccionConCAEX.caex

            binding.apply {
                // Set inspection type with appropriate background
                val tipoText = if (inspeccion.tipo == Inspeccion.TIPO_RECEPCION) "RECEPCIÓN" else "ENTREGA"
                tipoInspeccionTextView.text = tipoText

                // Set inspection status with appropriate background
                val estadoText = if (inspeccion.estado == Inspeccion.ESTADO_ABIERTA) "ABIERTA" else "CERRADA"
                estadoTextView.text = estadoText

                // Set background color based on status
                val statusColor = if (inspeccion.estado == Inspeccion.ESTADO_ABIERTA) {
                    ContextCompat.getColor(itemView.context, R.color.status_abierta)
                } else {
                    ContextCompat.getColor(itemView.context, R.color.status_cerrada)
                }
                estadoTextView.backgroundTintList = ColorStateList.valueOf(statusColor)

                // Set CAEX info
                caexInfoTextView.text = "CAEX ${caex.modelo} #${caex.numeroIdentificador}"

                // Set inspector name
                inspectorTextView.text = inspeccion.nombreInspector

                // Set supervisor name
                supervisorTextView.text = inspeccion.nombreSupervisor

                // Format and set date
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val formattedDate = dateFormat.format(Date(inspeccion.fechaCreacion))
                fechaTextView.text = formattedDate

                // Set button click listener
                viewInspectionButton.setOnClickListener {
                    onViewInspectionClick(inspeccionConCAEX)
                }
            }
        }
    }

    /**
     * DiffCallback for efficient list updates.
     */
    class InspectionDiffCallback : DiffUtil.ItemCallback<InspeccionConCAEX>() {
        override fun areItemsTheSame(oldItem: InspeccionConCAEX, newItem: InspeccionConCAEX): Boolean {
            return oldItem.inspeccion.inspeccionId == newItem.inspeccion.inspeccionId
        }

        override fun areContentsTheSame(oldItem: InspeccionConCAEX, newItem: InspeccionConCAEX): Boolean {
            return oldItem.inspeccion == newItem.inspeccion && oldItem.caex == newItem.caex
        }
    }
}