package com.example.sleeping.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.sleeping.MainActivity
import com.example.sleeping.R
import com.example.sleeping.data.database.SleepDatabase
import com.example.sleeping.data.entity.LightData
import com.example.sleeping.data.entity.NoiseData
import com.example.sleeping.data.entity.SleepSession
import com.example.sleeping.data.repository.SleepRepository
import kotlinx.coroutines.*
import java.util.*
import kotlin.math.*

/**
 * Servicio en primer plano para monitorear el sueño mediante análisis de audio y luminosidad
 */
class SleepMonitoringService : Service(), SensorEventListener {

    companion object {
        const val ACTION_START_MONITORING = "START_MONITORING"
        const val ACTION_STOP_MONITORING = "STOP_MONITORING"
        const val ACTION_SENSOR_UPDATE = "SENSOR_UPDATE"
        
        const val EXTRA_NOISE_LEVEL = "NOISE_LEVEL"
        const val EXTRA_LIGHT_LEVEL = "LIGHT_LEVEL"
        
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "sleep_monitoring_channel"
        
        // Configuración de audio
        private const val SAMPLE_RATE = 8000
        private const val BUFFER_SIZE = 1024
        private const val NOISE_THRESHOLD = 40f // dB
        private const val LIGHT_THRESHOLD = 5f // lux
        
        // Intervalos de muestreo (para optimizar batería)
        private const val AUDIO_SAMPLE_INTERVAL = 30000L // 30 segundos
        private const val LIGHT_SAMPLE_INTERVAL = 10000L // 10 segundos
    }

    private lateinit var repository: SleepRepository
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Audio recording
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    
    // Sensores
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    
    // Datos de la sesión actual
    private var currentSessionId: Long = -1
    private var sessionStartTime: Date? = null
    
    // Datos en tiempo real
    private var currentNoiseLevel = 0f
    private var currentLightLevel = 0f
    
    // Handler para tareas periódicas
    private val handler = Handler(Looper.getMainLooper())
    private var audioSamplingRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        
        val database = SleepDatabase.getDatabase(this)
        repository = SleepRepository(
            database.sleepSessionDao(),
            database.noiseDataDao(),
            database.lightDataDao()
        )
        
        setupSensorManager()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> startMonitoring()
            ACTION_STOP_MONITORING -> stopMonitoring()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setupSensorManager() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    }

    private fun startMonitoring() {
        // Crear notificación persistente
        val notification = createNotification("Iniciando monitoreo del sueño...")
        startForeground(NOTIFICATION_ID, notification)
        
        serviceScope.launch {
            // Crear nueva sesión
            val session = SleepSession(
                startTime = Date(),
                endTime = null,
                isCompleted = false
            )
            
            currentSessionId = repository.insertSession(session)
            sessionStartTime = session.startTime
            
            // Iniciar monitoreo de sensores
            startAudioMonitoring()
            startLightMonitoring()
            
            updateNotification("Monitoreando sueño - ${formatDuration(0)}")
        }
    }

    private fun stopMonitoring() {
        serviceScope.launch {
            // Completar sesión
            if (currentSessionId != -1L) {
                repository.completeSession(currentSessionId)
            }
            
            // Detener monitoreo
            stopAudioMonitoring()
            stopLightMonitoring()
            
            stopForeground(true)
            stopSelf()
        }
    }

    private fun startAudioMonitoring() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            )
            
            isRecording = true
            
            // Configurar muestreo periódico para ahorrar batería
            audioSamplingRunnable = object : Runnable {
                override fun run() {
                    if (isRecording) {
                        sampleAudio()
                        handler.postDelayed(this, AUDIO_SAMPLE_INTERVAL)
                    }
                }
            }
            
            handler.post(audioSamplingRunnable!!)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sampleAudio() {
        serviceScope.launch {
            try {
                audioRecord?.let { record ->
                    if (record.state == AudioRecord.STATE_INITIALIZED) {
                        record.startRecording()
                        
                        val buffer = ShortArray(BUFFER_SIZE)
                        val bytesRead = record.read(buffer, 0, BUFFER_SIZE)
                        
                        if (bytesRead > 0) {
                            val decibelLevel = calculateDecibelLevel(buffer, bytesRead)
                            currentNoiseLevel = decibelLevel
                            
                            // Guardar datos si supera el umbral o para referencia
                            val isSignificant = decibelLevel > NOISE_THRESHOLD
                            val isSnoring = detectSnoring(buffer, bytesRead)
                            
                            val noiseData = NoiseData(
                                sessionId = currentSessionId,
                                timestamp = Date(),
                                decibelLevel = decibelLevel,
                                isSnoring = isSnoring,
                                duration = AUDIO_SAMPLE_INTERVAL
                            )
                            
                            repository.insertNoiseData(noiseData)
                            
                            // Broadcast para actualizar UI
                            broadcastSensorUpdate()
                        }
                        
                        record.stop()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun calculateDecibelLevel(buffer: ShortArray, length: Int): Float {
        var sum = 0.0
        for (i in 0 until length) {
            sum += (buffer[i] * buffer[i]).toDouble()
        }
        val rms = sqrt(sum / length)
        
        // Convertir a decibelios (aproximación)
        return if (rms > 0) {
            (20 * log10(rms)).toFloat().coerceAtLeast(0f)
        } else {
            0f
        }
    }

    private fun detectSnoring(buffer: ShortArray, length: Int): Boolean {
        // Algoritmo básico de detección de ronquidos
        // Buscar patrones de frecuencia baja y regular
        val threshold = 25000 // Umbral de amplitud
        var consecutiveHighSamples = 0
        
        for (i in 0 until length) {
            if (abs(buffer[i].toInt()) > threshold) {
                consecutiveHighSamples++
            } else {
                consecutiveHighSamples = 0
            }
            
            // Si hay suficientes muestras consecutivas altas, podría ser ronquido
            if (consecutiveHighSamples > length * 0.3) {
                return true
            }
        }
        
        return false
    }

    private fun startLightMonitoring() {
        lightSensor?.let { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    private fun stopAudioMonitoring() {
        isRecording = false
        audioSamplingRunnable?.let { handler.removeCallbacks(it) }
        audioRecord?.release()
        audioRecord = null
    }

    private fun stopLightMonitoring() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_LIGHT) {
            val luxValue = event.values[0]
            currentLightLevel = luxValue
            
            serviceScope.launch {
                val isInterruption = luxValue > LIGHT_THRESHOLD
                
                val lightData = LightData(
                    sessionId = currentSessionId,
                    timestamp = Date(),
                    luxLevel = luxValue,
                    isInterruption = isInterruption
                )
                
                repository.insertLightData(lightData)
                
                // Broadcast para actualizar UI
                broadcastSensorUpdate()
            }
            
            // Actualizar notificación periódicamente
            val duration = sessionStartTime?.let { 
                (System.currentTimeMillis() - it.time) / 1000 / 60 
            } ?: 0
            updateNotification("Monitoreando sueño - ${formatDuration(duration)}")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No necesario implementar
    }

    private fun broadcastSensorUpdate() {
        val intent = Intent(ACTION_SENSOR_UPDATE).apply {
            putExtra(EXTRA_NOISE_LEVEL, currentNoiseLevel)
            putExtra(EXTRA_LIGHT_LEVEL, currentLightLevel)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Monitoreo del Sueño",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación para el servicio de monitoreo del sueño"
                setSound(null, null)
            }
            
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Analizador de Sueño")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_menu_slideshow)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun formatDuration(minutes: Long): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudioMonitoring()
        stopLightMonitoring()
        serviceScope.cancel()
    }
}
