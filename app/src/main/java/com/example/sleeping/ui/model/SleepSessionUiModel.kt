package com.example.sleeping.ui.model

import java.util.Date

/**
 * Clase de datos para mostrar información de una sesión en la UI
 */
data class SleepSessionUiModel(
    val id: Long,
    val date: Date,
    val duration: String,
    val sleepQualityScore: Float,
    val averageNoiseLevel: Float,
    val noiseEvents: Int,
    val lightInterruptions: Int,
    val isCompleted: Boolean,
    val qualityDescription: String = getQualityDescription(sleepQualityScore)
) {
    companion object {
        private fun getQualityDescription(score: Float): String {
            return when {
                score >= 80f -> "Excelente"
                score >= 60f -> "Buena"
                score >= 40f -> "Regular"
                score >= 20f -> "Mala"
                else -> "Muy mala"
            }
        }
    }
}
