package com.example.sleeping.data.dao

import androidx.room.*
import com.example.sleeping.data.entity.NoiseData
import kotlinx.coroutines.flow.Flow

@Dao
interface NoiseDataDao {
    
    @Query("SELECT * FROM noise_data WHERE sessionId = :sessionId ORDER BY timestamp")
    fun getNoiseDataForSession(sessionId: Long): Flow<List<NoiseData>>
    
    @Query("SELECT AVG(decibelLevel) FROM noise_data WHERE sessionId = :sessionId")
    suspend fun getAverageNoiseLevel(sessionId: Long): Float?
    
    @Query("SELECT MAX(decibelLevel) FROM noise_data WHERE sessionId = :sessionId")
    suspend fun getMaxNoiseLevel(sessionId: Long): Float?
    
    @Query("SELECT COUNT(*) FROM noise_data WHERE sessionId = :sessionId AND decibelLevel > :threshold")
    suspend fun getNoiseEventsCount(sessionId: Long, threshold: Float = 40f): Int
    
    @Query("SELECT COUNT(*) FROM noise_data WHERE sessionId = :sessionId AND isSnoring = 1")
    suspend fun getSnoringEventsCount(sessionId: Long): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoiseData(noiseData: NoiseData)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoiseDataList(noiseDataList: List<NoiseData>)
    
    @Query("DELETE FROM noise_data WHERE sessionId = :sessionId")
    suspend fun deleteNoiseDataForSession(sessionId: Long)
}
