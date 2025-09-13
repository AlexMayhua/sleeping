package com.example.sleeping.ui.home

import android.content.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.sleeping.databinding.FragmentHomeBinding
import com.example.sleeping.service.SleepMonitoringService
import com.example.sleeping.ui.model.MonitoringState
import com.example.sleeping.util.PermissionUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var homeViewModel: HomeViewModel
    private val dateFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    // BroadcastReceiver para actualizaciones del servicio
    private val sensorUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == SleepMonitoringService.ACTION_SENSOR_UPDATE) {
                val noiseLevel = intent.getFloatExtra(SleepMonitoringService.EXTRA_NOISE_LEVEL, 0f)
                val lightLevel = intent.getFloatExtra(SleepMonitoringService.EXTRA_LIGHT_LEVEL, 0f)
                
                homeViewModel.updateSensorData(noiseLevel, lightLevel)
                
                // Actualizar UI
                binding.textNoiseLevel.text = "${noiseLevel.toInt()} dB"
                binding.textLightLevel.text = "${lightLevel.toInt()} lux"
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        
        setupObservers()
        setupClickListeners()
        
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Verificar permisos al inicio
        updatePermissionStatus()
        
        // Registrar receiver para actualizaciones del servicio
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            sensorUpdateReceiver,
            IntentFilter(SleepMonitoringService.ACTION_SENSOR_UPDATE)
        )
    }

    private fun setupObservers() {
        homeViewModel.monitoringState.observe(viewLifecycleOwner) { state ->
            updateUIForState(state)
        }
        
        homeViewModel.permissionsGranted.observe(viewLifecycleOwner) { granted ->
            updatePermissionUI(granted)
        }
        
        homeViewModel.currentNoiseLevel.observe(viewLifecycleOwner) { level ->
            binding.textNoiseLevel.text = "${level.toInt()} dB"
        }
        
        homeViewModel.currentLightLevel.observe(viewLifecycleOwner) { level ->
            binding.textLightLevel.text = "${level.toInt()} lux"
        }
    }

    private fun setupClickListeners() {
        binding.buttonStartStop.setOnClickListener {
            when (homeViewModel.monitoringState.value) {
                is MonitoringState.Idle -> {
                    if (PermissionUtils.areAllPermissionsGranted(requireContext())) {
                        startMonitoring()
                    } else {
                        requestPermissions()
                    }
                }
                is MonitoringState.Active -> {
                    showStopConfirmationDialog()
                }
                else -> {
                    // Estado transitorio, no hacer nada
                }
            }
        }
        
        binding.buttonPermissions.setOnClickListener {
            requestPermissions()
        }
    }

    private fun updateUIForState(state: MonitoringState) {
        when (state) {
            is MonitoringState.Idle -> {
                binding.textStatus.text = "Listo para iniciar"
                binding.textDuration.visibility = View.GONE
                binding.buttonStartStop.text = "Iniciar Monitoreo"
                binding.buttonStartStop.isEnabled = true
                binding.buttonStartStop.setIconResource(android.R.drawable.ic_media_play)
            }
            
            is MonitoringState.Starting -> {
                binding.textStatus.text = "Iniciando monitoreo..."
                binding.buttonStartStop.isEnabled = false
            }
            
            is MonitoringState.Active -> {
                binding.textStatus.text = "Monitoreando sueño activamente"
                binding.textDuration.visibility = View.VISIBLE
                binding.textDuration.text = "Iniciado: ${dateFormatter.format(Date(state.startTime))}"
                binding.buttonStartStop.text = "Detener Monitoreo"
                binding.buttonStartStop.isEnabled = true
                binding.buttonStartStop.setIconResource(android.R.drawable.ic_media_pause)
            }
            
            is MonitoringState.Stopping -> {
                binding.textStatus.text = "Finalizando análisis..."
                binding.buttonStartStop.isEnabled = false
            }
            
            is MonitoringState.Completed -> {
                binding.textStatus.text = "Análisis completado"
                showCompletionSnackbar(state.sessionId)
                homeViewModel.clearError() // Volver a estado idle
            }
            
            is MonitoringState.Error -> {
                binding.textStatus.text = "Error: ${state.message}"
                binding.buttonStartStop.isEnabled = true
                showErrorSnackbar(state.message)
            }
        }
    }

    private fun updatePermissionUI(granted: Boolean) {
        if (granted) {
            binding.buttonPermissions.text = "✓ Permisos Concedidos"
            binding.buttonPermissions.isEnabled = false
        } else {
            binding.buttonPermissions.text = "Verificar Permisos"
            binding.buttonPermissions.isEnabled = true
        }
    }

    private fun startMonitoring() {
        val intent = Intent(requireContext(), SleepMonitoringService::class.java).apply {
            action = SleepMonitoringService.ACTION_START_MONITORING
        }
        requireContext().startForegroundService(intent)
        homeViewModel.startMonitoring()
    }

    private fun stopMonitoring() {
        val intent = Intent(requireContext(), SleepMonitoringService::class.java).apply {
            action = SleepMonitoringService.ACTION_STOP_MONITORING
        }
        requireContext().startService(intent)
        homeViewModel.stopMonitoring()
    }

    private fun showStopConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirmar")
            .setMessage("¿Está seguro que desea detener el monitoreo del sueño? Se generará un reporte con los datos recopilados.")
            .setPositiveButton("Detener") { _, _ ->
                stopMonitoring()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun requestPermissions() {
        if (PermissionUtils.shouldShowRationalForAnyPermission(requireActivity())) {
            showPermissionExplanationDialog()
        } else {
            PermissionUtils.requestAllPermissions(requireActivity())
        }
    }

    private fun showPermissionExplanationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Permisos Necesarios")
            .setMessage(PermissionUtils.getPermissionExplanationMessage())
            .setPositiveButton("Conceder Permisos") { _, _ ->
                PermissionUtils.requestAllPermissions(requireActivity())
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updatePermissionStatus() {
        val granted = PermissionUtils.areAllPermissionsGranted(requireContext())
        homeViewModel.updatePermissionStatus(granted)
    }

    private fun showCompletionSnackbar(sessionId: Long) {
        Snackbar.make(
            binding.root,
            "Análisis del sueño completado. Revise su reporte en la sección de Reportes.",
            Snackbar.LENGTH_LONG
        ).setAction("Ver Reporte") {
            // Navegar a la sección de reportes
            // TODO: Implementar navegación
        }.show()
    }

    private fun showErrorSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Reintentar") {
                homeViewModel.clearError()
            }.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        PermissionUtils.handlePermissionResult(
            requestCode = requestCode,
            permissions = permissions,
            grantResults = grantResults,
            onAllGranted = {
                updatePermissionStatus()
                Snackbar.make(binding.root, "Todos los permisos concedidos", Snackbar.LENGTH_SHORT).show()
            },
            onSomeGranted = { granted, denied ->
                updatePermissionStatus()
                val deniedNames = PermissionUtils.getPermissionDisplayNames(denied)
                Snackbar.make(
                    binding.root,
                    "Permisos faltantes: ${deniedNames.joinToString(", ")}",
                    Snackbar.LENGTH_LONG
                ).show()
            },
            onAllDenied = {
                updatePermissionStatus()
                Snackbar.make(
                    binding.root,
                    "Los permisos son necesarios para el funcionamiento de la app",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        )
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(sensorUpdateReceiver)
        _binding = null
    }
}