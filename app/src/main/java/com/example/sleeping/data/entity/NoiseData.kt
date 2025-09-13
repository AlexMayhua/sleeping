package com.example.sleeping.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidad que almacena datos detallados de ruido capturados durante el sueño
 */
@Entity(
    tableName = "noise_data",
    foreignKeys = [
        ForeignKey(
            entity = SleepSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class NoiseData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val sessionId: Long,
    val timestamp: Date,
    val decibelLevel: Float,
    val isSnoring: Boolean = false,
    val duration: Long // en milisegundos
)
