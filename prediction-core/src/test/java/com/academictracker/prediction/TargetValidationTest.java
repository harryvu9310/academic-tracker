package com.academictracker.prediction;

import com.academictracker.prediction.engine.CoursePredictionEngine;
import com.academictracker.prediction.engine.GlobalPredictionEngine;
import com.academictracker.prediction.model.Assessment;
import com.academictracker.prediction.model.Course;
import com.academictracker.prediction.model.CourseStatus;
import com.academictracker.prediction.model.PredictionContext;
import com.academictracker.prediction.model.PredictionMode;
import com.academictracker.prediction.model.ProgramConfig;
import com.academictracker.prediction.model.StudentPerformanceHistory;
import com.academictracker.prediction.result.CoursePredictionResult;
import com.academictracker.prediction.result.TargetValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetValidationTest {
    @Test
    void impossibleTargetIsNotValidBeforeSaveAndSuggestsRealisticTarget() {
        Course course = new Course("a", "Completed A", "General", 3, true, CourseStatus.COMPLETED);
        course.addAssessment(new Assessment("Final", 1.0, 80.0));
        PredictionContext context = new PredictionContext();
        context.setCourses(List.of(course));
        context.setProgramConfig(new ProgramConfig());

        TargetValidationResult result = new GlobalPredictionEngine().validateTargetBeforeSave(4.0, context);

        assertFalse(result.valid());
        assertTrue(result.maxAchievableGPA() < 4.0);
        assertTrue(result.suggestedTarget() <= result.maxAchievableGPA());
    }

    @Test
    void strongSubjectRecommendedTargetIsHigherThanRequiredTarget() {
        Course course = new Course("stats", "Statistics", "Quantitative", 3, true, CourseStatus.IN_PROGRESS);
        course.addAssessment(new Assessment("Midterm", 0.5, 80.0));
        course.addAssessment(new Assessment("Final", 0.5, null));

        CoursePredictionResult result = new CoursePredictionEngine().predict(
                course,
                70.0,
                PredictionMode.STRICT,
                List.of(new StudentPerformanceHistory("Quantitative", List.of(90.0, 92.0), null)),
                null
        );

        assertTrue(result.recommendedTargetScore() > result.requiredScoreForTarget());
        assertTrue(result.recommendedTargetGrade().scale4() >= 3.5);
    }
}
