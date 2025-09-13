package com.example.sleeping.data.dao

import androidx.room.*
import com.example.sleeping.data.entity.SleepSession
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface SleepSessionDao {
    
    @Query("SELECT * FROM sleep_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<SleepSession>>
    
    @Query("SELECT * FROM sleep_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): SleepSession?
    
    @Query("SELECT * FROM sleep_sessions WHERE isCompleted = 0 LIMIT 1")
    suspend fun getActiveSession(): SleepSession?
    
    @Query("SELECT * FROM sleep_sessions WHERE startTime >= :fromDate ORDER BY startTime DESC")
    fun getSessionsFromDate(fromDate: Date): Flow<List<SleepSession>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SleepSession): Long
    
    @Update
    suspend fun updateSession(session: SleepSession)
    
    @Delete
    suspend fun deleteSession(session: SleepSession)
    
    @Query("DELETE FROM sleep_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)
    
    @Query("SELECT COUNT(*) FROM sleep_sessions")
    suspend fun getSessionCount(): Int
}
