package com.signaldekho.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SignalGradeTest {
    @Test fun `wifi grades at locked thresholds`() {
        assertEquals(Grade.GOOD, SignalGrade.wifi(-60))
        assertEquals(Grade.GOOD, SignalGrade.wifi(-30))
        assertEquals(Grade.OK, SignalGrade.wifi(-61))
        assertEquals(Grade.OK, SignalGrade.wifi(-75))
        assertEquals(Grade.WEAK, SignalGrade.wifi(-76))
        assertEquals(Grade.WEAK, SignalGrade.wifi(-90))
    }

    @Test fun `cell grades at locked thresholds`() {
        assertEquals(Grade.GOOD, SignalGrade.cell(-90))
        assertEquals(Grade.GOOD, SignalGrade.cell(-70))
        assertEquals(Grade.OK, SignalGrade.cell(-91))
        assertEquals(Grade.OK, SignalGrade.cell(-105))
        assertEquals(Grade.WEAK, SignalGrade.cell(-106))
    }
}
