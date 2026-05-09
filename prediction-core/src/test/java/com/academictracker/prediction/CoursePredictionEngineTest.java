package com.academictracker.prediction;

import com.academictracker.prediction.engine.CoursePredictionEngine;
import com.academictracker.prediction.model.Assessment;
import com.academictracker.prediction.model.Course;
import com.academictracker.prediction.model.CourseStatus;
import com.academictracker.prediction.model.PredictionMode;
import com.academictracker.prediction.result.CoursePredictionResult;
import com.academictracker.prediction.result.WhatIfResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoursePredictionEngineTest {
    private final CoursePredictionEngine engine = new CoursePredictionEngine();

    @Test
    void coursePredictorCalculatesRequiredScoreWhenFinalIsMissing() {
        Course course = course("Java", 3);
        course.addAssessment(new Assessment("Midterm", 0.5, 80.0));
        course.addAssessment(new Assessment("Final", 0.5, null));

        CoursePredictionResult result = engine.predict(course, 85.0, PredictionMode.STRICT);

        assertEquals(90.0, result.requiredScoreForTarget(), 0.001);
    }

    @Test
    void coursePredictorExplainsImpossibleRequiredScoreAbove100() {
        Course course = course("Java", 3);
        course.addAssessment(new Assessment("Midterm", 0.5, 80.0));
        course.addAssessment(new Assessment("Final", 0.5, null));

        CoursePredictionResult result = engine.predict(course, 95.0, PredictionMode.STRICT);

        assertTrue(result.requiredScoreForTarget() > 100.0);
        assertTrue(result.explanationVi().contains("không khả thi"));
    }

    @Test
    void createsPerAssessmentPlanForMultipleMissingAssessments() {
        Course course = course("Project Studio", 3);
        course.addAssessment(new Assessment("Midterm", 0.3, 75.0));
        course.addAssessment(new Assessment("Project", 0.3, null));
        course.addAssessment(new Assessment("Final", 0.4, null));

        CoursePredictionResult result = engine.predict(course, 80.0, PredictionMode.STRICT);

        assertEquals(2, result.perAssessmentPlan().size());
        assertTrue(result.requiredScoreForTarget() > 0);
    }

    @Test
    void avoidsMisleadingAlreadyReachedWordingWhenTargetIsAbovePace() {
        Course course = course("Research", 3);
        course.addAssessment(new Assessment("Portfolio", 0.8, 90.0));
        course.addAssessment(new Assessment("Final", 0.2, null));

        CoursePredictionResult result = engine.predict(course, 60.0, PredictionMode.STRICT);

        assertTrue(result.requiredScoreForTarget() < 0);
        assertFalse(result.explanationVi().contains("Target already reached"));
        assertTrue(result.explanationVi().contains("above target pace"));
    }

    @Test
    void customWhatIfReturnsCorrectGradeForFinalScore() {
        Course course = course("Java", 3);
        course.addAssessment(new Assessment("Midterm", 0.5, 80.0));
        course.addAssessment(new Assessment("Final", 0.5, null));

        WhatIfResult whatIf = engine.customWhatIfScenario(course, Map.of("Final", 80.0), PredictionMode.STRICT);

        assertEquals(80.0, whatIf.finalCourseScore(), 0.001);
        assertEquals("A", whatIf.grade().letterGrade());
        assertEquals(3.5, whatIf.grade().scale4(), 0.001);
    }

    @Test
    void draftModeKeepsPredictionValidForIncompleteWeight() {
        Course course = course("Writing", 2);
        course.addAssessment(new Assessment("Essay", 0.4, 80.0));

        CoursePredictionResult result = engine.predict(course, 70.0, PredictionMode.DRAFT);

        assertTrue(result.valid());
        assertTrue(result.warnings().stream().anyMatch(item -> item.contains("Course weight is incomplete")));
    }

    @Test
    void weightAbove100MakesCoursePredictionInvalid() {
        Course course = course("Data Mining", 3);
        course.addAssessment(new Assessment("Project", 0.7, 80.0));
        course.addAssessment(new Assessment("Final", 0.5, null));

        CoursePredictionResult result = engine.predict(course, 80.0, PredictionMode.DRAFT);

        assertFalse(result.valid());
    }

    private Course course(String name, int credits) {
        return new Course(name.toLowerCase().replace(" ", "-"), name, "General", credits, true, CourseStatus.IN_PROGRESS);
    }
}
