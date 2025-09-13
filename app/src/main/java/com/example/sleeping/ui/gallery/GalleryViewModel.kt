package com.example.sleeping.ui.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.sleeping.data.database.SleepDatabase
import com.example.sleeping.data.entity.SleepSession
import com.example.sleeping.data.repository.SleepRepository
import com.example.sleeping.ui.model.SleepSessionUiModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel para mostrar el historial de sesiones de sueño
 */
class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SleepRepository
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    
    val sleepSessions: LiveData<List<SleepSessionUiModel>>
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _selectedSession = MutableLiveData<SleepSessionUiModel?>()
    val selectedSession: LiveData<SleepSessionUiModel?> = _selectedSession

    init {
        val database = SleepDatabase.getDatabase(application)
        repository = SleepRepository(
            database.sleepSessionDao(),
            database.noiseDataDao(),
            database.lightDataDao()
        )
        
        sleepSessions = repository.getAllSessions()
            .map { sessions -> sessions.map { it.toUiModel() } }
            .asLiveData()
    }
    
    private fun SleepSession.toUiModel(): SleepSessionUiModel {
        val duration = if (endTime != null) {
            val durationMs = endTime.time - startTime.time
            val hours = durationMs / (1000 * 60 * 60)
            val minutes = (durationMs / (1000 * 60)) % 60
            "${hours}h ${minutes}m"
        } else {
            "En progreso"
        }
        
        return SleepSessionUiModel(
            id = id,
            date = startTime,
            duration = duration,
            sleepQualityScore = sleepQualityScore,
            averageNoiseLevel = averageNoiseLevel,
            noiseEvents = noiseEvents,
            lightInterruptions = lightInterruptions,
            isCompleted = isCompleted
        )
    }
    
    fun selectSession(session: SleepSessionUiModel) {
        _selectedSession.value = session
    }
    
    fun clearSelection() {
        _selectedSession.value = null
    }
    
    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val session = repository.getSessionById(sessionId)
                if (session != null) {
                    repository.deleteSession(session)
                }
            } catch (e: Exception) {
                // Manejar error
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun getSessionsFromLastWeek(): LiveData<List<SleepSessionUiModel>> {
        val weekAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
        }.time
        
        return repository.getSessionsFromDate(weekAgo)
            .map { sessions -> sessions.map { it.toUiModel() } }
            .asLiveData()
    }
    
    fun getSessionsFromLastMonth(): LiveData<List<SleepSessionUiModel>> {
        val monthAgo = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
        }.time
        
        return repository.getSessionsFromDate(monthAgo)
            .map { sessions -> sessions.map { it.toUiModel() } }
            .asLiveData()
    }
}