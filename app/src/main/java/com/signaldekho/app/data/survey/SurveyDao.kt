package com.signaldekho.app.data.survey

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SurveyDao {
    @Insert suspend fun insertSurvey(s: Survey): Long
    @Insert suspend fun insertReading(r: RoomReadingEntity)
    @Query("SELECT id FROM surveys ORDER BY createdAt DESC LIMIT 1")
    suspend fun latestSurveyId(): Long?
    @Query("SELECT * FROM room_readings WHERE surveyId = :surveyId ORDER BY takenAt")
    fun readingsFor(surveyId: Long): Flow<List<RoomReadingEntity>>
    @Query("DELETE FROM surveys WHERE id = :surveyId")
    suspend fun deleteSurvey(surveyId: Long)
    @Query("DELETE FROM room_readings WHERE surveyId = :surveyId")
    suspend fun deleteReadings(surveyId: Long)
}
