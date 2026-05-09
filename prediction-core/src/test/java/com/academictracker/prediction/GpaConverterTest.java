package com.academictracker.prediction;

import com.academictracker.prediction.core.GpaConverter;
import com.academictracker.prediction.result.GpaResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GpaConverterTest {
    private final GpaConverter converter = new GpaConverter();

    @Test
    void convertsMandatoryBoundaryCases() {
        assertGrade(90.0, "A+", 4.0, true);
        assertGrade(89.99, "A", 3.5, true);
        assertGrade(80.0, "A", 3.5, true);
        assertGrade(70.0, "B+", 3.0, true);
        assertGrade(60.0, "B", 2.5, true);
        assertGrade(50.0, "C", 2.0, true);
        assertGrade(49.99, "D+", 1.5, false);
        assertGrade(30.0, "D", 1.0, false);
        assertGrade(29.99, "F", 0.0, false);
    }

    @Test
    void reversesScaleToMinimumScoreBoundary() {
        assertEquals(90.0, converter.minimumScoreForScale(4.0), 0.001);
        assertEquals(80.0, converter.minimumScoreForScale(3.5), 0.001);
        assertEquals(70.0, converter.minimumScoreForScale(3.0), 0.001);
        assertEquals(60.0, converter.minimumScoreForScale(2.5), 0.001);
        assertEquals(50.0, converter.minimumScoreForScale(2.0), 0.001);
        assertEquals(80.0, converter.minimumScoreForScale(3.27), 0.001);
    }

    @Test
    void rejectsNaNAndInfinityBeforeBandLookup() {
        assertThrows(IllegalArgumentException.class, () -> converter.fromScore(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> converter.fromScore(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> converter.minimumScoreForScale(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> converter.minimumScoreForScale(Double.NEGATIVE_INFINITY));
    }

    private void assertGrade(double score, String letter, double scale4, boolean passing) {
        GpaResult result = converter.fromScore(score);
        assertEquals(letter, result.letterGrade());
        assertEquals(scale4, result.scale4(), 0.001);
        if (passing) {
            assertTrue(result.passing());
        } else {
            assertFalse(result.passing());
        }
    }
}
