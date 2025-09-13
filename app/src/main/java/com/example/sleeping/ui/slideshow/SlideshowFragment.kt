package com.example.sleeping.ui.slideshow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sleeping.databinding.FragmentSlideshowBinding
import com.example.sleeping.ui.adapter.RecommendationAdapter
import com.example.sleeping.util.ReportExporter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SlideshowFragment : Fragment() {

    private var _binding: FragmentSlideshowBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var slideshowViewModel: SlideshowViewModel
    private lateinit var recommendationAdapter: RecommendationAdapter
    
    private val dateFormatter = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        slideshowViewModel = ViewModelProvider(this)[SlideshowViewModel::class.java]
        
        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)
        
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        
        return binding.root
    }

    private fun setupRecyclerView() {
        recommendationAdapter = RecommendationAdapter()
        
        binding.recyclerViewRecommendations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recommendationAdapter
        }
    }

    private fun setupObservers() {
        slideshowViewModel.currentSession.observe(viewLifecycleOwner) { session ->
            session?.let { updateSessionUI(it) }
        }
        
        slideshowViewModel.reportSummary.observe(viewLifecycleOwner) { summary ->
            summary?.let { updateSummaryUI(it) }
        }
        
        slideshowViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Mostrar/ocultar loading si es necesario
            binding.buttonExport.isEnabled = !isLoading
        }
    }

    private fun setupClickListeners() {
        binding.buttonExport.setOnClickListener {
            exportCurrentReport()
        }
    }

    private fun updateSessionUI(session: com.example.sleeping.data.entity.SleepSession) {
        binding.apply {
            textSessionDate.text = dateFormatter.format(session.startTime)
            textSleepHours.text = String.format("%.1f", session.estimatedSleepHours)
            textQualityScore.text = session.sleepQualityScore.toInt().toString()
            textQualityDescription.text = getQualityDescription(session.sleepQualityScore)
            
            // Métricas de ruido
            textAvgNoise.text = session.averageNoiseLevel.toInt().toString()
            textMaxNoise.text = session.maxNoiseLevel.toInt().toString()
            textNoiseEvents.text = session.noiseEvents.toString()
            
            // Métricas de luz
            textAvgLight.text = String.format("%.1f", session.averageLightLevel)
            textLightInterruptions.text = session.lightInterruptions.toString()
        }
    }

    private fun updateSummaryUI(summary: SlideshowViewModel.ReportSummary) {
        binding.apply {
            // Actualizar el texto de ronquidos
            textSnoringEvents.text = "Ronquidos detectados: ${summary.snoringEvents} eventos"
            
            // Actualizar recomendaciones
            recommendationAdapter.submitList(summary.recommendations)
        }
    }

    private fun getQualityDescription(score: Float): String {
        return when {
            score >= 80f -> "Calidad Excelente"
            score >= 60f -> "Buena Calidad"
            score >= 40f -> "Calidad Regular"
            score >= 20f -> "Mala Calidad"
            else -> "Calidad Muy Mala"
        }
    }

    private fun exportCurrentReport() {
        val session = slideshowViewModel.currentSession.value
        if (session == null) {
            Snackbar.make(binding.root, "No hay sesión disponible para exportar", Snackbar.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                binding.buttonExport.isEnabled = false
                binding.buttonExport.text = "Exportando..."
                
                val result = ReportExporter.exportSessionToCsv(
                    context = requireContext(),
                    session = session,
                    includeRawData = false
                )
                
                result.fold(
                    onSuccess = { uri ->
                        val fileName = "reporte_sueno_${System.currentTimeMillis()}.csv"
                        
                        Snackbar.make(
                            binding.root,
                            "Reporte exportado exitosamente",
                            Snackbar.LENGTH_LONG
                        ).setAction("Compartir") {
                            ReportExporter.shareReport(requireContext(), uri, fileName)
                        }.show()
                    },
                    onFailure = { exception ->
                        Snackbar.make(
                            binding.root,
                            "Error al exportar: ${exception.message}",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                )
            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    "Error inesperado: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            } finally {
                binding.buttonExport.isEnabled = true
                binding.buttonExport.text = "Exportar Reporte"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}