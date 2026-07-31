package com.signaldekho.app.data.survey

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Survey::class, RoomReadingEntity::class], version = 1, exportSchema = true)
abstract class SurveyDb : RoomDatabase() {
    abstract fun surveyDao(): SurveyDao

    companion object {
        fun build(context: Context): SurveyDb =
            Room.databaseBuilder(context, SurveyDb::class.java, "signaldekho.db").build()
    }
}
