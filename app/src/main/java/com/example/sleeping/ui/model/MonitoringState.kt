package com.example.sleeping.ui.model

/**
 * Estados para la UI de monitoreo del sueño
 */
sealed class MonitoringState {
    object Idle : MonitoringState()
    object Starting : MonitoringState()
    data class Active(
        val startTime: Long,
        val currentNoiseLevel: Float = 0f,
        val currentLightLevel: Float = 0f
    ) : MonitoringState()
    object Stopping : MonitoringState()
    data class Completed(val sessionId: Long) : MonitoringState()
    data class Error(val message: String) : MonitoringState()
}
