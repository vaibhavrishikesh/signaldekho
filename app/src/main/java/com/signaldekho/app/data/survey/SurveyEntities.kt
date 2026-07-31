package com.signaldekho.app.data.survey

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "surveys")
data class Survey(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
)

@Entity(tableName = "room_readings")
data class RoomReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surveyId: Long,
    val roomName: String,
    val wifiSsid: String?,
    val wifiRssi: Int?,
    val cellDbmSim1: Int?,
    val cellDbmSim2: Int?,
    val takenAt: Long,
)
