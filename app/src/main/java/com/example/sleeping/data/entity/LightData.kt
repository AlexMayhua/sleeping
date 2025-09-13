package com.example.sleeping.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidad que almacena datos de luminosidad durante el sueño
 */
@Entity(
    tableName = "light_data",
    foreignKeys = [
        ForeignKey(
            entity = SleepSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LightData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val sessionId: Long,
    val timestamp: Date,
    val luxLevel: Float,
    val isInterruption: Boolean = false // si hubo una interrupción por luz
)
