package com.signaldekho.app.domain

enum class Grade { EXCELLENT, GOOD, WEAK, VERY_WEAK }

object SignalGrade {
    fun wifi(rssi: Int): Grade = when {
        rssi >= -55 -> Grade.EXCELLENT
        rssi >= -67 -> Grade.GOOD
        rssi >= -80 -> Grade.WEAK
        else -> Grade.VERY_WEAK
    }

    fun cell(dbm: Int): Grade = when {
        dbm >= -85 -> Grade.EXCELLENT
        dbm >= -95 -> Grade.GOOD
        dbm >= -110 -> Grade.WEAK
        else -> Grade.VERY_WEAK
    }

    fun wifiFraction(rssi: Int): Float = fraction(rssi, worst = -90, best = -30)

    fun cellFraction(dbm: Int): Float = fraction(dbm, worst = -120, best = -70)

    private fun fraction(value: Int, worst: Int, best: Int): Float =
        ((value - worst).toFloat() / (best - worst)).coerceIn(0f, 1f)
}
