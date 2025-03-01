package com.application.audio_recorder_application.data.db

import androidx.room.*
import com.application.audio_recorder_application.data.model.Recording
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY dateCreated DESC")
    fun getAllRecordings(): Flow<List<Recording>>

    @Query("SELECT * FROM recordings WHERE category = :category ORDER BY dateCreated DESC")
    fun getRecordingsByCategory(category: String): Flow<List<Recording>>

    @Query("SELECT * FROM recordings WHERE isFavorite = 1 ORDER BY dateCreated DESC")
    fun getFavoriteRecordings(): Flow<List<Recording>>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getRecordingById(id: String): Recording?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: Recording)

    @Update
    suspend fun updateRecording(recording: Recording)

    @Delete
    suspend fun deleteRecording(recording: Recording)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteRecordingById(id: String)

    @Query("SELECT DISTINCT category FROM recordings")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT * FROM recordings WHERE fileName LIKE '%' || :query || '%' OR transcription LIKE '%' || :query || '%'")
    fun searchRecordings(query: String): Flow<List<Recording>>

    @Query("SELECT * FROM recordings ORDER BY dateCreated DESC")
    suspend fun getAllRecordingsSync(): List<Recording>
} 