package com.example.sleeping.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.sleeping.data.database.SleepDatabase
import com.example.sleeping.data.repository.SleepRepository
import com.example.sleeping.ui.model.MonitoringState
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla principal de monitoreo del sueño
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SleepRepository
    
    private val _monitoringState = MutableLiveData<MonitoringState>(MonitoringState.Idle)
    val monitoringState: LiveData<MonitoringState> = _monitoringState
    
    private val _permissionsGranted = MutableLiveData<Boolean>(false)
    val permissionsGranted: LiveData<Boolean> = _permissionsGranted
    
    private val _currentNoiseLevel = MutableLiveData<Float>(0f)
    val currentNoiseLevel: LiveData<Float> = _currentNoiseLevel
    
    private val _currentLightLevel = MutableLiveData<Float>(0f)
    val currentLightLevel: LiveData<Float> = _currentLightLevel

    init {
        val database = SleepDatabase.getDatabase(application)
        repository = SleepRepository(
            database.sleepSessionDao(),
            database.noiseDataDao(),
            database.lightDataDao()
        )
        
        checkForActiveSession()
    }
    
    private fun checkForActiveSession() {
        viewModelScope.launch {
            val activeSession = repository.getActiveSession()
            if (activeSession != null) {
                _monitoringState.value = MonitoringState.Active(
                    startTime = activeSession.startTime.time
                )
            }
        }
    }
    
    fun updatePermissionStatus(granted: Boolean) {
        _permissionsGranted.value = granted
    }
    
    fun startMonitoring() {
        if (_permissionsGranted.value != true) {
            _monitoringState.value = MonitoringState.Error("Permisos requeridos no concedidos")
            return
        }
        
        _monitoringState.value = MonitoringState.Starting
        
        viewModelScope.launch {
            try {
                // Aquí se iniciará el servicio de monitoreo
                // Por ahora simulamos el inicio
                _monitoringState.value = MonitoringState.Active(
                    startTime = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                _monitoringState.value = MonitoringState.Error("Error al iniciar monitoreo: ${e.message}")
            }
        }
    }
    
    fun stopMonitoring() {
        _monitoringState.value = MonitoringState.Stopping
        
        viewModelScope.launch {
            try {
                val activeSession = repository.getActiveSession()
                if (activeSession != null) {
                    repository.completeSession(activeSession.id)
                    _monitoringState.value = MonitoringState.Completed(activeSession.id)
                } else {
                    _monitoringState.value = MonitoringState.Idle
                }
            } catch (e: Exception) {
                _monitoringState.value = MonitoringState.Error("Error al detener monitoreo: ${e.message}")
            }
        }
    }
    
    fun updateSensorData(noiseLevel: Float, lightLevel: Float) {
        _currentNoiseLevel.value = noiseLevel
        _currentLightLevel.value = lightLevel
        
        val currentState = _monitoringState.value
        if (currentState is MonitoringState.Active) {
            _monitoringState.value = currentState.copy(
                currentNoiseLevel = noiseLevel,
                currentLightLevel = lightLevel
            )
        }
    }
    
    fun clearError() {
        if (_monitoringState.value is MonitoringState.Error) {
            _monitoringState.value = MonitoringState.Idle
        }
    }
}