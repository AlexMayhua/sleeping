package com.example.sleeping.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.sleeping.data.converter.Converters
import com.example.sleeping.data.dao.LightDataDao
import com.example.sleeping.data.dao.NoiseDataDao
import com.example.sleeping.data.dao.SleepSessionDao
import com.example.sleeping.data.entity.LightData
import com.example.sleeping.data.entity.NoiseData
import com.example.sleeping.data.entity.SleepSession

/**
 * Base de datos Room principal para el análisis de calidad del sueño
 */
@Database(
    entities = [
        SleepSession::class,
        NoiseData::class,
        LightData::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SleepDatabase : RoomDatabase() {
    
    abstract fun sleepSessionDao(): SleepSessionDao
    abstract fun noiseDataDao(): NoiseDataDao
    abstract fun lightDataDao(): LightDataDao
    
    companion object {
        @Volatile
        private var INSTANCE: SleepDatabase? = null
        
        fun getDatabase(context: Context): SleepDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SleepDatabase::class.java,
                    "sleep_database"
                )
                    .fallbackToDestructiveMigration() // Para desarrollo
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
