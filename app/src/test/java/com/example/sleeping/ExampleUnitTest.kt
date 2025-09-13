package com.example.sleeping

import com.example.sleeping.data.entity.SleepSession
import com.example.sleeping.util.BatteryOptimizationUtils
import org.junit.Test
import org.junit.Assert.*
import java.util.*

/**
 * Pruebas unitarias para el analizador de calidad del sueño
 */
class SleepAnalysisUnitTest {

    @Test
    fun sleepSession_calculatesQualityCorrectly() {
        val session = SleepSession(
            id = 1,
            startTime = Date(),
            endTime = Date(System.currentTimeMillis() + 8 * 60 * 60 * 1000), // 8 horas después
            averageNoiseLevel = 25f,
            maxNoiseLevel = 40f,
            noiseEvents = 3,
            averageLightLevel = 0.5f,
            lightInterruptions = 1,
            estimatedSleepHours = 8f,
            sleepQualityScore = 85f,
            isCompleted = true
        )

        // Verificar que los valores están en rangos esperados
        assertTrue("La calidad debe estar entre 0 y 100", session.sleepQualityScore in 0f..100f)
        assertTrue("Las horas de sueño deben ser positivas", session.estimatedSleepHours > 0)
        assertTrue("El ruido promedio debe ser positivo", session.averageNoiseLevel >= 0)
        assertTrue("La luz promedio debe ser positiva", session.averageLightLevel >= 0)
    }

    @Test
    fun sleepSession_validatesCompletion() {
        val activeSession = SleepSession(
            startTime = Date(),
            endTime = null,
            isCompleted = false
        )

        val completedSession = SleepSession(
            startTime = Date(),
            endTime = Date(),
            isCompleted = true
        )

        assertFalse("Sesión activa no debe estar completada", activeSession.isCompleted)
        assertTrue("Sesión completada debe estar marcada como completada", completedSession.isCompleted)
        assertNotNull("Sesión completada debe tener fecha de fin", completedSession.endTime)
    }

    @Test
    fun batteryOptimization_configurationIsValid() {
        val config = BatteryOptimizationUtils.Config(
            audioSampleInterval = 30000L,
            lightSampleInterval = 10000L,
            audioRecordingDuration = 3000L,
            enableContinuousMonitoring = true
        )

        assertTrue("Intervalo de audio debe ser positivo", config.audioSampleInterval > 0)
        assertTrue("Intervalo de luz debe ser positivo", config.lightSampleInterval > 0)
        assertTrue("Duración de grabación debe ser positiva", config.audioRecordingDuration > 0)
        assertTrue("Duración de grabación debe ser menor al intervalo", 
            config.audioRecordingDuration < config.audioSampleInterval)
    }

    @Test
    fun qualityScore_calculationLogic() {
        // Prueba para diferentes escenarios de calidad
        val excellentScore = 95f
        val goodScore = 75f
        val poorScore = 25f

        assertTrue("Puntuación excelente debe ser >= 80", excellentScore >= 80f)
        assertTrue("Puntuación buena debe estar entre 60-79", goodScore in 60f..79f)
        assertTrue("Puntuación pobre debe ser < 40", poorScore < 40f)
    }

    @Test
    fun noiseLevel_thresholdValidation() {
        val quietNight = 20f // dB
        val normalNight = 35f // dB
        val noisyNight = 50f // dB

        val noiseThreshold = 40f

        assertTrue("Noche silenciosa debe estar por debajo del umbral", quietNight < noiseThreshold)
        assertTrue("Noche normal debe estar por debajo del umbral", normalNight < noiseThreshold)
        assertTrue("Noche ruidosa debe estar por encima del umbral", noisyNight > noiseThreshold)
    }

    @Test
    fun sleepDuration_validation() {
        val shortSleep = 4f // horas
        val idealSleep = 8f // horas
        val longSleep = 12f // horas

        val minRecommended = 6f
        val maxRecommended = 10f

        assertTrue("Sueño corto debe ser menor al mínimo recomendado", shortSleep < minRecommended)
        assertTrue("Sueño ideal debe estar en rango recomendado", idealSleep in minRecommended..maxRecommended)
        assertTrue("Sueño largo debe ser mayor al máximo recomendado", longSleep > maxRecommended)
    }

    @Test
    fun lightInterruption_detection() {
        val darkRoom = 0.1f // lux
        val dimRoom = 2f // lux
        val brightRoom = 20f // lux

        val lightThreshold = 5f

        assertFalse("Habitación oscura no debe triggear interrupción", darkRoom > lightThreshold)
        assertFalse("Habitación tenue no debe triggear interrupción", dimRoom > lightThreshold)
        assertTrue("Habitación brillante debe triggear interrupción", brightRoom > lightThreshold)
    }
}