package com.signaldekho.app.domain

enum class Grade { GOOD, OK, WEAK }

object SignalGrade {
    fun wifi(rssi: Int): Grade = when {
        rssi >= -60 -> Grade.GOOD
        rssi >= -75 -> Grade.OK
        else -> Grade.WEAK
    }

    fun cell(dbm: Int): Grade = when {
        dbm >= -90 -> Grade.GOOD
        dbm >= -105 -> Grade.OK
        else -> Grade.WEAK
    }
}
