package com.example.sleeping.ui.slideshow

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.sleeping.data.database.SleepDatabase
import com.example.sleeping.data.entity.LightData
import com.example.sleeping.data.entity.NoiseData
import com.example.sleeping.data.entity.SleepSession
import com.example.sleeping.data.repository.SleepRepository
import kotlinx.coroutines.launch

/**
 * ViewModel para mostrar reportes detallados de sesiones de sueño
 */
class SlideshowViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SleepRepository
    
    private val _currentSession = MutableLiveData<SleepSession?>()
    val currentSession: LiveData<SleepSession?> = _currentSession
    
    private val _noiseData = MutableLiveData<List<NoiseData>>()
    val noiseData: LiveData<List<NoiseData>> = _noiseData
    
    private val _lightData = MutableLiveData<List<LightData>>()
    val lightData: LiveData<List<LightData>> = _lightData
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _reportSummary = MutableLiveData<ReportSummary>()
    val reportSummary: LiveData<ReportSummary> = _reportSummary

    init {
        val database = SleepDatabase.getDatabase(application)
        repository = SleepRepository(
            database.sleepSessionDao(),
            database.noiseDataDao(),
            database.lightDataDao()
        )
        
        loadLatestCompletedSession()
    }
    
    private fun loadLatestCompletedSession() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Cargar la sesión completada más reciente
                repository.getAllSessions().collect { sessions ->
                    val latestCompleted = sessions.firstOrNull { it.isCompleted }
                    if (latestCompleted != null) {
                        loadSessionDetails(latestCompleted.id)
                    }
                }
            } catch (e: Exception) {
                // Manejar error
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadSessionDetails(sessionId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val session = repository.getSessionById(sessionId)
                _currentSession.value = session
                
                if (session != null) {
                    // Cargar datos de ruido y luz
                    repository.getNoiseDataForSession(sessionId).collect { noiseList ->
                        _noiseData.value = noiseList
                    }
                    
                    repository.getLightDataForSession(sessionId).collect { lightList ->
                        _lightData.value = lightList
                    }
                    
                    // Generar resumen del reporte
                    generateReportSummary(session)
                }
            } catch (e: Exception) {
                // Manejar error
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun generateReportSummary(session: SleepSession) {
        val snoringEvents = repository.getSnoringEventsCount(session.id)
        
        val summary = ReportSummary(
            sessionId = session.id,
            date = session.startTime,
            totalSleepHours = session.estimatedSleepHours,
            sleepQuality = session.sleepQualityScore,
            averageNoiseLevel = session.averageNoiseLevel,
            maxNoiseLevel = session.maxNoiseLevel,
            totalNoiseEvents = session.noiseEvents,
            snoringEvents = snoringEvents,
            averageLightLevel = session.averageLightLevel,
            lightInterruptions = session.lightInterruptions,
            recommendations = generateRecommendations(session, snoringEvents)
        )
        
        _reportSummary.value = summary
    }
    
    private fun generateRecommendations(session: SleepSession, snoringEvents: Int): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (session.averageNoiseLevel > 35f) {
            recommendations.add("Considere usar tapones para los oídos o mejorar el aislamiento acústico")
        }
        
        if (snoringEvents > 5) {
            recommendations.add("Detectamos ronquidos frecuentes. Considere consultar con un especialista")
        }
        
        if (session.lightInterruptions > 2) {
            recommendations.add("Use cortinas blackout o antifaz para evitar interrupciones por luz")
        }
        
        if (session.estimatedSleepHours < 6f) {
            recommendations.add("Intente acostarse más temprano para obtener al menos 7-8 horas de sueño")
        } else if (session.estimatedSleepHours > 10f) {
            recommendations.add("El exceso de sueño también puede afectar la calidad. Mantenga un horario regular")
        }
        
        if (session.sleepQualityScore >= 80f) {
            recommendations.add("¡Excelente calidad de sueño! Mantenga estos hábitos")
        }
        
        return recommendations
    }
    
    data class ReportSummary(
        val sessionId: Long,
        val date: java.util.Date,
        val totalSleepHours: Float,
        val sleepQuality: Float,
        val averageNoiseLevel: Float,
        val maxNoiseLevel: Float,
        val totalNoiseEvents: Int,
        val snoringEvents: Int,
        val averageLightLevel: Float,
        val lightInterruptions: Int,
        val recommendations: List<String>
    )
}