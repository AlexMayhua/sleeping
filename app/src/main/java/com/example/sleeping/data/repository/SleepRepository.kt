package com.example.sleeping.data.repository

import com.example.sleeping.data.dao.LightDataDao
import com.example.sleeping.data.dao.NoiseDataDao
import com.example.sleeping.data.dao.SleepSessionDao
import com.example.sleeping.data.entity.LightData
import com.example.sleeping.data.entity.NoiseData
import com.example.sleeping.data.entity.SleepSession
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Repositorio principal para manejar datos de análisis de sueño
 */
class SleepRepository(
    private val sleepSessionDao: SleepSessionDao,
    private val noiseDataDao: NoiseDataDao,
    private val lightDataDao: LightDataDao
) {
    
    // === SESIONES DE SUEÑO ===
    
    fun getAllSessions(): Flow<List<SleepSession>> = sleepSessionDao.getAllSessions()
    
    suspend fun getSessionById(sessionId: Long): SleepSession? = sleepSessionDao.getSessionById(sessionId)
    
    suspend fun getActiveSession(): SleepSession? = sleepSessionDao.getActiveSession()
    
    fun getSessionsFromDate(fromDate: Date): Flow<List<SleepSession>> = 
        sleepSessionDao.getSessionsFromDate(fromDate)
    
    suspend fun insertSession(session: SleepSession): Long = sleepSessionDao.insertSession(session)
    
    suspend fun updateSession(session: SleepSession) = sleepSessionDao.updateSession(session)
    
    suspend fun deleteSession(session: SleepSession) = sleepSessionDao.deleteSession(session)
    
    suspend fun getSessionCount(): Int = sleepSessionDao.getSessionCount()
    
    // === DATOS DE RUIDO ===
    
    fun getNoiseDataForSession(sessionId: Long): Flow<List<NoiseData>> = 
        noiseDataDao.getNoiseDataForSession(sessionId)
    
    suspend fun insertNoiseData(noiseData: NoiseData) = noiseDataDao.insertNoiseData(noiseData)
    
    suspend fun insertNoiseDataList(noiseDataList: List<NoiseData>) = 
        noiseDataDao.insertNoiseDataList(noiseDataList)
    
    suspend fun getAverageNoiseLevel(sessionId: Long): Float = 
        noiseDataDao.getAverageNoiseLevel(sessionId) ?: 0f
    
    suspend fun getMaxNoiseLevel(sessionId: Long): Float = 
        noiseDataDao.getMaxNoiseLevel(sessionId) ?: 0f
    
    suspend fun getNoiseEventsCount(sessionId: Long, threshold: Float = 40f): Int = 
        noiseDataDao.getNoiseEventsCount(sessionId, threshold)
    
    suspend fun getSnoringEventsCount(sessionId: Long): Int = 
        noiseDataDao.getSnoringEventsCount(sessionId)
    
    // === DATOS DE LUZ ===
    
    fun getLightDataForSession(sessionId: Long): Flow<List<LightData>> = 
        lightDataDao.getLightDataForSession(sessionId)
    
    suspend fun insertLightData(lightData: LightData) = lightDataDao.insertLightData(lightData)
    
    suspend fun insertLightDataList(lightDataList: List<LightData>) = 
        lightDataDao.insertLightDataList(lightDataList)
    
    suspend fun getAverageLightLevel(sessionId: Long): Float = 
        lightDataDao.getAverageLightLevel(sessionId) ?: 0f
    
    suspend fun getLightInterruptionsCount(sessionId: Long): Int = 
        lightDataDao.getLightInterruptionsCount(sessionId)
    
    // === ANÁLISIS COMPLETO ===
    
    /**
     * Completa una sesión de sueño con todos los análisis calculados
     */
    suspend fun completeSession(sessionId: Long) {
        val session = getSessionById(sessionId) ?: return
        
        val avgNoise = getAverageNoiseLevel(sessionId)
        val maxNoise = getMaxNoiseLevel(sessionId)
        val noiseEvents = getNoiseEventsCount(sessionId)
        val avgLight = getAverageLightLevel(sessionId)
        val lightInterruptions = getLightInterruptionsCount(sessionId)
        
        val sleepHours = calculateSleepHours(session)
        val qualityScore = calculateSleepQuality(
            avgNoise, noiseEvents, lightInterruptions, sleepHours
        )
        
        val completedSession = session.copy(
            endTime = Date(),
            averageNoiseLevel = avgNoise,
            maxNoiseLevel = maxNoise,
            noiseEvents = noiseEvents,
            averageLightLevel = avgLight,
            lightInterruptions = lightInterruptions,
            estimatedSleepHours = sleepHours,
            sleepQualityScore = qualityScore,
            isCompleted = true
        )
        
        updateSession(completedSession)
    }
    
    private fun calculateSleepHours(session: SleepSession): Float {
        val endTime = session.endTime ?: Date()
        val durationMs = endTime.time - session.startTime.time
        return durationMs / (1000f * 60f * 60f) // Convertir a horas
    }
    
    private fun calculateSleepQuality(
        avgNoise: Float,
        noiseEvents: Int,
        lightInterruptions: Int,
        sleepHours: Float
    ): Float {
        // Algoritmo básico de calidad del sueño (0-100)
        var score = 100f
        
        // Penalizar por ruido promedio alto
        if (avgNoise > 35f) {
            score -= (avgNoise - 35f) * 2f
        }
        
        // Penalizar por eventos de ruido
        score -= noiseEvents * 5f
        
        // Penalizar por interrupciones de luz
        score -= lightInterruptions * 10f
        
        // Penalizar por duración inadecuada (menos de 6h o más de 10h)
        if (sleepHours < 6f) {
            score -= (6f - sleepHours) * 10f
        } else if (sleepHours > 10f) {
            score -= (sleepHours - 10f) * 5f
        }
        
        return maxOf(0f, minOf(100f, score))
    }
}
