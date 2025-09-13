package com.example.sleeping.data.dao

import androidx.room.*
import com.example.sleeping.data.entity.LightData
import kotlinx.coroutines.flow.Flow

@Dao
interface LightDataDao {
    
    @Query("SELECT * FROM light_data WHERE sessionId = :sessionId ORDER BY timestamp")
    fun getLightDataForSession(sessionId: Long): Flow<List<LightData>>
    
    @Query("SELECT AVG(luxLevel) FROM light_data WHERE sessionId = :sessionId")
    suspend fun getAverageLightLevel(sessionId: Long): Float?
    
    @Query("SELECT COUNT(*) FROM light_data WHERE sessionId = :sessionId AND isInterruption = 1")
    suspend fun getLightInterruptionsCount(sessionId: Long): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLightData(lightData: LightData)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLightDataList(lightDataList: List<LightData>)
    
    @Query("DELETE FROM light_data WHERE sessionId = :sessionId")
    suspend fun deleteLightDataForSession(sessionId: Long)
}
