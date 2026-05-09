package com.academictracker.prediction;

import com.academictracker.prediction.core.GradeCalculator;
import com.academictracker.prediction.model.Assessment;
import com.academictracker.prediction.model.Course;
import com.academictracker.prediction.model.CourseStatus;
import com.academictracker.prediction.model.PassRiskLevel;
import com.academictracker.prediction.model.PredictionMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GradeCalculatorTest {
    private final GradeCalculator calculator = new GradeCalculator();

    @Test
    void calculatesRequiredScoreWhenFinalIsMissing() {
        Course course = course("Java", 3);
        course.addAssessment(new Assessment("Midterm", 0.5, 80.0));
        course.addAssessment(new Assessment("Final", 0.5, null));

        GradeCalculator.Calculation result = calculator.calculate(course, 85.0, PredictionMode.STRICT, 75.0);

        assertTrue(result.valid());
        assertEquals(90.0, result.requiredScoreForTarget(), 0.001);
    }

    @Test
    void reportsImpossibleWhenRequiredScoreIsAbove100() {
        Course course = course("Java", 3);
        course.addAssessment(new Assessment("Midterm", 0.5, 80.0));
        course.addAssessment(new Assessment("Final", 0.5, null));

        GradeCalculator.Calculation result = calculator.calculate(course, 95.0, PredictionMode.STRICT, 75.0);

        assertTrue(result.requiredScoreForTarget() > 100.0);
    }

    @Test
    void draftModeAllowsIncompleteWeightWithWarning() {
        Course course = course("English", 2);
        course.addAssessment(new Assessment("Essay", 0.4, 80.0));

        GradeCalculator.Calculation result = calculator.calculate(course, 70.0, PredictionMode.DRAFT, 75.0);

        assertTrue(result.valid());
        assertEquals(0.6, result.unassignedWeight(), 0.001);
        assertTrue(result.warnings().stream().anyMatch(item -> item.contains("Course weight is incomplete")));
    }

    @Test
    void weightAbove100IsValidationError() {
        Course course = course("Statistics", 3);
        course.addAssessment(new Assessment("Project", 0.7, 80.0));
        course.addAssessment(new Assessment("Final", 0.5, null));

        GradeCalculator.Calculation result = calculator.calculate(course, 80.0, PredictionMode.DRAFT, 75.0);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(item -> item.contains("exceeds 100%")));
    }

    @Test
    void lowExpectedScoreCreatesRiskyOrCriticalPassRisk() {
        Course course = course("Calculus", 4);
        course.addAssessment(new Assessment("Midterm", 0.8, 20.0));
        course.addAssessment(new Assessment("Final", 0.2, null));

        GradeCalculator.Calculation result = calculator.calculate(course, 50.0, PredictionMode.STRICT, 30.0);

        assertTrue(result.passRiskLevel() == PassRiskLevel.RISKY || result.passRiskLevel() == PassRiskLevel.CRITICAL);
    }

    @Test
    void rejectsNaNAndInfinityAssessmentValues() {
        assertThrows(IllegalArgumentException.class, () -> new Assessment("Bad score", 0.5, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new Assessment("Bad score", 0.5, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new Assessment("Bad weight", Double.NaN, 80.0));
        assertThrows(IllegalArgumentException.class, () -> new Assessment("Bad weight", Double.NEGATIVE_INFINITY, 80.0));
    }

    @Test
    void invalidNaNTargetProducesValidationErrorInsteadOfCrash() {
        Course course = course("Java", 3);
        course.addAssessment(new Assessment("Midterm", 0.5, 80.0));
        course.addAssessment(new Assessment("Final", 0.5, null));

        GradeCalculator.Calculation result = calculator.calculate(course, Double.NaN, PredictionMode.STRICT, 75.0);

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("Target score must be a finite number")));
    }

    @Test
    void courseCreditsMustBePositive() {
        assertThrows(IllegalArgumentException.class,
                () -> new Course("zero", "Zero", "General", 0, true, CourseStatus.IN_PROGRESS));
        assertThrows(IllegalArgumentException.class,
                () -> new Course("negative", "Negative", "General", -1, true, CourseStatus.IN_PROGRESS));

        Course course = new Course();
        assertThrows(IllegalArgumentException.class, () -> course.setCredits(0));
        course.setCredits(1);
        assertEquals(1, course.getCredits());
    }

    private Course course(String name, int credits) {
        return new Course(name.toLowerCase(), name, "General", credits, true, CourseStatus.IN_PROGRESS);
    }
}
