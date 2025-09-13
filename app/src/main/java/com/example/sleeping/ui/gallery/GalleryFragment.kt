package com.example.sleeping.ui.gallery

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sleeping.databinding.FragmentGalleryBinding
import com.example.sleeping.ui.adapter.SleepSessionAdapter
import com.example.sleeping.ui.model.SleepSessionUiModel
import com.google.android.material.snackbar.Snackbar

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var galleryViewModel: GalleryViewModel
    private lateinit var sessionAdapter: SleepSessionAdapter
    
    // Variables para controlar qué filtro está activo
    private var currentFilter: FilterType = FilterType.ALL
    
    enum class FilterType {
        ALL, WEEK, MONTH
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("GalleryFragment", "onCreateView started")
        
        try {
            _binding = FragmentGalleryBinding.inflate(inflater, container, false)
            
            Log.d("GalleryFragment", "Binding inflated successfully")
            
            // Crear el ViewModel con ViewModelProvider y factory
            galleryViewModel = ViewModelProvider(
                this, 
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
            )[GalleryViewModel::class.java]
            
            Log.d("GalleryFragment", "ViewModel created successfully")
            
            setupRecyclerView()
            setupObservers()
            setupClickListeners()
            
            Log.d("GalleryFragment", "onCreateView completed successfully")
            
            return binding.root
        } catch (e: Exception) {
            Log.e("GalleryFragment", "Error in onCreateView", e)
            throw e
        }
    }

    private fun setupRecyclerView() {
        Log.d("GalleryFragment", "Setting up RecyclerView")
        
        try {
            sessionAdapter = SleepSessionAdapter { session ->
                Log.d("GalleryFragment", "Session clicked: ${session.id}")
                onSessionClick(session)
            }
            
            Log.d("GalleryFragment", "Adapter created successfully")
            
            binding.recyclerViewSessions.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = sessionAdapter
                // Agregar un poco de espacio entre elementos
                try {
                    addItemDecoration(androidx.recyclerview.widget.DividerItemDecoration(
                        requireContext(),
                        androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
                    ))
                } catch (e: Exception) {
                    Log.w("GalleryFragment", "Could not add item decoration", e)
                }
            }
            
            Log.d("GalleryFragment", "RecyclerView setup completed")
        } catch (e: Exception) {
            Log.e("GalleryFragment", "Error setting up RecyclerView", e)
            // Fallback básico si hay problemas con el RecyclerView
            if (_binding != null) {
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.recyclerViewSessions.visibility = View.GONE
            }
        }
    }

    private fun setupObservers() {
        // Observar el estado de carga
        galleryViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (_binding != null) {
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
        
        // Configurar observador inicial
        observeSessionsByFilter(FilterType.ALL)
    }

    private fun setupClickListeners() {
        if (_binding == null) return
        
        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, _ ->
            when {
                binding.chipAll.isChecked -> {
                    currentFilter = FilterType.ALL
                    observeSessionsByFilter(FilterType.ALL)
                }
                binding.chipWeek.isChecked -> {
                    currentFilter = FilterType.WEEK
                    observeSessionsByFilter(FilterType.WEEK)
                }
                binding.chipMonth.isChecked -> {
                    currentFilter = FilterType.MONTH
                    observeSessionsByFilter(FilterType.MONTH)
                }
            }
        }
    }

    private fun observeSessionsByFilter(filterType: FilterType) {
        if (_binding == null) return
        
        val liveData = when (filterType) {
            FilterType.ALL -> galleryViewModel.sleepSessions
            FilterType.WEEK -> galleryViewModel.getSessionsFromLastWeek()
            FilterType.MONTH -> galleryViewModel.getSessionsFromLastMonth()
        }
        
        // Remover observadores anteriores para evitar conflictos
        liveData.removeObservers(viewLifecycleOwner)
        
        // Agregar nuevo observador
        liveData.observe(viewLifecycleOwner) { sessions ->
            if (_binding != null && sessions != null) {
                sessionAdapter.submitList(sessions)
                updateEmptyState(sessions.isEmpty())
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (_binding == null) return
        
        try {
            binding.layoutEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.recyclerViewSessions.visibility = if (isEmpty) View.GONE else View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun onSessionClick(session: SleepSessionUiModel) {
        try {
            if (session.isCompleted) {
                // Navegar al reporte detallado
                galleryViewModel.selectSession(session)
                showSessionDetailsSnackbar(session)
            } else {
                Snackbar.make(
                    binding.root,
                    "Esta sesión aún está en progreso",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Snackbar.make(
                binding.root,
                "Error al mostrar los detalles de la sesión",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun showSessionDetailsSnackbar(session: SleepSessionUiModel) {
        try {
            val message = "Calidad: ${session.qualityDescription} (${session.sleepQualityScore.toInt()}/100)"
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                .setAction("Ver Detalle") {
                    // TODO: Navegar a pantalla de detalles o mostrar el reporte
                    // Por ahora mostramos información adicional
                    showSessionSummary(session)
                }
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
            Snackbar.make(
                binding.root,
                "Error al mostrar información de la sesión",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun showSessionSummary(session: SleepSessionUiModel) {
        try {
            val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val summary = """
                📅 Fecha: ${dateFormat.format(session.date)}
                ⏰ Duración: ${session.duration}
                📊 Calidad: ${session.qualityDescription} (${session.sleepQualityScore.toInt()}/100)
                🔊 Ruido promedio: ${session.averageNoiseLevel.toInt()} dB
                🚨 Eventos de ruido: ${session.noiseEvents}
                💡 Interrupciones de luz: ${session.lightInterruptions}
            """.trimIndent()
            
            if (isAdded && context != null) {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Resumen de la Sesión")
                    .setMessage(summary)
                    .setPositiveButton("Cerrar", null)
                    .show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (isAdded) {
                Snackbar.make(
                    binding.root,
                    "Error al mostrar el resumen de la sesión",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}