package com.example.sleeping.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Utilidades para optimizar el consumo de batería y gestionar configuraciones de energía
 */
object BatteryOptimizationUtils {
    
    /**
     * Verifica si la app está exenta de optimizaciones de batería
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = ContextCompat.getSystemService(context, PowerManager::class.java)
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } else {
            true // En versiones anteriores no hay optimizaciones de batería
        }
    }
    
    /**
     * Abre la configuración para desactivar optimizaciones de batería
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isIgnoringBatteryOptimizations(context)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
        }
    }
    
    /**
     * Configuraciones recomendadas para optimizar el consumo durante el monitoreo
     */
    object MonitoringConfig {
        // Intervalos de muestreo (en milisegundos)
        const val AUDIO_SAMPLE_INTERVAL_NORMAL = 30000L // 30 segundos
        const val AUDIO_SAMPLE_INTERVAL_BATTERY_SAVER = 60000L // 1 minuto
        
        const val LIGHT_SAMPLE_INTERVAL_NORMAL = 10000L // 10 segundos
        const val LIGHT_SAMPLE_INTERVAL_BATTERY_SAVER = 20000L // 20 segundos
        
        // Duración de muestreo de audio (en milisegundos)
        const val AUDIO_RECORDING_DURATION_NORMAL = 3000L // 3 segundos
        const val AUDIO_RECORDING_DURATION_BATTERY_SAVER = 2000L // 2 segundos
        
        // Umbrales para eventos significativos
        const val NOISE_THRESHOLD_DB = 40f
        const val LIGHT_THRESHOLD_LUX = 5f
        
        /**
         * Obtiene la configuración apropiada basada en el nivel de batería
         */
        fun getOptimizedConfig(context: Context): Config {
            val batteryLevel = getBatteryLevel(context)
            
            return if (batteryLevel < 20) {
                // Modo ahorro extremo de batería
                Config(
                    audioSampleInterval = AUDIO_SAMPLE_INTERVAL_BATTERY_SAVER * 2,
                    lightSampleInterval = LIGHT_SAMPLE_INTERVAL_BATTERY_SAVER * 2,
                    audioRecordingDuration = AUDIO_RECORDING_DURATION_BATTERY_SAVER,
                    enableContinuousMonitoring = false
                )
            } else if (batteryLevel < 50) {
                // Modo ahorro de batería
                Config(
                    audioSampleInterval = AUDIO_SAMPLE_INTERVAL_BATTERY_SAVER,
                    lightSampleInterval = LIGHT_SAMPLE_INTERVAL_BATTERY_SAVER,
                    audioRecordingDuration = AUDIO_RECORDING_DURATION_BATTERY_SAVER,
                    enableContinuousMonitoring = true
                )
            } else {
                // Modo normal
                Config(
                    audioSampleInterval = AUDIO_SAMPLE_INTERVAL_NORMAL,
                    lightSampleInterval = LIGHT_SAMPLE_INTERVAL_NORMAL,
                    audioRecordingDuration = AUDIO_RECORDING_DURATION_NORMAL,
                    enableContinuousMonitoring = true
                )
            }
        }
        
        private fun getBatteryLevel(context: Context): Int {
            return try {
                val batteryManager = ContextCompat.getSystemService(context, android.os.BatteryManager::class.java)
                batteryManager?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
            } catch (e: Exception) {
                100 // Asumir batería completa si no se puede obtener
            }
        }
    }
    
    /**
     * Clase de configuración para el monitoreo
     */
    data class Config(
        val audioSampleInterval: Long,
        val lightSampleInterval: Long,
        val audioRecordingDuration: Long,
        val enableContinuousMonitoring: Boolean
    )
    
    /**
     * Consejos para el usuario sobre optimización de batería
     */
    fun getBatteryOptimizationTips(): List<String> {
        return listOf(
            "Coloque el teléfono en modo avión con WiFi activado para reducir el consumo",
            "Asegúrese de que el teléfono esté conectado al cargador durante sesiones largas",
            "Cierre otras aplicaciones antes de iniciar el monitoreo",
            "Reduzca el brillo de la pantalla al mínimo o use modo nocturno",
            "Desactive las notificaciones no esenciales durante el monitoreo",
            "Use el modo 'No molestar' para evitar interrupciones"
        )
    }
}
