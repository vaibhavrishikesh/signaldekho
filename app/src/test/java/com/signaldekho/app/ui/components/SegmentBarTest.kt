package com.signaldekho.app.ui.components

import com.signaldekho.app.domain.Grade
import org.junit.Assert.assertEquals
import org.junit.Test

class SegmentBarTest {
    @Test fun `four-segment meter agrees with the grade word`() {
        assertEquals(4, litSegments(Grade.EXCELLENT, 4))
        assertEquals(3, litSegments(Grade.GOOD, 4))
        assertEquals(2, litSegments(Grade.WEAK, 4))
        assertEquals(1, litSegments(Grade.VERY_WEAK, 4))
    }

    @Test fun `three-segment meter never shows an empty bar`() {
        assertEquals(3, litSegments(Grade.EXCELLENT, 3))
        assertEquals(2, litSegments(Grade.GOOD, 3))
        assertEquals(1, litSegments(Grade.WEAK, 3))
        assertEquals(1, litSegments(Grade.VERY_WEAK, 3))
    }
}
