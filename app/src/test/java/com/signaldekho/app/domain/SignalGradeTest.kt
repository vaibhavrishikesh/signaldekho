package com.signaldekho.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SignalGradeTest {
    @Test fun `wifi grades at locked thresholds`() {
        assertEquals(Grade.EXCELLENT, SignalGrade.wifi(-30))
        assertEquals(Grade.EXCELLENT, SignalGrade.wifi(-55))
        assertEquals(Grade.GOOD, SignalGrade.wifi(-56))
        assertEquals(Grade.GOOD, SignalGrade.wifi(-67))
        assertEquals(Grade.WEAK, SignalGrade.wifi(-68))
        assertEquals(Grade.WEAK, SignalGrade.wifi(-80))
        assertEquals(Grade.VERY_WEAK, SignalGrade.wifi(-81))
        assertEquals(Grade.VERY_WEAK, SignalGrade.wifi(-95))
    }

    @Test fun `cell grades at locked thresholds`() {
        assertEquals(Grade.EXCELLENT, SignalGrade.cell(-70))
        assertEquals(Grade.EXCELLENT, SignalGrade.cell(-85))
        assertEquals(Grade.GOOD, SignalGrade.cell(-86))
        assertEquals(Grade.GOOD, SignalGrade.cell(-95))
        assertEquals(Grade.WEAK, SignalGrade.cell(-96))
        assertEquals(Grade.WEAK, SignalGrade.cell(-110))
        assertEquals(Grade.VERY_WEAK, SignalGrade.cell(-111))
    }

    @Test fun `wifi fraction maps range and clamps`() {
        assertEquals(0f, SignalGrade.wifiFraction(-90), 0.001f)
        assertEquals(1f, SignalGrade.wifiFraction(-30), 0.001f)
        assertEquals(0.5f, SignalGrade.wifiFraction(-60), 0.001f)
        assertEquals(0f, SignalGrade.wifiFraction(-120), 0.001f)
        assertEquals(1f, SignalGrade.wifiFraction(-10), 0.001f)
    }

    @Test fun `cell fraction maps range and clamps`() {
        assertEquals(0f, SignalGrade.cellFraction(-120), 0.001f)
        assertEquals(1f, SignalGrade.cellFraction(-70), 0.001f)
        assertEquals(0.5f, SignalGrade.cellFraction(-95), 0.001f)
        assertEquals(0f, SignalGrade.cellFraction(-130), 0.001f)
    }
}
