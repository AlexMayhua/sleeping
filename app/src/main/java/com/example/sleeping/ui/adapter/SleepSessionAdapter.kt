package com.example.sleeping.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sleeping.R
import com.example.sleeping.databinding.ItemSleepSessionBinding
import com.example.sleeping.ui.model.SleepSessionUiModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adaptador para mostrar sesiones de sueño en el RecyclerView
 */
class SleepSessionAdapter(
    private val onItemClick: (SleepSessionUiModel) -> Unit
) : ListAdapter<SleepSessionUiModel, SleepSessionAdapter.SessionViewHolder>(SessionDiffCallback()) {

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemSleepSessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SessionViewHolder(
        private val binding: ItemSleepSessionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(session: SleepSessionUiModel) {
            binding.apply {
                textDate.text = dateFormatter.format(session.date)
                textDuration.text = "Duración: ${session.duration}"
                textQualityScore.text = session.sleepQualityScore.toInt().toString()
                textNoiseLevel.text = session.averageNoiseLevel.toInt().toString()
                textInterruptions.text = (session.noiseEvents + session.lightInterruptions).toString()
                
                chipQuality.text = session.qualityDescription
                
                // Cambiar color del chip según la calidad
                val colorRes = when {
                    session.sleepQualityScore >= 80f -> R.color.design_default_color_primary
                    session.sleepQualityScore >= 60f -> android.R.color.holo_green_dark
                    session.sleepQualityScore >= 40f -> android.R.color.holo_orange_dark
                    else -> android.R.color.holo_red_dark
                }
                
                chipQuality.setChipBackgroundColorResource(colorRes)
                
                // Cambiar color del score según la calidad
                val scoreColor = ContextCompat.getColor(itemView.context, colorRes)
                textQualityScore.setTextColor(scoreColor)
            }
        }
    }

    private class SessionDiffCallback : DiffUtil.ItemCallback<SleepSessionUiModel>() {
        override fun areItemsTheSame(oldItem: SleepSessionUiModel, newItem: SleepSessionUiModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SleepSessionUiModel, newItem: SleepSessionUiModel): Boolean {
            return oldItem == newItem
        }
    }
}
