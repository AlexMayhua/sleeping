package com.example.sleeping.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidad que representa una sesión completa de análisis de sueño
 */
@Entity(tableName = "sleep_sessions")
data class SleepSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val startTime: Date,
    val endTime: Date?,
    
    val averageNoiseLevel: Float = 0f,
    val maxNoiseLevel: Float = 0f,
    val noiseEvents: Int = 0,
    
    val averageLightLevel: Float = 0f,
    val lightInterruptions: Int = 0,
    
    val estimatedSleepHours: Float = 0f,
    val sleepQualityScore: Float = 0f,
    
    val isCompleted: Boolean = false,
    val notes: String? = null
)
